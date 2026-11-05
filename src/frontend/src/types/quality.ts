// Mirrors QualityAnalysisResponse.java
export interface QualityAnalysisResponse {
  id: number;
  projectId: number;
  linesOfCode: number;
  numberOfMethods: number;
  cyclomaticComplexity: number;
  codeDuplicationPercent: number;
  maintainabilityIndex: number;
  codeSmells: number;
  potentialBugs: number;
  testCoveragePercent: number;
  technicalDebtHours: number;
  overallQualityScore: number;
  analyzedAt: string;
}
