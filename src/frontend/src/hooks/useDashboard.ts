import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api/dashboardApi';

export function useDashboard(projectId: number | undefined) {
  return useQuery({
    queryKey: ['dashboard', projectId],
    queryFn: () => dashboardApi.getDashboard(projectId as number),
    enabled: !!projectId,
    refetchInterval: 60_000,
  });
}
