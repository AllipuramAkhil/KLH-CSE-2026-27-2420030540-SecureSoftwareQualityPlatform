import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Plus, ListChecks, Trash2, PlayCircle } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { FilterBar } from '@/components/common/FilterBar';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Pagination } from '@/components/ui/Pagination';
import { useTestCases, useDeleteTestCase } from '@/hooks/useTesting';
import { useToast } from '@/components/ui/Toast';
import { formatDateTime } from '@/utils/formatters';
import type { TestType, TestCaseStatus } from '@/types/enums';
import type { ApiError } from '@/api/axios';

const TEST_TYPES: TestType[] = ['FUNCTIONAL', 'UNIT', 'INTEGRATION', 'API', 'REGRESSION', 'SECURITY', 'PERFORMANCE'];
const STATUSES: TestCaseStatus[] = ['ACTIVE', 'DRAFT', 'DEPRECATED', 'UNDER_REVIEW'];

export default function TestCases() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { show } = useToast();

  const [page, setPage] = useState(0);
  const [type, setType] = useState<TestType | ''>('');
  const [status, setStatus] = useState<TestCaseStatus | ''>('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const { data, isLoading, isError, error, refetch } = useTestCases(projectId, {
    page,
    size: 10,
    type: type || undefined,
    status: status || undefined,
  });
  const deleteTestCase = useDeleteTestCase(projectId);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteTestCase.mutateAsync(deleteTarget);
      show('Test case deleted', 'success');
      setDeleteTarget(null);
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">Test Cases</h2>
          <p className="text-sm text-muted-foreground">Manage and execute test cases for this project</p>
        </div>
        <Button onClick={() => navigate(`/projects/${projectId}/tests/new`)}>
          <Plus className="h-4 w-4" /> New Test Case
        </Button>
      </div>

      <FilterBar>
        <Select value={type} onChange={(e) => setType(e.target.value as TestType | '')} className="w-44">
          <option value="">All Types</option>
          {TEST_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </Select>
        <Select value={status} onChange={(e) => setStatus(e.target.value as TestCaseStatus | '')} className="w-40">
          <option value="">All Statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </Select>
      </FilterBar>

      {isLoading && <LoadingSpinner label="Loading test cases…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {data && data.content.length === 0 && (
        <EmptyState
          icon={<ListChecks className="h-8 w-8" />}
          title="No test cases yet"
          description="Create your first test case for this project."
          action={{ label: 'New Test Case', onClick: () => navigate(`/projects/${projectId}/tests/new`) }}
        />
      )}

      {data && data.content.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[860px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Title</th>
                <th className="px-4 py-3 font-medium">Type</th>
                <th className="px-4 py-3 font-medium">Priority</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Risk Score</th>
                <th className="px-4 py-3 font-medium">Last Execution</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.id} className="border-b border-border last:border-0 hover:bg-muted/40">
                  <td className="px-4 py-3">
                    <button
                      onClick={() => navigate(`/projects/${projectId}/tests/${t.id}/executions`)}
                      className="font-medium text-foreground hover:text-primary hover:underline"
                    >
                      {t.title}
                    </button>
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{t.testType}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{t.priority}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={t.status} />
                  </td>
                  <td className="px-4 py-3 tabular-nums">{t.riskScore.toFixed(1)}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDateTime(t.lastExecution)}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="Execute test"
                        onClick={() => navigate(`/projects/${projectId}/tests/${t.id}/executions`)}
                      >
                        <PlayCircle className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" aria-label="Delete test case" onClick={() => setDeleteTarget(t.id)}>
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="px-4 pb-3">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete test case?"
        description="This will permanently delete the test case and its execution history."
        confirmLabel="Delete"
        isLoading={deleteTestCase.isPending}
      />
    </div>
  );
}
