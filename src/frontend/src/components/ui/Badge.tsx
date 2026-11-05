import type { HTMLAttributes } from 'react';
import { cn } from './utils';

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'outline';
}

export function Badge({ className, variant = 'default', ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
        variant === 'default' && 'border-transparent bg-muted text-muted-foreground',
        variant === 'outline' && 'border-border bg-transparent',
        className
      )}
      {...props}
    />
  );
}
