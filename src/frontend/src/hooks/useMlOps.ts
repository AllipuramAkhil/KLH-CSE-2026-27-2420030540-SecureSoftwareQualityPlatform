import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mlApi } from '@/api/mlApi';

export function useMlStatus() {
  return useQuery({
    queryKey: ['ml', 'status'],
    queryFn: () => mlApi.getStatus(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useModelInfo(enabled: boolean) {
  return useQuery({
    queryKey: ['ml', 'model-info'],
    queryFn: () => mlApi.getModelInfo(),
    enabled,
    retry: false,
  });
}

export function useModelMetrics(enabled: boolean) {
  return useQuery({
    queryKey: ['ml', 'metrics'],
    queryFn: () => mlApi.getMetrics(),
    enabled,
    retry: false,
  });
}

export function usePredictionHistory(projectId: number, enabled: boolean) {
  return useQuery({
    queryKey: ['ml', 'prediction-history', projectId],
    queryFn: () => mlApi.getPredictionHistory(projectId),
    enabled,
    retry: false,
  });
}

export function useRetrainModel() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (compareLogreg: boolean) => mlApi.retrain(compareLogreg),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ml', 'model-info'] });
      qc.invalidateQueries({ queryKey: ['ml', 'metrics'] });
    },
  });
}
