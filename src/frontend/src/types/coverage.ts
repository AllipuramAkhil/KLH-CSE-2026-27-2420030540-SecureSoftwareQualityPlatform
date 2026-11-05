// Mirrors CoverageReportResponse.java / CoverageTrendPoint.java
export interface CoverageReportResponse {
  id: number;
  projectId: number;
  lineCoveragePercent: number;
  branchCoveragePercent: number;
  instructionCoveragePercent: number;
  overallCoveragePercent: number;
  linesCovered: number;
  linesMissed: number;
  branchesCovered: number;
  branchesMissed: number;
  classesCovered: number;
  classesMissed: number;
  reportSource: string;
  importedAt: string;
}

export interface CoverageTrendPoint {
  date: string;
  lineCoveragePercent: number;
  branchCoveragePercent: number;
  overallCoveragePercent: number;
}
