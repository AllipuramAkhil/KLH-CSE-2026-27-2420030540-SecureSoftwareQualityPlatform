// Mirrors ModelInfoResponse.java / ModelMetricsResponse.java / RetrainResponse.java / PredictionHistoryEntry.java

export interface ModelInfoResponse {
  modelVersion: string;
  algorithm: string;
  trainedAt: string;
  datasetPath: string;
  datasetHash: string;
  datasetRows: number;
  featureNames: string[];
  classLabels: string[];
  notes: string;
}

export interface ModelMetricsResponse {
  accuracy: number;
  precisionWeighted: number;
  recallWeighted: number;
  f1Weighted: number;
  comparison?: Record<string, unknown> | null;
}

export interface RetrainResponse {
  status: 'SUCCESS' | 'FAILED';
  message: string;
  metadata?: ModelInfoResponse | null;
}

export interface PredictionHistoryEntry {
  id: number;
  projectId: number;
  componentName: string;
  predictionMethod: 'RULE_BASED' | 'ML' | 'HYBRID';
  ruleBasedRiskLevel: string;
  mlRiskLevel: string;
  finalRiskLevel: string;
  mlConfidence: number;
  mlModelVersion: string;
  createdAt: string;
}
