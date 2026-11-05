import { useForm } from 'react-hook-form';
import { useEffect } from 'react';
import { Save, User as UserIcon } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Badge } from '@/components/ui/Badge';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/components/ui/Toast';
import { userApi } from '@/api/userApi';
import { formatDate, initials } from '@/utils/formatters';
import type { UserUpdateRequest } from '@/types/user';
import type { ApiError } from '@/api/axios';

export default function Profile() {
  const { user, refreshUser } = useAuth();
  const { show } = useToast();

  const { register, handleSubmit, reset, formState: { isSubmitting } } = useForm<UserUpdateRequest>();

  useEffect(() => {
    if (user) reset({ fullName: user.fullName });
  }, [user, reset]);

  const onSubmit = async (values: UserUpdateRequest) => {
    try {
      await userApi.updateMe(values);
      await refreshUser();
      show('Profile updated successfully', 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  if (!user) return null;

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Profile</h1>
        <p className="text-sm text-muted-foreground">Your ASSQP account information</p>
      </div>

      <Card>
        <CardContent className="flex items-center gap-4 pt-4">
          <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10 text-lg font-bold text-primary">
            {initials(user.fullName)}
          </div>
          <div>
            <p className="font-semibold">{user.fullName}</p>
            <p className="text-sm text-muted-foreground">{user.email}</p>
            <div className="mt-1 flex items-center gap-2">
              <Badge variant="outline">{user.role}</Badge>
              <span className="text-xs text-muted-foreground">Joined {formatDate(user.createdAt)}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <UserIcon className="h-4 w-4" /> Edit profile
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div>
              <Label htmlFor="fullName">Full name</Label>
              <Input id="fullName" {...register('fullName')} />
            </div>
            <div>
              <Label>Email</Label>
              <Input value={user.email} disabled />
              <p className="mt-1 text-xs text-muted-foreground">Email cannot be changed from this page.</p>
            </div>
            <div className="flex justify-end">
              <Button type="submit" isLoading={isSubmitting}>
                <Save className="h-4 w-4" /> Save Changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
