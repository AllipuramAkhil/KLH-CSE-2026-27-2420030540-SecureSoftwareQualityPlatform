import { cn } from './utils';
import { VULNERABILITY_SOURCE_COLORS, VULNERABILITY_SOURCE_LABELS } from '@/constants/statuses';
import type { VulnerabilitySource } from '@/types/enums';

export function SourceBadge({ source, className }: { source?: VulnerabilitySource; className?: string }) {
  const resolved = source ?? 'RULE_BASED';
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold',
        VULNERABILITY_SOURCE_COLORS[resolved],
        className
      )}
    >
      {VULNERABILITY_SOURCE_LABELS[resolved]}
    </span>
  );
}
