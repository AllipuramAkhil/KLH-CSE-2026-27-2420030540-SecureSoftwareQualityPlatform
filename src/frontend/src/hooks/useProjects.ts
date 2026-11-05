import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectApi, type ProjectListParams } from '@/api/projectApi';
import type { ProjectRequest } from '@/types/project';

export const projectKeys = {
  all: ['projects'] as const,
  list: (params: ProjectListParams) => [...projectKeys.all, 'list', params] as const,
  detail: (id: number) => [...projectKeys.all, 'detail', id] as const,
};

export function useProjectList(params: ProjectListParams = {}) {
  return useQuery({
    queryKey: projectKeys.list(params),
    queryFn: () => projectApi.getAll(params),
  });
}

export function useProject(id: number | undefined) {
  return useQuery({
    queryKey: projectKeys.detail(id ?? -1),
    queryFn: () => projectApi.getById(id as number),
    enabled: !!id,
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProjectRequest) => projectApi.create(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: projectKeys.all }),
  });
}

export function useUpdateProject(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ProjectRequest) => projectApi.update(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: projectKeys.all });
      qc.invalidateQueries({ queryKey: projectKeys.detail(id) });
    },
  });
}

export function useDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => projectApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: projectKeys.all }),
  });
}
