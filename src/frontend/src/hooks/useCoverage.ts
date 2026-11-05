import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { coverageApi } from '@/api/coverageApi';

export function useCoverageLatest(projectId: number | undefined) {
  return useQuery({
    queryKey: ['coverage', 'latest', projectId],
    queryFn: () => coverageApi.getLatest(projectId as number),
    enabled: !!projectId,
    retry: false,
  });
}

export function useCoverageTrend(projectId: number | undefined) {
  return useQuery({
    queryKey: ['coverage', 'trend', projectId],
    queryFn: () => coverageApi.getTrend(projectId as number),
    enabled: !!projectId,
  });
}

function useInvalidateCoverage(projectId: number) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ['coverage', 'latest', projectId] });
    qc.invalidateQueries({ queryKey: ['coverage', 'trend', projectId] });
    qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
  };
}

export function useImportCoverage(projectId: number) {
  const invalidate = useInvalidateCoverage(projectId);
  return useMutation({
    mutationFn: () => coverageApi.importFromDefaultPath(projectId),
    onSuccess: invalidate,
  });
}

export function useImportCoverageFile(projectId: number) {
  const invalidate = useInvalidateCoverage(projectId);
  return useMutation({
    mutationFn: (file: File) => coverageApi.importFromFile(projectId, file),
    onSuccess: invalidate,
  });
}
