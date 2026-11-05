import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});
export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
  fullName: z.string().min(2, 'Full name must be at least 2 characters'),
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  role: z.enum(['ADMIN', 'DEVELOPER', 'TESTER', 'SECURITY_ANALYST']).optional(),
});
export type RegisterFormValues = z.infer<typeof registerSchema>;

export const projectSchema = z.object({
  name: z.string().min(1, 'Project name is required'),
  description: z.string().optional(),
  repositoryUrl: z.string().optional(),
  branch: z.string().optional(),
  technologyStack: z.string().optional(),
});
export type ProjectFormValues = z.infer<typeof projectSchema>;

export const testCaseSchema = z.object({
  title: z.string().min(1, 'Title is required'),
  description: z.string().optional(),
  testType: z.enum(['FUNCTIONAL', 'UNIT', 'INTEGRATION', 'API', 'REGRESSION', 'SECURITY', 'PERFORMANCE']),
  priority: z.string().optional(),
  preconditions: z.string().optional(),
  testSteps: z.string().optional(),
  expectedResult: z.string().optional(),
  relatedComponent: z.string().optional(),
});
export type TestCaseFormValues = z.infer<typeof testCaseSchema>;
