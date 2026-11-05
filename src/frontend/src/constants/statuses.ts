import type { Severity, RiskLevel, ExecutionStatus, PipelineStatus, VulnerabilityStatus, VulnerabilitySource } from '@/types/enums';

export const SEVERITY_ORDER: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'];
export const RISK_LEVEL_ORDER: RiskLevel[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: 'text-red-500 bg-red-500/10 border-red-500/30',
  HIGH: 'text-orange-500 bg-orange-500/10 border-orange-500/30',
  MEDIUM: 'text-yellow-500 bg-yellow-500/10 border-yellow-500/30',
  LOW: 'text-blue-500 bg-blue-500/10 border-blue-500/30',
  INFO: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
};

export const RISK_LEVEL_COLORS: Record<RiskLevel, string> = {
  CRITICAL: 'text-red-500 bg-red-500/10 border-red-500/30',
  HIGH: 'text-orange-500 bg-orange-500/10 border-orange-500/30',
  MEDIUM: 'text-yellow-500 bg-yellow-500/10 border-yellow-500/30',
  LOW: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
};

export const EXECUTION_STATUS_COLORS: Record<ExecutionStatus, string> = {
  PASSED: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
  FAILED: 'text-red-500 bg-red-500/10 border-red-500/30',
  SKIPPED: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
  BLOCKED: 'text-orange-500 bg-orange-500/10 border-orange-500/30',
  ERROR: 'text-red-600 bg-red-600/10 border-red-600/30',
};

export const PIPELINE_STATUS_COLORS: Record<PipelineStatus, string> = {
  QUEUED: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
  RUNNING: 'text-blue-500 bg-blue-500/10 border-blue-500/30',
  SUCCESS: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
  FAILED: 'text-red-500 bg-red-500/10 border-red-500/30',
  CANCELLED: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
};

export const VULNERABILITY_STATUS_COLORS: Record<VulnerabilityStatus, string> = {
  OPEN: 'text-red-500 bg-red-500/10 border-red-500/30',
  IN_PROGRESS: 'text-blue-500 bg-blue-500/10 border-blue-500/30',
  RESOLVED: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
  FALSE_POSITIVE: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
  ACCEPTED_RISK: 'text-yellow-500 bg-yellow-500/10 border-yellow-500/30',
};

export const VULNERABILITY_SOURCE_LABELS: Record<VulnerabilitySource, string> = {
  RULE_BASED: 'Rule-Based',
  TRIVY: 'Trivy',
  ZAP: 'OWASP ZAP',
  SONARQUBE: 'SonarQube',
};

export const VULNERABILITY_SOURCE_COLORS: Record<VulnerabilitySource, string> = {
  RULE_BASED: 'text-slate-400 bg-slate-400/10 border-slate-400/30',
  TRIVY: 'text-purple-500 bg-purple-500/10 border-purple-500/30',
  ZAP: 'text-cyan-500 bg-cyan-500/10 border-cyan-500/30',
  SONARQUBE: 'text-indigo-500 bg-indigo-500/10 border-indigo-500/30',
};
