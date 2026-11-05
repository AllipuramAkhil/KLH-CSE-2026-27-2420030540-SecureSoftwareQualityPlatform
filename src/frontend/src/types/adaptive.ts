import type { RiskLevel } from './enums';

// Item shape inside AdaptiveTestingController's "recommendedTests" array
export interface AdaptiveTestRecommendation {
  testCaseId: number;
  title: string;
  priorityScore: number;
  riskLevel: RiskLevel;
  recommendedOrder: number;
  reason: string;
}

// Shape returned by POST /prioritize and GET /recommendations
export interface AdaptiveTestingResponse {
  projectId: number;
  totalTests: number;
  recommendedTests: AdaptiveTestRecommendation[];
}

// Shape returned by GET /adaptive-testing/history (AnalysisHistory entity, serialized)
export interface AnalysisHistoryEntry {
  id: number;
  cycleType: string;
  summary: string;
  qualityScore: number | null;
  securityScore: number | null;
  riskScore: number | null;
  recordedAt: string;
}
