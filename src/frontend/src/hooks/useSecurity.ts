import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { securityApi, type VulnerabilityFilterParams } from '@/api/securityApi';
import type { VulnerabilityStatus } from '@/types/enums';

export function useVulnerabilities(projectId: number | undefined, filters: VulnerabilityFilterParams = {}) {
  return useQuery({
    queryKey: ['security', 'vulnerabilities', projectId, filters],
    queryFn: () => securityApi.getVulnerabilities(projectId as number, filters),
    enabled: !!projectId,
  });
}

export function useVulnerability(id: number | undefined) {
  return useQuery({
    queryKey: ['security', 'vulnerability', id],
    queryFn: () => securityApi.getById(id as number),
    enabled: !!id,
  });
}

export function useRunSecurityAnalysis(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => securityApi.analyze(projectId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['security', 'vulnerabilities', projectId] });
      qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
      qc.invalidateQueries({ queryKey: ['projects', 'detail', projectId] });
    },
  });
}

export function useUpdateVulnerabilityStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: VulnerabilityStatus }) =>
      securityApi.updateStatus(id, status),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['security'] });
      qc.invalidateQueries({ queryKey: ['security', 'vulnerability', variables.id] });
    },
  });
}
