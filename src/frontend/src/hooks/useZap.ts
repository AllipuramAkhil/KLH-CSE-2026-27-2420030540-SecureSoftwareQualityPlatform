import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { zapApi } from '@/api/zapApi';

export function useZapStatus() {
  return useQuery({
    queryKey: ['zap', 'status'],
    queryFn: () => zapApi.getStatus(),
    staleTime: 5 * 60 * 1000,
  });
}

/** Polls spider progress every 2s while scanId is set and not yet complete. */
export function useZapScanStatus(scanId: string | null) {
  return useQuery({
    queryKey: ['zap', 'scan-status', scanId],
    queryFn: () => zapApi.getScanStatus(scanId as string),
    enabled: !!scanId,
    refetchInterval: (query) => (query.state.data?.complete ? false : 2000),
  });
}

function useInvalidateZap(projectId: number) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ['security', 'vulnerabilities', projectId] });
    qc.invalidateQueries({ queryKey: ['zap', 'findings', projectId] });
    qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
    qc.invalidateQueries({ queryKey: ['projects', 'detail', projectId] });
  };
}

export function useTriggerZapScan(projectId: number) {
  return useMutation({
    mutationFn: (targetUrl: string) => zapApi.triggerBaselineScan(projectId, targetUrl),
  });
}

export function useCollectZapResults(projectId: number) {
  const invalidate = useInvalidateZap(projectId);
  return useMutation({
    mutationFn: (targetUrl: string) => zapApi.collectResults(projectId, targetUrl),
    onSuccess: invalidate,
  });
}

export function useImportZapFile(projectId: number) {
  const invalidate = useInvalidateZap(projectId);
  return useMutation({
    mutationFn: (file: File) => zapApi.importFromFile(projectId, file),
    onSuccess: invalidate,
  });
}
