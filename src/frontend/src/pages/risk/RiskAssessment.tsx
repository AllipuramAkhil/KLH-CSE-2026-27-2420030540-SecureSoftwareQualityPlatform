import { useParams } from 'react-router-dom';
import { PlayCircle, Gauge, GitCommit } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { RiskBadge } from '@/components/ui/RiskBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { ScoreCard } from '@/components/common/ScoreCard';
import { useRiskAssessments, useRiskSummary, useRunRiskAnalysis } from '@/hooks/useRisk';
import { useGitChurnStatus, useCodeChurn, useAnalyzeCodeChurn } from '@/hooks/useChurn';
import { useToast } from '@/components/ui/Toast';
import { formatDate } from '@/utils/formatters';
import { riskMatrixCell } from '@/utils/risk';
import { cn } from '@/components/ui/utils';
import { RISK_LEVEL_COLORS } from '@/constants/statuses';
import type { ApiError } from '@/api/axios';

const PROB_LEVELS = ['High', 'Medium', 'Low'] as const;
const IMPACT_LEVELS = ['Low', 'Medium', 'High'] as const;
// Representative sample points used purely to render the matrix legend cells (0-100 scale, matches backend RiskAssessmentServiceImpl)
const PROB_VALUES = { High: 90, Medium: 55, Low: 20 };
const IMPACT_VALUES = { Low: 20, Medium: 55, High: 90 };

