import { AlertTriangle, AlertCircle, Info, ShieldAlert, CircleDot } from 'lucide-react';
import { cn } from './utils';
import { SEVERITY_COLORS } from '@/constants/statuses';
import type { Severity } from '@/types/enums';

const ICONS: Record<Severity, typeof AlertTriangle> = {
  CRITICAL: ShieldAlert,
  HIGH: AlertTriangle,
  MEDIUM: AlertCircle,
  LOW: CircleDot,
  INFO: Info,
};

export function SeverityBadge({ severity, className }: { severity: Severity; className?: string }) {
  const Icon = ICONS[severity];
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-semibold',
        SEVERITY_COLORS[severity],
        className
      )}
    >
      <Icon className="h-3 w-3" aria-hidden="true" />
      {severity}
    </span>
  );
}
