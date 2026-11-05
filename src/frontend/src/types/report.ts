// Mirrors the Map<String,Object> returned by ReportController
export interface ReportResponse {
  id: number;
  projectId: number;
  title: string;
  content: {
    qualityScore?: number;
    securityScore?: number;
    riskScore?: number;
    totalTests?: number;
    passedTests?: number;
    failedTests?: number;
    testCoverage?: number;
    vulnerabilities?: number;
    riskAssessments?: number;
    recentAnalyses?: string[];
    mostRiskyComponents?: string[];
  };
  generatedAt: string;
}
