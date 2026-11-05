import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { riskApi } from '@/api/riskApi';

export function useRiskAssessments(projectId: number | undefined) {
  return useQuery({
    queryKey: ['risk', 'assessments', projectId],
    queryFn: () => riskApi.getByProject(projectId as number),
    enabled: !!projectId,
  });
}

export function useRiskSummary(projectId: number | undefined) {
  return useQuery({
    queryKey: ['risk', 'summary', projectId],
    queryFn: () => riskApi.getSummary(projectId as number),
    enabled: !!projectId,
  });
}

export function useRunRiskAnalysis(projectId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => riskApi.analyze(projectId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['risk', 'assessments', projectId] });
      qc.invalidateQueries({ queryKey: ['risk', 'summary', projectId] });
      qc.invalidateQueries({ queryKey: ['dashboard', projectId] });
      qc.invalidateQueries({ queryKey: ['projects', 'detail', projectId] });
    },
  });
}
