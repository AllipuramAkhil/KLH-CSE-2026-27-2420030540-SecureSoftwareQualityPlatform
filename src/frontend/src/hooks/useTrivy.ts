import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { trivyApi } from '@/api/trivyApi';

export function useTrivyStatus() {
  return useQuery({
    queryKey: ['trivy', 'status'],
    queryFn: () => trivyApi.getStatus(),
    staleTime: 5 * 60 * 1000,
  });
}

function useInvalidateTrivy(projectId: number) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ['security', 'vulnerabilities', projectId] });
    qc.invalidateQueries({ queryKey: ['trivy', 'findings', projectId] });
    qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
    qc.invalidateQueries({ queryKey: ['projects', 'detail', projectId] });
  };
}

export function useScanFilesystem(projectId: number) {
  const invalidate = useInvalidateTrivy(projectId);
  return useMutation({
    mutationFn: (targetPath?: string) => trivyApi.scanFilesystem(projectId, targetPath),
    onSuccess: invalidate,
  });
}

export function useScanImage(projectId: number) {
  const invalidate = useInvalidateTrivy(projectId);
  return useMutation({
    mutationFn: (imageRef: string) => trivyApi.scanImage(projectId, imageRef),
    onSuccess: invalidate,
  });
}

export function useImportTrivyFile(projectId: number) {
  const invalidate = useInvalidateTrivy(projectId);
  return useMutation({
    mutationFn: (file: File) => trivyApi.importFromFile(projectId, file),
    onSuccess: invalidate,
  });
}
