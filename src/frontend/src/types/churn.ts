// Mirrors GitChurnStatusResponse.java / CodeChurnResponse.java / CodeChurnAnalysisResponse.java
export interface GitChurnStatusResponse {
  gitAvailable: boolean;
  repoPath: string;
  lookbackDays: number;
}

export interface CodeChurnResponse {
  id: number;
  projectId: number;
  componentName: string;
  commitCount: number;
  linesAdded: number;
  linesDeleted: number;
  totalLinesChanged: number;
  changeFrequency: number;
  recentModificationScore: number;
  computedAt: string;
}

export interface CodeChurnAnalysisResponse {
  gitAvailable: boolean;
  componentsAnalyzed: number;
  results: CodeChurnResponse[];
}
