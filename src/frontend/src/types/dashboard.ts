// Mirrors DashboardResponse.java
export interface DashboardResponse {
  projectId: number;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  testCoverage: number;
  totalVulnerabilities: number;
  criticalVulnerabilities: number;
  highVulnerabilities: number;
  openDefects: number;
  qualityScore: number;
  securityScore: number;
  riskScore: number;
  recentAnalyses: string[];
  riskDistribution: Record<string, number>;
  testExecutionTrends: Array<{ date: string; executions: number }>;
  mostRiskyComponents: string[];

  // Real JaCoCo coverage (Phase 3) - optional/undefined until a project
  // has imported at least one report; existing `testCoverage` above is
  // unchanged and still always present.
  realCoverageAvailable?: boolean;
  jacocoLineCoveragePercent?: number | null;
  jacocoBranchCoveragePercent?: number | null;
  jacocoOverallCoveragePercent?: number | null;
  coverageImportedAt?: string | null;

  // Real SonarQube analysis (Phase 4) - optional/undefined until SonarQube
  // is configured and imported at least once. The rule-based quality score
  // above is unaffected either way.
  sonarQubeAvailable?: boolean;
  sonarBugs?: number | null;
  sonarVulnerabilities?: number | null;
  sonarCodeSmells?: number | null;
  sonarTechnicalDebtMinutes?: number | null;
  sonarMaintainabilityRating?: number | null;
  sonarReliabilityRating?: number | null;
  sonarSecurityRating?: number | null;
  sonarCoveragePercent?: number | null;
  sonarDuplicatedLinesDensity?: number | null;
  sonarAnalyzedAt?: string | null;

  // Git code churn (Phase 9) - optional/undefined until the configured
  // local repo is readable and has been analyzed at least once for this project.
  codeChurnAvailable?: boolean;
  mostChurnedComponents?: string[];
}
