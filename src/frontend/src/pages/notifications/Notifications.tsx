import { useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { Pagination } from '@/components/ui/Pagination';
import { useNotifications, useMarkNotificationRead, useMarkAllNotificationsRead, useDeleteNotification } from '@/hooks/useNotifications';
import { formatRelativeTime, titleCase } from '@/utils/formatters';
import { cn } from '@/components/ui/utils';

export default function Notifications() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error, refetch } = useNotifications(page, 15);
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();
  const remove = useDeleteNotification();

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Notifications</h1>
          <p className="text-sm text-muted-foreground">All notifications for your account</p>
        </div>
        <Button variant="outline" onClick={() => markAllRead.mutate()}>
          <Check className="h-4 w-4" /> Mark all read
        </Button>
      </div>

      {isLoading && <LoadingSpinner label="Loading notifications…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {data && data.content.length === 0 && <EmptyState icon={<Bell className="h-8 w-8" />} title="No notifications" />}

      {data && data.content.length > 0 && (
        <Card>
          <div className="divide-y divide-border">
            {data.content.map((n) => (
              <div key={n.id} className={cn('flex items-start gap-3 p-4', !n.isRead && 'bg-primary/5')}>
                <div className="mt-1 h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: n.isRead ? 'transparent' : 'hsl(var(--primary))' }} />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium">{n.title}</p>
                    <span className="text-[10px] uppercase tracking-wide text-muted-foreground">{titleCase(n.type)}</span>
                  </div>
                  {n.message && <p className="text-sm text-muted-foreground">{n.message}</p>}
                  <p className="mt-1 text-xs text-muted-foreground">{formatRelativeTime(n.createdAt)}</p>
                </div>
                <div className="flex shrink-0 gap-1">
                  {!n.isRead && (
                    <Button variant="ghost" size="icon" onClick={() => markRead.mutate(n.id)} aria-label="Mark as read">
                      <Check className="h-4 w-4" />
                    </Button>
                  )}
                  <Button variant="ghost" size="icon" onClick={() => remove.mutate(n.id)} aria-label="Delete notification">
                    <Trash2 className="h-4 w-4 text-danger" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
          <CardContent>
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
