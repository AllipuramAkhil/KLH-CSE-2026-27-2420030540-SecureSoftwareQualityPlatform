import { useParams } from 'react-router-dom';
import { Zap, ArrowDown } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { RiskBadge } from '@/components/ui/RiskBadge';
import { Progress } from '@/components/ui/Progress';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { useAdaptiveRecommendations, usePrioritizeTests } from '@/hooks/useAdaptiveTesting';
import { useToast } from '@/components/ui/Toast';
import type { ApiError } from '@/api/axios';

const WORKFLOW_STEPS = [
  'Code Changes',
  'Risk Analysis',
  'Historical Results',
  'AI / Rule-Based Analysis',
  'Test Prioritization',
  'Recommended Execution Order',
];

export default function AdaptiveTesting() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();

  const { data, isLoading, isError, error, refetch } = useAdaptiveRecommendations(projectId);
  const prioritize = usePrioritizeTests(projectId);

  const handlePrioritize = async () => {
    try {
      const result = await prioritize.mutateAsync();
      show(`Prioritized ${result.totalTests} test case(s)`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold">
            <Zap className="h-5 w-5 text-primary" /> Adaptive Testing Engine
          </h2>
          <p className="text-sm text-muted-foreground">
            Dynamically prioritized test execution order based on risk, history, and vulnerabilities
          </p>
        </div>
        <Button onClick={handlePrioritize} isLoading={prioritize.isPending}>
          <Zap className="h-4 w-4" /> Prioritize Tests
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>How adaptive prioritization works</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center gap-1">
            {WORKFLOW_STEPS.map((step, i) => (
              <div key={step} className="flex flex-col items-center">
                <div className="rounded-full border border-border bg-muted/50 px-4 py-1.5 text-xs font-medium">{step}</div>
                {i < WORKFLOW_STEPS.length - 1 && <ArrowDown className="my-1 h-3.5 w-3.5 text-muted-foreground" />}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {isLoading && <LoadingSpinner label="Loading recommendations…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {data && data.recommendedTests.length === 0 && (
        <EmptyState
          icon={<Zap className="h-8 w-8" />}
          title="No prioritization run yet"
          description="Run the adaptive testing engine to get a recommended execution order."
          action={{ label: 'Prioritize Tests', onClick: handlePrioritize }}
        />
      )}

      {data && data.recommendedTests.length === 0 && (
        <EmptyState icon={<Zap className="h-8 w-8" />} title="No test cases to prioritize" description="Create test cases for this project first." />
      )}

      {data && data.recommendedTests.length > 0 && (
        <div className="space-y-3">
          <p className="text-sm text-muted-foreground">
            {data.totalTests} test case(s) — recommended execution order below
          </p>
          {data.recommendedTests.map((rec) => (
            <Card key={rec.testCaseId}>
              <CardContent className="flex items-center gap-4 pt-4">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
                  #{rec.recommendedOrder}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="truncate font-medium text-foreground">{rec.title}</p>
                    <RiskBadge level={rec.riskLevel} />
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{rec.reason}</p>
                  <div className="mt-2 flex items-center gap-2">
                    <Progress value={rec.priorityScore * 100} className="max-w-xs" />
                    <span className="text-xs font-semibold tabular-nums">{rec.priorityScore.toFixed(2)}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
