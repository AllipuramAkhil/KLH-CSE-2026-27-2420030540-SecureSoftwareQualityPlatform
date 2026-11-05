package com.assqp.platform.churn;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes per-file code churn directly from LOCAL git history via JGit -
 * no GitHub API / network access required (works offline, in CI, or
 * against any checked-out repository ASSQP is pointed at).
 *
 * Per Phase 9 requirements: NEVER throws for "not a git repo" / no
 * history / anything else that goes wrong reading it - {@link #isAvailable()}
 * reports the state and {@link #analyze()} simply returns an empty list,
 * logging a warning. Callers (CodeChurnServiceImpl) treat that exactly
 * like "no churn data yet", never as an error response.
 */
@Slf4j
@Component
public class GitChurnAnalyzer {

    @Value("${git.churn.repo-path:.}")
    private String repoPath;

    @Value("${git.churn.lookback-days:90}")
    private int lookbackDays;

    @Value("${git.churn.max-commits:500}")
    private int maxCommits;

    public String getRepoPath() {
        return repoPath;
    }

    public int getLookbackDays() {
        return lookbackDays;
    }

    /** True only if repoPath points at (or inside) a readable git repository. Cheap existence check, not a guarantee analyze() will find commits. */
    public boolean isAvailable() {
        File dir = new File(repoPath);
        if (!dir.exists()) {
            return false;
        }
        try (Git git = Git.open(dir)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Walks up to maxCommits commits (newest-first) within lookbackDays,
     * diffing each against its first parent (or an empty tree for a root
     * commit) to accumulate per-file commit count, lines added/deleted,
     * change frequency, and a recency-weighted "recentModificationScore"
     * (sum of exp(-ageDays/30) across that file's commits in the window -
     * recent changes count for more than old ones, decaying with a
     * roughly one-month half-life). Results sorted by total lines changed
     * descending. Returns an empty list (never throws) if the repo is
     * missing/unreadable/has no commits in range.
     */
    public List<ChurnResult> analyze() {
        File dir = new File(repoPath);
        if (!dir.exists()) {
            log.warn("Configured git repo path '{}' does not exist - code churn metrics unavailable", repoPath);
            return List.of();
        }

        Map<String, ChurnAccumulator> accumulators = new HashMap<>();
        long cutoffEpochSeconds = Instant.now().minus(lookbackDays, ChronoUnit.DAYS).getEpochSecond();

        try (Git git = Git.open(dir)) {
            Repository repository = git.getRepository();
            try (RevWalk revWalk = new RevWalk(repository)) {
                for (RevCommit commit : git.log().setMaxCount(maxCommits).call()) {
                    if (commit.getCommitTime() < cutoffEpochSeconds) {
                        // git log is newest-first, so once we're past the window
                        // every remaining commit is also out of range.
                        break;
                    }
                    accumulateCommit(repository, revWalk, commit, accumulators);
                }
            }
        } catch (Exception ex) {
            log.warn("Code churn analysis unavailable for repo path '{}': {}", repoPath, ex.getMessage());
            return List.of();
        }

        List<ChurnResult> results = new ArrayList<>();
        for (Map.Entry<String, ChurnAccumulator> e : accumulators.entrySet()) {
            ChurnAccumulator acc = e.getValue();
            results.add(ChurnResult.builder()
                    .path(e.getKey())
                    .commitCount(acc.commitCount)
                    .linesAdded(acc.linesAdded)
                    .linesDeleted(acc.linesDeleted)
                    .totalLinesChanged(acc.linesAdded + acc.linesDeleted)
                    .changeFrequency(lookbackDays > 0 ? round(acc.commitCount / (double) lookbackDays) : 0.0)
                    .recentModificationScore(round(acc.recentScore))
                    .build());
        }
        results.sort((a, b) -> Integer.compare(b.getTotalLinesChanged(), a.getTotalLinesChanged()));
        return results;
    }

    private void accumulateCommit(Repository repository, RevWalk revWalk, RevCommit commit,
                                   Map<String, ChurnAccumulator> accumulators) {
        long ageDays = Math.max(0, (Instant.now().getEpochSecond() - commit.getCommitTime()) / 86400);
        double recencyWeight = Math.exp(-ageDays / 30.0);

        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repository);
            df.setDetectRenames(true);

            List<DiffEntry> diffs;
            if (commit.getParentCount() > 0) {
                RevCommit parent = revWalk.parseCommit(commit.getParent(0).getId());
                diffs = df.scan(parent.getTree(), commit.getTree());
            } else {
                diffs = df.scan(new EmptyTreeIterator(), prepareTreeParser(repository, commit.getTree()));
            }

            for (DiffEntry entry : diffs) {
                String path = "/dev/null".equals(entry.getNewPath()) ? entry.getOldPath() : entry.getNewPath();
                if (path == null || "/dev/null".equals(path)) {
                    continue;
                }
                FileHeader header = df.toFileHeader(entry);
                int added = 0;
                int deleted = 0;
                for (Edit edit : header.toEditList()) {
                    added += edit.getEndB() - edit.getBeginB();
                    deleted += edit.getEndA() - edit.getBeginA();
                }

                ChurnAccumulator acc = accumulators.computeIfAbsent(path, p -> new ChurnAccumulator());
                acc.commitCount++;
                acc.linesAdded += added;
                acc.linesDeleted += deleted;
                acc.recentScore += recencyWeight;
            }
        } catch (IOException ex) {
            log.warn("Skipping commit {} while computing code churn: {}", commit.getName(), ex.getMessage());
        }
    }

    private CanonicalTreeParser prepareTreeParser(Repository repository, RevTree tree) throws IOException {
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            treeParser.reset(reader, tree.getId());
            return treeParser;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class ChurnAccumulator {
        int commitCount = 0;
        int linesAdded = 0;
        int linesDeleted = 0;
        double recentScore = 0.0;
    }
}