export default function RiskAssessment() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();

  const { data: assessments, isLoading, isError, error, refetch } = useRiskAssessments(projectId);
  const { data: summary } = useRiskSummary(projectId);
  const runAnalysis = useRunRiskAnalysis(projectId);

  const { data: churnStatus } = useGitChurnStatus();
  const { data: churnMetrics } = useCodeChurn(projectId);
  const analyzeChurn = useAnalyzeCodeChurn(projectId);

  const handleAnalyzeChurn = async () => {
    try {
      const result = await analyzeChurn.mutateAsync();
      show(
        result.gitAvailable
          ? `Analyzed churn for ${result.componentsAnalyzed} file(s) from local git history`
          : 'Local git repository is not readable - configure GIT_REPO_PATH on the backend',
        result.gitAvailable ? 'success' : 'error'
      );
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleAnalyze = async () => {
    try {
      const results = await runAnalysis.mutateAsync();
      show(`Risk analysis completed for ${results.length} component(s)`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">Risk Assessment</h2>
          <p className="text-sm text-muted-foreground">Component-level risk = probability × impact</p>
        </div>
        <Button onClick={handleAnalyze} isLoading={runAnalysis.isPending}>
          <PlayCircle className="h-4 w-4" /> Run Risk Analysis
        </Button>
      </div>

      {summary && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <ScoreCard label="Overall Risk Score" score={summary.overallRiskScore} invert />
          <Card>
            <CardContent className="flex items-center justify-between pt-4">
              <div>
                <p className="text-xs text-muted-foreground">Overall Risk Level</p>
                <div className="mt-1">
                  <RiskBadge level={summary.overallRiskLevel} className="text-sm" />
                </div>
              </div>
              {summary.topRiskyComponent && (
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">Top Risky Component</p>
                  <p className="font-medium">{summary.topRiskyComponent}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader>
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <CardTitle>Git Code Churn</CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={handleAnalyzeChurn}
              isLoading={analyzeChurn.isPending}
              disabled={churnStatus !== undefined && !churnStatus.gitAvailable}
            >
              <GitCommit className="h-4 w-4" /> Analyze git history
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Per-file commit count, lines changed, and recency-weighted activity from local git history - feeds the
            ML risk prediction's <code>code_churn</code> feature above.
          </p>
        </CardHeader>
        <CardContent>
          {churnStatus && !churnStatus.gitAvailable && (
            <p className="mb-3 text-xs text-muted-foreground">
              No readable git repository configured at <code>{churnStatus.repoPath}</code> - set{' '}
              <code>GIT_REPO_PATH</code> on the backend to a checked-out repo to enable this.
            </p>
          )}
          {churnMetrics && churnMetrics.length === 0 && churnStatus?.gitAvailable && (
            <EmptyState
              icon={<GitCommit className="h-8 w-8" />}
              title="No churn data yet"
              description="Click 'Analyze git history' to compute per-file churn metrics."
            />
          )}
          {churnMetrics && churnMetrics.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[600px] text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs text-muted-foreground">
                    <th className="px-2 py-2 font-medium">File</th>
                    <th className="px-2 py-2 font-medium">Commits</th>
                    <th className="px-2 py-2 font-medium">+/-</th>
                    <th className="px-2 py-2 font-medium">Change Freq</th>
                    <th className="px-2 py-2 font-medium">Activity Score</th>
                  </tr>
                </thead>
                <tbody>
                  {churnMetrics.slice(0, 10).map((c) => (
                    <tr key={c.id} className="border-b border-border last:border-0">
                      <td className="px-2 py-2 font-mono text-xs">{c.componentName}</td>
                      <td className="px-2 py-2 tabular-nums">{c.commitCount}</td>
                      <td className="px-2 py-2 tabular-nums text-xs">
                        <span className="text-success">+{c.linesAdded}</span>{' '}
                        <span className="text-danger">-{c.linesDeleted}</span>
                      </td>
                      <td className="px-2 py-2 tabular-nums">{c.changeFrequency.toFixed(2)}/day</td>
                      <td className="px-2 py-2 tabular-nums font-semibold">{c.recentModificationScore.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Risk Matrix</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[420px] border-collapse text-center text-xs">
              <thead>
                <tr>
                  <th className="p-2"></th>
                  <th colSpan={3} className="p-2 font-medium text-muted-foreground">
                    IMPACT
                  </th>
                </tr>
                <tr>
                  <th className="p-2 font-medium text-muted-foreground">PROBABILITY</th>
                  {IMPACT_LEVELS.map((l) => (
                    <th key={l} className="p-2 font-medium text-muted-foreground">
                      {l}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {PROB_LEVELS.map((p) => (
                  <tr key={p}>
                    <td className="p-2 text-left font-medium text-muted-foreground">{p}</td>
                    {IMPACT_LEVELS.map((imp) => {
                      const level = riskMatrixCell(PROB_VALUES[p] / 100, IMPACT_VALUES[imp]);
                      return (
                        <td key={imp} className="p-1">
                          <div className={cn('rounded-md border py-3 font-bold', RISK_LEVEL_COLORS[level])}>{level[0]}</div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {isLoading && <LoadingSpinner label="Loading risk assessments…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {assessments && assessments.length === 0 && (
        <EmptyState icon={<Gauge className="h-8 w-8" />} title="No risk assessments yet" action={{ label: 'Run Risk Analysis', onClick: handleAnalyze }} />
      )}

      {assessments && assessments.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Component</th>
                <th className="px-4 py-3 font-medium">Probability</th>
                <th className="px-4 py-3 font-medium">Impact</th>
                <th className="px-4 py-3 font-medium">Risk Score</th>
                <th className="px-4 py-3 font-medium">Level</th>
                <th className="px-4 py-3 font-medium">Method</th>
                <th className="px-4 py-3 font-medium">Assessed</th>
              </tr>
            </thead>
            <tbody>
              {assessments.map((r) => (
                <tr key={r.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3 font-medium">{r.componentName}</td>
                  <td className="px-4 py-3 tabular-nums">{r.probability.toFixed(2)}</td>
                  <td className="px-4 py-3 tabular-nums">{r.impact.toFixed(1)}</td>
                  <td className="px-4 py-3 tabular-nums font-semibold">{r.riskScore.toFixed(1)}</td>
                  <td className="px-4 py-3">
                    <RiskBadge level={r.riskLevel} />
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold',
                        r.predictionMethod === 'HYBRID'
                          ? 'text-violet-500 bg-violet-500/10 border-violet-500/30'
                          : r.predictionMethod === 'ML'
                            ? 'text-cyan-500 bg-cyan-500/10 border-cyan-500/30'
                            : 'text-slate-400 bg-slate-400/10 border-slate-400/30'
                      )}
                    >
                      {r.predictionMethod ?? 'RULE_BASED'}
                    </span>
                    {r.predictionMethod === 'HYBRID' && r.mlConfidence != null && (
                      <span className="ml-1.5 text-xs text-muted-foreground">
                        (ML: {r.mlRiskLevel}, {Math.round(r.mlConfidence * 100)}% confidence)
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDate(r.assessedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
