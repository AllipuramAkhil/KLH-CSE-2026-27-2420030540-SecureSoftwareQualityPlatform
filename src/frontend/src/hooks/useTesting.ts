import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { testCaseApi, type TestCaseListParams } from '@/api/testCaseApi';
import { testExecutionApi } from '@/api/testExecutionApi';
import type { TestCaseRequest, TestExecutionRequest } from '@/types/testing';
import type { TestCaseStatus } from '@/types/enums';

export function useTestCases(projectId: number | undefined, params: TestCaseListParams = {}) {
  return useQuery({
    queryKey: ['tests', projectId, params],
    queryFn: () => testCaseApi.getByProject(projectId as number, params),
    enabled: !!projectId,
  });
}

export function useTestCase(id: number | undefined) {
  return useQuery({
    queryKey: ['test', id],
    queryFn: () => testCaseApi.getById(id as number),
    enabled: !!id,
  });
}

export function useCreateTestCase(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: TestCaseRequest) => testCaseApi.create(projectId, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tests', projectId] }),
  });
}

export function useUpdateTestCase(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: TestCaseRequest) => testCaseApi.update(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tests'] });
      qc.invalidateQueries({ queryKey: ['test', id] });
    },
  });
}

export function useUpdateTestCaseStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: TestCaseStatus }) =>
      testCaseApi.updateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tests'] }),
  });
}

export function useDeleteTestCase(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => testCaseApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tests', projectId] }),
  });
}

export function useTestExecutions(testId: number | undefined) {
  return useQuery({
    queryKey: ['executions', 'test', testId],
    queryFn: () => testExecutionApi.getByTestCase(testId as number),
    enabled: !!testId,
  });
}

export function useProjectExecutions(projectId: number | undefined) {
  return useQuery({
    queryKey: ['executions', 'project', projectId],
    queryFn: () => testExecutionApi.getByProject(projectId as number),
    enabled: !!projectId,
  });
}

export function useExecuteTest(testId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: TestExecutionRequest) => testExecutionApi.execute(testId, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['executions', 'test', testId] });
      qc.invalidateQueries({ queryKey: ['test', testId] });
      qc.invalidateQueries({ queryKey: ['tests'] });
    },
  });
}
