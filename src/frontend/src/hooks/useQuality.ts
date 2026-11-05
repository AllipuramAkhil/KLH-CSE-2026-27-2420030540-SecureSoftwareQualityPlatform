import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { qualityApi } from '@/api/qualityApi';

export function useQualityHistory(projectId: number | undefined) {
  return useQuery({
    queryKey: ['quality', 'history', projectId],
    queryFn: () => qualityApi.getHistory(projectId as number),
    enabled: !!projectId,
  });
}

export function useQualityLatest(projectId: number | undefined) {
  return useQuery({
    queryKey: ['quality', 'latest', projectId],
    queryFn: () => qualityApi.getLatest(projectId as number),
    enabled: !!projectId,
    retry: false,
  });
}

export function useRunQualityAnalysis(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => qualityApi.analyze(projectId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['quality', 'history', projectId] });
      qc.invalidateQueries({ queryKey: ['quality', 'latest', projectId] });
      qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
      qc.invalidateQueries({ queryKey: ['projects', 'detail', projectId] });
    },
  });
}
