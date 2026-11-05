import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, ChevronDown, ChevronRight, PlayCircle } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { Modal } from '@/components/ui/Modal';
import { useTestCase, useTestExecutions, useExecuteTest } from '@/hooks/useTesting';
import { useToast } from '@/components/ui/Toast';
import { formatDateTime, formatDuration } from '@/utils/formatters';
import type { ExecutionStatus } from '@/types/enums';
import type { TestExecutionRequest } from '@/types/testing';
import type { ApiError } from '@/api/axios';

const STATUSES: ExecutionStatus[] = ['PASSED', 'FAILED', 'SKIPPED', 'BLOCKED', 'ERROR'];

export default function TestExecution() {
  const { id, testId } = useParams();
  const projectId = Number(id);
  const testCaseId = Number(testId);
  const navigate = useNavigate();
  const { show } = useToast();
  const [modalOpen, setModalOpen] = useState(false);
  const [expanded, setExpanded] = useState<number | null>(null);

  const { data: testCase } = useTestCase(testCaseId);
  const { data: executions, isLoading, isError, error, refetch } = useTestExecutions(testCaseId);
  const executeTest = useExecuteTest(testCaseId);

  const { register, handleSubmit, reset } = useForm<TestExecutionRequest>({
    defaultValues: { status: 'PASSED', environment: 'local', buildNumber: '' },
  });

  const onSubmit = async (values: TestExecutionRequest) => {
    try {
      await executeTest.mutateAsync(values);
      show('Test execution recorded', 'success');
      setModalOpen(false);
      reset();
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(`/projects/${projectId}/tests`)} aria-label="Back to test cases">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h2 className="text-lg font-semibold">{testCase?.title ?? 'Test Execution'}</h2>
          <p className="text-sm text-muted-foreground">Execution history and results</p>
        </div>
        <Button className="ml-auto" onClick={() => setModalOpen(true)}>
          <PlayCircle className="h-4 w-4" /> Record Execution
        </Button>
      </div>

      {isLoading && <LoadingSpinner label="Loading executions…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {executions && executions.length === 0 && (
        <EmptyState
          icon={<PlayCircle className="h-8 w-8" />}
          title="No executions recorded yet"
          action={{ label: 'Record Execution', onClick: () => setModalOpen(true) }}
        />
      )}

      {executions && executions.length > 0 && (
        <div className="space-y-2">
          {executions.map((e) => (
            <Card key={e.id}>
              <button
                onClick={() => setExpanded(expanded === e.id ? null : e.id)}
                className="flex w-full items-center gap-3 p-4 text-left"
              >
                {expanded === e.id ? <ChevronDown className="h-4 w-4 shrink-0" /> : <ChevronRight className="h-4 w-4 shrink-0" />}
                <StatusBadge status={e.status} />
                <span className="text-sm text-muted-foreground">{formatDateTime(e.startTime)}</span>
                <span className="ml-auto text-xs text-muted-foreground">{formatDuration(e.durationMs)}</span>
              </button>
              {expanded === e.id && (
                <CardContent className="grid grid-cols-1 gap-3 border-t border-border pt-4 text-sm sm:grid-cols-2">
                  <Field label="Environment" value={e.environment || '—'} />
                  <Field label="Build" value={e.buildNumber || '—'} />
                  <Field label="Result" value={e.result || '—'} />
                  <Field label="Error Message" value={e.errorMessage || '—'} />
                  {e.logs && (
                    <div className="sm:col-span-2">
                      <p className="mb-1 text-xs text-muted-foreground">Logs</p>
                      <pre className="max-h-40 overflow-auto rounded-md bg-muted/50 p-2 font-mono text-xs">{e.logs}</pre>
                    </div>
                  )}
                </CardContent>
              )}
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title="Record Test Execution">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <Label htmlFor="status">Result</Label>
            <Select id="status" {...register('status')}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <Label htmlFor="environment">Environment</Label>
            <Input id="environment" {...register('environment')} />
          </div>
          <div>
            <Label htmlFor="buildNumber">Build number</Label>
            <Input id="buildNumber" {...register('buildNumber')} />
          </div>
          <div>
            <Label htmlFor="result">Result notes</Label>
            <Input id="result" {...register('result')} />
          </div>
          <div>
            <Label htmlFor="errorMessage">Error message (if any)</Label>
            <Input id="errorMessage" {...register('errorMessage')} />
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" isLoading={executeTest.isPending}>
              Save Execution
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-medium text-foreground">{value}</p>
    </div>
  );
}
