import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, FileText, Trash2, Eye } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Modal } from '@/components/ui/Modal';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { reportApi } from '@/api/reportApi';
import { useToast } from '@/components/ui/Toast';
import { formatDateTime, formatScore } from '@/utils/formatters';
import type { ReportResponse } from '@/types/report';
import type { ApiError } from '@/api/axios';

export default function Reports() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();
  const qc = useQueryClient();
  const [viewing, setViewing] = useState<ReportResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const { data: reports, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['reports', projectId],
    queryFn: () => reportApi.getByProject(projectId),
    enabled: !!projectId,
  });

  const generate = useMutation({
    mutationFn: () => reportApi.generate(projectId),
    onSuccess: () => {
      show('Report generated successfully', 'success');
      qc.invalidateQueries({ queryKey: ['reports', projectId] });
    },
    onError: (err) => show((err as unknown as ApiError).message, 'error'),
  });

  const remove = useMutation({
    mutationFn: (reportId: number) => reportApi.delete(reportId),
    onSuccess: () => {
      show('Report deleted', 'success');
      qc.invalidateQueries({ queryKey: ['reports', projectId] });
      setDeleteTarget(null);
    },
    onError: (err) => show((err as unknown as ApiError).message, 'error'),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">Reports</h2>
          <p className="text-sm text-muted-foreground">Centralized quality, security, risk, and testing reports</p>
        </div>
        <Button onClick={() => generate.mutate()} isLoading={generate.isPending}>
          <Plus className="h-4 w-4" /> Generate Report
        </Button>
      </div>

      {isLoading && <LoadingSpinner label="Loading reports…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {reports && reports.length === 0 && (
        <EmptyState icon={<FileText className="h-8 w-8" />} title="No reports yet" action={{ label: 'Generate Report', onClick: () => generate.mutate() }} />
      )}

      {reports && reports.length > 0 && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {reports.map((r) => (
            <Card key={r.id}>
              <CardContent className="space-y-3 pt-4">
                <div className="flex items-start justify-between gap-2">
                  <p className="font-medium">{r.title}</p>
                  <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                </div>
                <p className="text-xs text-muted-foreground">{formatDateTime(r.generatedAt)}</p>
                <div className="grid grid-cols-3 gap-2 text-center text-xs">
                  <MiniStat label="Quality" value={formatScore(r.content.qualityScore)} />
                  <MiniStat label="Security" value={formatScore(r.content.securityScore)} />
                  <MiniStat label="Risk" value={formatScore(r.content.riskScore)} />
                </div>
                <div className="flex justify-end gap-1 pt-1">
                  <Button variant="outline" size="sm" onClick={() => setViewing(r)}>
                    <Eye className="h-3.5 w-3.5" /> View
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(r.id)} aria-label="Delete report">
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Modal open={!!viewing} onClose={() => setViewing(null)} title={viewing?.title} className="max-w-2xl">
        {viewing && (
          <pre className="max-h-[60vh] overflow-auto rounded-md bg-muted/40 p-4 font-mono text-xs">
            {JSON.stringify(viewing.content, null, 2)}
          </pre>
        )}
      </Modal>

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && remove.mutate(deleteTarget)}
        title="Delete report?"
        confirmLabel="Delete"
        isLoading={remove.isPending}
      />
    </div>
  );
}

function MiniStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-muted/40 py-2">
      <p className="font-semibold">{value}</p>
      <p className="text-muted-foreground">{label}</p>
    </div>
  );
}
