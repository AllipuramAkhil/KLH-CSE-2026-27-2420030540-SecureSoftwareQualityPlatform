// Mirrors AiGenerateTestsRequest.java
export interface AiGenerateTestsRequest {
  componentName?: string;
  sourceCodeSnippet?: string;
  numberOfTests?: number;
}

// Mirrors AiAnalysisRequest.java
export interface AiAnalysisRequest {
  analysisType?: string;
  context?: string;
}

// Mirrors AiAnalysisResponse.java
export interface AiAnalysisResponse {
  summary: string;
  insights: string[];
  recommendations: string[];
  aiGenerated: boolean;
  source: string;
}
