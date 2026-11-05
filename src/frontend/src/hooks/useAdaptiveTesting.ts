import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adaptiveTestingApi } from '@/api/adaptiveTestingApi';

export function useAdaptiveRecommendations(projectId: number | undefined) {
  return useQuery({
    queryKey: ['adaptive', 'recommendations', projectId],
    queryFn: () => adaptiveTestingApi.getRecommendations(projectId as number),
    enabled: !!projectId,
    retry: false,
  });
}

export function usePrioritizeTests(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => adaptiveTestingApi.prioritize(projectId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['adaptive', 'recommendations', projectId] }),
  });
}

export function useAdaptiveHistory(projectId: number | undefined) {
  return useQuery({
    queryKey: ['adaptive', 'history', projectId],
    queryFn: () => adaptiveTestingApi.getHistory(projectId as number),
    enabled: !!projectId,
  });
}

export function useSubmitAdaptiveFeedback(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (summary: string) => adaptiveTestingApi.submitFeedback(projectId, summary),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['adaptive', 'history', projectId] }),
  });
}
