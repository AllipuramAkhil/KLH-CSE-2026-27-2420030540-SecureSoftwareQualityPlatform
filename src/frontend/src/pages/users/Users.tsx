import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Trash2, Users as UsersIcon } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { userApi } from '@/api/userApi';
import { useToast } from '@/components/ui/Toast';
import { formatDate, initials } from '@/utils/formatters';
import type { ApiError } from '@/api/axios';

export default function Users() {
  const { show } = useToast();
  const qc = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const { data: users, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['users'],
    queryFn: () => userApi.getAll(),
  });

  const remove = useMutation({
    mutationFn: (id: number) => userApi.delete(id),
    onSuccess: () => {
      show('User deleted', 'success');
      qc.invalidateQueries({ queryKey: ['users'] });
      setDeleteTarget(null);
    },
    onError: (err) => show((err as unknown as ApiError).message, 'error'),
  });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-bold tracking-tight">Users</h1>
        <p className="text-sm text-muted-foreground">Manage ASSQP user accounts (admin only)</p>
      </div>

      {isLoading && <LoadingSpinner label="Loading users…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {users && users.length === 0 && <EmptyState icon={<UsersIcon className="h-8 w-8" />} title="No users found" />}

      {users && users.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Created</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-b border-border last:border-0">
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="flex h-7 w-7 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                        {initials(u.fullName)}
                      </div>
                      {u.fullName}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{u.email}</td>
                  <td className="px-4 py-3">
                    <Badge variant="outline">{u.role}</Badge>
                  </td>
                  <td className="px-4 py-3">
                    <Badge className={u.enabled ? 'text-success bg-success/10' : 'text-muted-foreground'}>
                      {u.enabled ? 'Active' : 'Disabled'}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDate(u.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(u.id)} aria-label="Delete user">
                      <Trash2 className="h-4 w-4 text-danger" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && remove.mutate(deleteTarget)}
        title="Delete user?"
        description="This action cannot be undone."
        confirmLabel="Delete"
        isLoading={remove.isPending}
      />
    </div>
  );
}
