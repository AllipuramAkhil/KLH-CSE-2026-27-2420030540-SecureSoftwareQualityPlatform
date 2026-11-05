import { cn } from './utils';
import { RISK_LEVEL_COLORS } from '@/constants/statuses';
import type { RiskLevel } from '@/types/enums';

export function RiskBadge({ level, className }: { level: RiskLevel; className?: string }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold',
        RISK_LEVEL_COLORS[level],
        className
      )}
    >
      {level}
    </span>
  );
}
