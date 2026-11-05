import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate, useParams } from 'react-router-dom';
import { Save } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Select } from '@/components/ui/Select';
import { Textarea } from '@/components/ui/Textarea';
import { useCreateTestCase } from '@/hooks/useTesting';
import { useToast } from '@/components/ui/Toast';
import { testCaseSchema, type TestCaseFormValues } from '@/utils/validators';
import type { ApiError } from '@/api/axios';

export default function CreateTestCase() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { show } = useToast();
  const createTestCase = useCreateTestCase(projectId);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TestCaseFormValues>({ resolver: zodResolver(testCaseSchema), defaultValues: { testType: 'FUNCTIONAL', priority: 'MEDIUM' } });

  const onSubmit = async (values: TestCaseFormValues) => {
    try {
      await createTestCase.mutateAsync(values);
      show('Test case created successfully', 'success');
      navigate(`/projects/${projectId}/tests`);
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div>
        <h1 className="text-xl font-bold tracking-tight">New Test Case</h1>
        <p className="text-sm text-muted-foreground">Define a new test case for this project</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Test case details</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
            <div>
              <Label htmlFor="title">Title</Label>
              <Input id="title" placeholder="Verify login with valid credentials" error={errors.title?.message} {...register('title')} />
              {errors.title && <p className="mt-1 text-xs text-danger">{errors.title.message}</p>}
            </div>

            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea id="description" {...register('description')} />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="testType">Test type</Label>
                <Select id="testType" {...register('testType')}>
                  <option value="FUNCTIONAL">Functional</option>
                  <option value="UNIT">Unit</option>
                  <option value="INTEGRATION">Integration</option>
                  <option value="API">API</option>
                  <option value="REGRESSION">Regression</option>
                  <option value="SECURITY">Security</option>
                  <option value="PERFORMANCE">Performance</option>
                </Select>
              </div>
              <div>
                <Label htmlFor="priority">Priority</Label>
                <Select id="priority" {...register('priority')}>
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                  <option value="CRITICAL">Critical</option>
                </Select>
              </div>
            </div>

            <div>
              <Label htmlFor="preconditions">Preconditions</Label>
              <Textarea id="preconditions" placeholder="User account exists and is active" {...register('preconditions')} />
            </div>

            <div>
              <Label htmlFor="testSteps">Test steps</Label>
              <Textarea id="testSteps" rows={4} placeholder="1. Navigate to login page&#10;2. Enter valid credentials&#10;3. Click submit" {...register('testSteps')} />
            </div>

            <div>
              <Label htmlFor="expectedResult">Expected result</Label>
              <Textarea id="expectedResult" placeholder="User is redirected to the dashboard" {...register('expectedResult')} />
            </div>

            <div>
              <Label htmlFor="relatedComponent">Related component</Label>
              <Input id="relatedComponent" placeholder="AuthController.java" {...register('relatedComponent')} />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate(`/projects/${projectId}/tests`)}>
                Cancel
              </Button>
              <Button type="submit" isLoading={createTestCase.isPending}>
                <Save className="h-4 w-4" /> Create Test Case
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
