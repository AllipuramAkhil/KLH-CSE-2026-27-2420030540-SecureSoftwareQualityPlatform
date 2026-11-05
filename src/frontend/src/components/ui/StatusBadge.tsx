import { cn } from './utils';
import {
  EXECUTION_STATUS_COLORS,
  PIPELINE_STATUS_COLORS,
  VULNERABILITY_STATUS_COLORS,
} from '@/constants/statuses';
import type { ExecutionStatus, PipelineStatus, VulnerabilityStatus } from '@/types/enums';

type AnyStatus = ExecutionStatus | PipelineStatus | VulnerabilityStatus | string;

const ALL_COLORS: Record<string, string> = {
  ...EXECUTION_STATUS_COLORS,
  ...PIPELINE_STATUS_COLORS,
  ...VULNERABILITY_STATUS_COLORS,
};

export function StatusBadge({ status, className }: { status: AnyStatus; className?: string }) {
  const colorClass = ALL_COLORS[status] ?? 'text-muted-foreground bg-muted border-border';
  return (
    <span className={cn('inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-semibold', colorClass, className)}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}
