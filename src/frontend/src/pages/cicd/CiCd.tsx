import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { GitBranch, PlayCircle, GitCommitHorizontal } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { cicdApi } from '@/api/cicdApi';
import { useToast } from '@/components/ui/Toast';
import { formatDateTime } from '@/utils/formatters';
import type { ApiError } from '@/api/axios';

export default function CiCd() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();
  const qc = useQueryClient();

  const { data: pipelines, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['cicd', projectId],
    queryFn: () => cicdApi.getByProject(projectId),
    enabled: !!projectId,
  });

  const trigger = useMutation({
    mutationFn: () => cicdApi.trigger(projectId),
    onSuccess: () => {
      show('Pipeline triggered successfully', 'success');
      qc.invalidateQueries({ queryKey: ['cicd', projectId] });
    },
    onError: (err) => show((err as unknown as ApiError).message, 'error'),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">CI/CD Pipelines</h2>
          <p className="text-sm text-muted-foreground">Continuous quality &amp; security monitoring across pipeline runs</p>
        </div>
        <Button onClick={() => trigger.mutate()} isLoading={trigger.isPending}>
          <PlayCircle className="h-4 w-4" /> Run Pipeline
        </Button>
      </div>

      {isLoading && <LoadingSpinner label="Loading pipelines…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {pipelines && pipelines.length === 0 && (
        <EmptyState icon={<GitBranch className="h-8 w-8" />} title="No pipeline runs yet" action={{ label: 'Run Pipeline', onClick: () => trigger.mutate() }} />
      )}

      {pipelines && pipelines.length > 0 && (
        <div className="space-y-3">
          {pipelines.map((p) => (
            <Card key={p.id}>
              <CardContent className="flex flex-wrap items-center gap-4 pt-4">
                <StatusBadge status={p.status} />
                <div className="flex items-center gap-1.5 text-sm">
                  <GitBranch className="h-3.5 w-3.5 text-muted-foreground" />
                  {p.branch || '—'}
                </div>
                <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                  <GitCommitHorizontal className="h-3.5 w-3.5" />
                  {p.commitHash?.slice(0, 8) || '—'}
                </div>
                <span className="text-sm text-muted-foreground">Build {p.buildNumber || '—'}</span>
                <span className="text-xs text-muted-foreground">{p.triggerSource || '—'}</span>
                <div className="ml-auto text-right text-xs text-muted-foreground">
                  <p>Started: {formatDateTime(p.startTime)}</p>
                  <p>Completed: {formatDateTime(p.endTime)}</p>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
