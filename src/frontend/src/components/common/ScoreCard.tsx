import type { ReactNode } from 'react';
import { Card, CardContent } from '@/components/ui/Card';
import { Progress } from '@/components/ui/Progress';
import { cn } from '@/components/ui/utils';
import { formatScore } from '@/utils/formatters';

export function ScoreCard({
  label,
  score,
  max = 100,
  icon,
  suffix = `/ ${max}`,
  invert = false,
}: {
  label: string;
  score: number | null | undefined;
  max?: number;
  icon?: ReactNode;
  suffix?: string;
  /** invert=true means a lower score is "better" (e.g. risk score) */
  invert?: boolean;
}) {
  const value = score ?? 0;
  const pct = max > 0 ? (value / max) * 100 : 0;
  const barColor = invert
    ? pct >= 75
      ? 'bg-danger'
      : pct >= 50
        ? 'bg-warning'
        : 'bg-success'
    : pct >= 75
      ? 'bg-success'
      : pct >= 50
        ? 'bg-info'
        : pct >= 25
          ? 'bg-warning'
          : 'bg-danger';

  return (
    <Card>
      <CardContent className="pt-4">
        <div className="mb-2 flex items-center justify-between">
          <span className="text-xs font-medium text-muted-foreground">{label}</span>
          {icon && <span className="text-muted-foreground">{icon}</span>}
        </div>
        <div className="mb-2 flex items-baseline gap-1">
          <span className={cn('text-2xl font-bold tabular-nums')}>{formatScore(score)}</span>
          <span className="text-xs text-muted-foreground">{suffix}</span>
        </div>
        <Progress value={value} max={max} barClassName={barColor} />
      </CardContent>
    </Card>
  );
}
