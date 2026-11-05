package com.assqp.platform.service.impl;

import com.assqp.platform.churn.ChurnResult;
import com.assqp.platform.churn.GitChurnAnalyzer;
import com.assqp.platform.dto.churn.CodeChurnAnalysisResponse;
import com.assqp.platform.dto.churn.CodeChurnResponse;
import com.assqp.platform.dto.churn.GitChurnStatusResponse;
import com.assqp.platform.entity.CodeChurnMetrics;
import com.assqp.platform.entity.Project;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.CodeChurnMetricsRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.CodeChurnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists {@link GitChurnAnalyzer} output per project (Phase 9). Upserts
 * by (project, componentName) so re-running analysis refreshes existing
 * rows instead of accumulating duplicates. Capped at the top 200
 * highest-churn files per run to keep the table and API response bounded
 * for large repositories - this is a pragmatic limit, not a correctness
 * one, since risk assessment/dashboard only ever care about the highest-
 * churn components anyway.
 */
@Service
@RequiredArgsConstructor
public class CodeChurnServiceImpl implements CodeChurnService {

    private static final int MAX_COMPONENTS_PERSISTED = 200;

    private final GitChurnAnalyzer gitChurnAnalyzer;
    private final CodeChurnMetricsRepository codeChurnMetricsRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;

    @Override
    public GitChurnStatusResponse getStatus() {
        return GitChurnStatusResponse.builder()
                .gitAvailable(gitChurnAnalyzer.isAvailable())
                .repoPath(gitChurnAnalyzer.getRepoPath())
                .lookbackDays(gitChurnAnalyzer.getLookbackDays())
                .build();
    }

    @Override
    @Transactional
    public CodeChurnAnalysisResponse analyze(Long projectId) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        List<ChurnResult> results = gitChurnAnalyzer.analyze();
        boolean available = gitChurnAnalyzer.isAvailable();

        List<CodeChurnMetrics> saved = results.stream()
                .limit(MAX_COMPONENTS_PERSISTED)
                .map(r -> {
                    CodeChurnMetrics metrics = codeChurnMetricsRepository
                            .findByProjectIdAndComponentName(projectId, r.getPath())
                            .orElseGet(() -> CodeChurnMetrics.builder()
                                    .project(project)
                                    .componentName(r.getPath())
                                    .build());
                    metrics.setCommitCount(r.getCommitCount());
                    metrics.setLinesAdded(r.getLinesAdded());
                    metrics.setLinesDeleted(r.getLinesDeleted());
                    metrics.setTotalLinesChanged(r.getTotalLinesChanged());
                    metrics.setChangeFrequency(r.getChangeFrequency());
                    metrics.setRecentModificationScore(r.getRecentModificationScore());
                    return codeChurnMetricsRepository.save(metrics);
                })
                .toList();

        return CodeChurnAnalysisResponse.builder()
                .gitAvailable(available)
                .componentsAnalyzed(saved.size())
                .results(saved.stream().map(this::toResponse).toList())
                .build();
    }

    @Override
    public List<CodeChurnResponse> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return codeChurnMetricsRepository.findByProjectIdOrderByTotalLinesChangedDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    private CodeChurnResponse toResponse(CodeChurnMetrics m) {
        return CodeChurnResponse.builder()
                .id(m.getId())
                .projectId(m.getProject().getId())
                .componentName(m.getComponentName())
                .commitCount(m.getCommitCount())
                .linesAdded(m.getLinesAdded())
                .linesDeleted(m.getLinesDeleted())
                .totalLinesChanged(m.getTotalLinesChanged())
                .changeFrequency(m.getChangeFrequency())
                .recentModificationScore(m.getRecentModificationScore())
                .computedAt(m.getComputedAt())
                .build();
    }
}
