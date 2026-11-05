import { useParams } from 'react-router-dom';
import { PlayCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { useProjectExecutions } from '@/hooks/useTesting';
import { formatDateTime, formatDuration } from '@/utils/formatters';

export default function ExecutionHistory() {
  const { id } = useParams();
  const projectId = Number(id);
  const { data: executions, isLoading, isError, error, refetch } = useProjectExecutions(projectId);

  return (
    <div className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold">Execution History</h2>
        <p className="text-sm text-muted-foreground">All test executions recorded for this project</p>
      </div>

      {isLoading && <LoadingSpinner label="Loading execution history…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {executions && executions.length === 0 && (
        <EmptyState icon={<PlayCircle className="h-8 w-8" />} title="No executions recorded yet" />
      )}

      {executions && executions.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[700px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Environment</th>
                <th className="px-4 py-3 font-medium">Build</th>
                <th className="px-4 py-3 font-medium">Duration</th>
                <th className="px-4 py-3 font-medium">Started</th>
              </tr>
            </thead>
            <tbody>
              {executions.map((e) => (
                <tr key={e.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    <StatusBadge status={e.status} />
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{e.environment || '—'}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{e.buildNumber || '—'}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDuration(e.durationMs)}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDateTime(e.startTime)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
