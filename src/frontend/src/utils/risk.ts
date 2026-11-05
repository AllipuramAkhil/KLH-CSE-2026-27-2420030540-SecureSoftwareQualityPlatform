import type { RiskLevel, Severity } from '@/types/enums';

export function scoreToRiskLevel(score: number): RiskLevel {
  if (score >= 75) return 'CRITICAL';
  if (score >= 50) return 'HIGH';
  if (score >= 25) return 'MEDIUM';
  return 'LOW';
}

// Mirrors the backend's RiskAssessmentServiceImpl classification thresholds (0-100 scale)
export function riskMatrixCell(probability: number, impact: number): RiskLevel {
  const score = probability * impact;
  return scoreToRiskLevel(score);
}

export function severityWeight(severity: Severity): number {
  switch (severity) {
    case 'CRITICAL':
      return 5;
    case 'HIGH':
      return 4;
    case 'MEDIUM':
      return 3;
    case 'LOW':
      return 2;
    case 'INFO':
      return 1;
  }
}

export function scoreColorClass(score: number, max = 100): string {
  const pct = (score / max) * 100;
  if (pct >= 75) return 'bg-success';
  if (pct >= 50) return 'bg-info';
  if (pct >= 25) return 'bg-warning';
  return 'bg-danger';
}
