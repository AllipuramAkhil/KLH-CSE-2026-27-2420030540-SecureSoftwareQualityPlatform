import type { RiskLevel } from './enums';

// Mirrors RiskAssessmentResponse.java
export interface RiskAssessmentResponse {
  id: number;
  projectId: number;
  componentName: string;
  probability: number;
  impact: number;
  riskScore: number;
  riskLevel: RiskLevel;
  contributingFactors?: string;
  assessedAt: string;

  // Phase 8 additions - predictionMethod is always populated (RULE_BASED
  // when the ML service wasn't available for this assessment run).
  predictionMethod?: 'RULE_BASED' | 'ML' | 'HYBRID';
  mlRiskLevel?: RiskLevel;
  mlConfidence?: number;
  mlModelVersion?: string;
}

// Mirrors RiskSummaryResponse.java
export interface RiskSummaryResponse {
  projectId: number;
  overallRiskScore: number;
  overallRiskLevel: RiskLevel;
  riskDistribution: Record<RiskLevel, number>;
  topRiskyComponent: string | null;
}
