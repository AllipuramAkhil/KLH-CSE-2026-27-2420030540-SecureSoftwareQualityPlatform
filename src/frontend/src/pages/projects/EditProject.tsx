import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useParams } from 'react-router-dom';
import { Save } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useProject, useUpdateProject } from '@/hooks/useProjects';
import { useToast } from '@/components/ui/Toast';
import { projectSchema, type ProjectFormValues } from '@/utils/validators';
import { ROUTES } from '@/constants/routes';
import type { ApiError } from '@/api/axios';

export default function EditProject() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { show } = useToast();
  const { data: project, isLoading, isError, error, refetch } = useProject(projectId);
  const updateProject = useUpdateProject(projectId);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProjectFormValues>({ resolver: zodResolver(projectSchema) });

  useEffect(() => {
    if (project) {
      reset({
        name: project.name,
        description: project.description ?? '',
        repositoryUrl: project.repositoryUrl ?? '',
        branch: project.branch ?? '',
        technologyStack: project.technologyStack ?? '',
      });
    }
  }, [project, reset]);

  const onSubmit = async (values: ProjectFormValues) => {
    try {
      await updateProject.mutateAsync(values);
      show('Project updated successfully', 'success');
      navigate(ROUTES.PROJECT_DETAILS(projectId));
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  if (isLoading) return <LoadingSpinner label="Loading project…" />;
  if (isError) return <ErrorState error={error} onRetry={() => refetch()} />;

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Edit Project</h1>
        <p className="text-sm text-muted-foreground">Update project details</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Project details</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div>
              <Label htmlFor="name">Project name</Label>
              <Input id="name" error={errors.name?.message} {...register('name')} />
              {errors.name && <p className="mt-1 text-xs text-danger">{errors.name.message}</p>}
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" {...register('description')} />
            </div>

            <div>
              <Label htmlFor="repositoryUrl">Repository URL</Label>
              <Input id="repositoryUrl" {...register('repositoryUrl')} />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="branch">Branch</Label>
                <Input id="branch" {...register('branch')} />
              </div>
              <div>
                <Label htmlFor="technologyStack">Technology stack</Label>
                <Input id="technologyStack" {...register('technologyStack')} />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate(ROUTES.PROJECT_DETAILS(projectId))}>
                Cancel
              </Button>
              <Button type="submit" isLoading={updateProject.isPending}>
                <Save className="h-4 w-4" /> Save Changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
