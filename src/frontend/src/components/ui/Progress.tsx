import { cn } from './utils';
import { scoreColorClass } from '@/utils/risk';

export function Progress({
  value,
  max = 100,
  className,
  barClassName,
}: {
  value: number;
  max?: number;
  className?: string;
  barClassName?: string;
}) {
  const pct = Math.max(0, Math.min(100, (value / max) * 100));
  return (
    <div className={cn('h-1.5 w-full overflow-hidden rounded-full bg-muted', className)}>
      <div
        className={cn('h-full rounded-full transition-all duration-500', barClassName ?? scoreColorClass(value, max))}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
