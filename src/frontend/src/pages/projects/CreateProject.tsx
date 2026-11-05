import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { Save } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { useCreateProject } from '@/hooks/useProjects';
import { useToast } from '@/components/ui/Toast';
import { projectSchema, type ProjectFormValues } from '@/utils/validators';
import { ROUTES } from '@/constants/routes';
import { appStore } from '@/store/appStore';
import type { ApiError } from '@/api/axios';

export default function CreateProject() {
  const navigate = useNavigate();
  const { show } = useToast();
  const createProject = useCreateProject();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProjectFormValues>({ resolver: zodResolver(projectSchema) });

  const onSubmit = async (values: ProjectFormValues) => {
    try {
      const project = await createProject.mutateAsync(values);
      appStore.setLastProjectId(project.id);
      show('Project created successfully', 'success');
      navigate(ROUTES.PROJECT_DETAILS(project.id));
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Create Project</h1>
        <p className="text-sm text-muted-foreground">Set up a new project for quality &amp; security analysis</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Project details</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div>
              <Label htmlFor="name">Project name</Label>
              <Input id="name" placeholder="ASSQP Capstone" error={errors.name?.message} {...register('name')} />
              {errors.name && <p className="mt-1 text-xs text-danger">{errors.name.message}</p>}
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" placeholder="Brief description of the project" {...register('description')} />
            </div>

            <div>
              <Label htmlFor="repositoryUrl">Repository URL</Label>
              <Input id="repositoryUrl" placeholder="https://github.com/org/repo" {...register('repositoryUrl')} />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="branch">Branch</Label>
                <Input id="branch" placeholder="main" {...register('branch')} />
              </div>
              <div>
                <Label htmlFor="technologyStack">Technology stack</Label>
                <Input id="technologyStack" placeholder="Java 21, Spring Boot" {...register('technologyStack')} />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate(ROUTES.PROJECTS)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={createProject.isPending}>
                <Save className="h-4 w-4" /> Create Project
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
