import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, UserPlus } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Select } from '@/components/ui/Select';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/components/ui/Toast';
import { registerSchema, type RegisterFormValues } from '@/utils/validators';
import { ROUTES } from '@/constants/routes';
import type { ApiError } from '@/api/axios';

export default function Register() {
  const { register: registerUser } = useAuth();
  const { show } = useToast();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema), defaultValues: { role: 'DEVELOPER' } });

  const onSubmit = async (values: RegisterFormValues) => {
    setServerError(null);
    try {
      await registerUser(values);
      show('Account created successfully', 'success');
      navigate(ROUTES.DASHBOARD, { replace: true });
    } catch (err) {
      setServerError((err as ApiError).message);
    }
  };

  return (
    <div>
      <h2 className="mb-5 text-center text-base font-semibold">Create your account</h2>

      {serverError && (
        <div className="mb-4 rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger">
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div>
          <Label htmlFor="fullName">Full name</Label>
          <Input id="fullName" autoComplete="name" placeholder="Akhil Allipuram" error={errors.fullName?.message} {...register('fullName')} />
          {errors.fullName && <p className="mt-1 text-xs text-danger">{errors.fullName.message}</p>}
        </div>

        <div>
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" autoComplete="email" placeholder="you@example.com" error={errors.email?.message} {...register('email')} />
          {errors.email && <p className="mt-1 text-xs text-danger">{errors.email.message}</p>}
        </div>

        <div>
          <Label htmlFor="password">Password</Label>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="At least 8 characters"
              error={errors.password?.message}
              className="pr-9"
              {...register('password')}
            />
            <button
              type="button"
              onClick={() => setShowPassword((s) => !s)}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
          {errors.password && <p className="mt-1 text-xs text-danger">{errors.password.message}</p>}
        </div>

        <div>
          <Label htmlFor="role">Role</Label>
          <Select id="role" {...register('role')}>
            <option value="DEVELOPER">Developer</option>
            <option value="TESTER">Tester</option>
            <option value="SECURITY_ANALYST">Security Analyst</option>
            <option value="ADMIN">Admin</option>
          </Select>
        </div>

        <Button type="submit" className="w-full" isLoading={isSubmitting}>
          <UserPlus className="h-4 w-4" /> Create Account
        </Button>
      </form>

      <p className="mt-5 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to={ROUTES.LOGIN} className="font-medium text-primary hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
