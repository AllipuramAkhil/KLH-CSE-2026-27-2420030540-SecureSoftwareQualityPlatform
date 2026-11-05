import { AlertTriangle, WifiOff, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import type { ApiError } from '@/api/axios';

export function ErrorState({ error, onRetry }: { error?: unknown; onRetry?: () => void }) {
  const apiError = error as ApiError | undefined;
  const status = apiError?.status;

  let Icon = AlertTriangle;
  let title = 'Something went wrong';
  let description = apiError?.message ?? 'Please try again.';

  if (status === 0) {
    Icon = WifiOff;
    title = 'Network error';
    description = 'Unable to reach the ASSQP backend. Check that the API server is running.';
  } else if (status === 401) {
    Icon = ShieldAlert;
    title = 'Session expired';
    description = 'Please sign in again to continue.';
  } else if (status === 403) {
    Icon = ShieldAlert;
    title = 'Access denied';
    description = "You don't have permission to view this resource.";
  }

  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-danger/20 bg-danger/5 py-14 text-center">
      <Icon className="mb-1 h-8 w-8 text-danger" />
      <p className="text-sm font-medium text-foreground">{title}</p>
      <p className="max-w-xs text-xs text-muted-foreground">{description}</p>
      {onRetry && (
        <Button size="sm" variant="outline" className="mt-3" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  );
}
