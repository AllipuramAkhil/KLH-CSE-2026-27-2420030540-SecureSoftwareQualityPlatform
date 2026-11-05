import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { churnApi } from '@/api/churnApi';

export function useGitChurnStatus() {
  return useQuery({
    queryKey: ['churn', 'status'],
    queryFn: () => churnApi.getStatus(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCodeChurn(projectId: number) {
  return useQuery({
    queryKey: ['churn', 'project', projectId],
    queryFn: () => churnApi.getByProject(projectId),
    enabled: !!projectId,
  });
}

export function useAnalyzeCodeChurn(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => churnApi.analyze(projectId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['churn', 'project', projectId] });
      qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
    },
  });
}
