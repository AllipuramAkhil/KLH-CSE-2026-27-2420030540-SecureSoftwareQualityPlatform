import { useState } from 'react';
import { Bell, Check, Trash2 } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { Button } from '@/components/ui/Button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import {
  useUnreadNotifications,
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useDeleteNotification,
} from '@/hooks/useNotifications';
import { formatRelativeTime } from '@/utils/formatters';

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const { data: unread, isLoading } = useUnreadNotifications();
  const markAllRead = useMarkAllNotificationsRead();
  const markRead = useMarkNotificationRead();
  const remove = useDeleteNotification();

  const count = unread?.length ?? 0;

  return (
    <div className="relative">
      <Button variant="ghost" size="icon" onClick={() => setOpen((o) => !o)} aria-label="Notifications">
        <Bell className="h-4 w-4" />
        {count > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-danger px-1 text-[10px] font-bold text-white">
            {count > 9 ? '9+' : count}
          </span>
        )}
      </Button>

      <AnimatePresence>
        {open && (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.15 }}
              className="absolute right-0 z-50 mt-2 w-80 rounded-lg border border-border bg-card shadow-xl"
            >
              <div className="flex items-center justify-between border-b border-border px-3 py-2.5">
                <span className="text-sm font-semibold">Notifications</span>
                {count > 0 && (
                  <button
                    onClick={() => markAllRead.mutate()}
                    className="flex items-center gap-1 text-xs text-primary hover:underline"
                  >
                    <Check className="h-3 w-3" /> Mark all read
                  </button>
                )}
              </div>
              <div className="max-h-96 overflow-y-auto scrollbar-thin">
                {isLoading && <LoadingSpinner className="py-8" />}
                {!isLoading && count === 0 && (
                  <div className="p-4">
                    <EmptyState title="You're all caught up" description="No unread notifications." />
                  </div>
                )}
                {unread?.map((n) => (
                  <div key={n.id} className="flex items-start gap-2 border-b border-border px-3 py-2.5 last:border-0 hover:bg-muted/50">
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">{n.title}</p>
                      {n.message && <p className="text-xs text-muted-foreground">{n.message}</p>}
                      <p className="mt-0.5 text-[11px] text-muted-foreground">{formatRelativeTime(n.createdAt)}</p>
                    </div>
                    <div className="flex shrink-0 gap-1">
                      <button
                        onClick={() => markRead.mutate(n.id)}
                        aria-label="Mark as read"
                        className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-success"
                      >
                        <Check className="h-3.5 w-3.5" />
                      </button>
                      <button
                        onClick={() => remove.mutate(n.id)}
                        aria-label="Delete notification"
                        className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-danger"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  );
}
