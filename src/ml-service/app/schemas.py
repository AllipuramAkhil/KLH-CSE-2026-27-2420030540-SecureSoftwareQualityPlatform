from typing import Optional

from pydantic import BaseModel, Field


class RiskPredictionRequest(BaseModel):
    codeComplexity: float = Field(..., ge=0)
    codeChurn: float = Field(..., ge=0)
    historicalDefects: float = Field(..., ge=0)
    vulnerabilityCount: float = Field(..., ge=0)
    maxVulnerabilitySeverity: float = Field(..., ge=0, le=10)
    testCoverage: float = Field(..., ge=0, le=100)
    codeSmells: float = Field(..., ge=0)
    duplication: float = Field(..., ge=0, le=100)
    historicalFailureRate: float = Field(..., ge=0, le=1)
    technicalDebt: float = Field(..., ge=0)


class RiskPredictionResponse(BaseModel):
    riskLevel: str
    confidence: float
    modelVersion: str
    featureContributions: dict[str, float]


class ModelInfoResponse(BaseModel):
    modelVersion: str
    algorithm: str
    trainedAt: str
    datasetPath: str
    datasetHash: str
    datasetRows: int
    featureNames: list[str]
    classLabels: list[str]
    notes: str


class ModelMetricsResponse(BaseModel):
    accuracy: float
    precisionWeighted: float
    recallWeighted: float
    f1Weighted: float
    comparison: Optional[dict] = None


class RetrainResponse(BaseModel):
    status: str
    message: str
    metadata: Optional[ModelInfoResponse] = None


class PredictionHistoryEntry(BaseModel):
    timestamp: str
    request: RiskPredictionRequest
    riskLevel: str
    confidence: float
    modelVersion: str


class HealthResponse(BaseModel):
    status: str
    modelLoaded: bool
    modelVersion: Optional[str] = None
