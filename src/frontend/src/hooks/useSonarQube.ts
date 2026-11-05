import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sonarQubeApi } from '@/api/sonarqubeApi';

export function useSonarQubeStatus() {
  return useQuery({
    queryKey: ['sonarqube', 'status'],
    queryFn: () => sonarQubeApi.getStatus(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useSonarQubeLatest(projectId: number | undefined) {
  return useQuery({
    queryKey: ['sonarqube', 'latest', projectId],
    queryFn: () => sonarQubeApi.getLatest(projectId as number),
    enabled: !!projectId,
    retry: false,
  });
}

export function useSonarQubeHistory(projectId: number | undefined) {
  return useQuery({
    queryKey: ['sonarqube', 'history', projectId],
    queryFn: () => sonarQubeApi.getHistory(projectId as number),
    enabled: !!projectId,
  });
}

export function useImportSonarQube(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (sonarProjectKey?: string) => sonarQubeApi.importLatest(projectId, sonarProjectKey),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sonarqube', 'latest', projectId] });
      qc.invalidateQueries({ queryKey: ['sonarqube', 'history', projectId] });
      qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
    },
  });
}
