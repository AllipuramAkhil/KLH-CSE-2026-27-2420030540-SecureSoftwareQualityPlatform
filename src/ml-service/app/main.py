"""
ASSQP ML Risk Prediction Service.

Standalone FastAPI microservice consumed by the Spring Boot backend's
MlPredictionClient (Phase 8). Runs independently of the main backend -
if this service is down, ASSQP falls back to its rule-based risk engine,
so nothing here is a hard dependency for the rest of the platform.

Run directly:
    uvicorn app.main:app --host 0.0.0.0 --port 8000

Or via Docker (see ../Dockerfile / docker-compose.yml's `ml-service`).
"""
import logging
import sys
from pathlib import Path

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.config import settings  # noqa: E402
from app.model_service import ModelNotLoadedError, model_service  # noqa: E402
from app.schemas import (  # noqa: E402
    HealthResponse,
    ModelInfoResponse,
    ModelMetricsResponse,
    PredictionHistoryEntry,
    RetrainResponse,
    RiskPredictionRequest,
    RiskPredictionResponse,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("assqp-ml-service")

app = FastAPI(
    title="ASSQP ML Risk Prediction Service",
    description=(
        "RandomForestClassifier-based software risk prediction, consumed by the "
        "ASSQP Spring Boot backend as one input to its hybrid (ML + rule-based) "
        "risk assessment. Standalone and optional - the platform's rule-based "
        "risk engine works fully without this service."
    ),
    version="1.0.0",
)

# Security fix (audit issue #7): this service is only ever called server-to-server by the
# Spring Boot backend (see ML_SERVICE_URL) - the frontend never calls it directly, so it
# doesn't need browser CORS at all. A wildcard "*" with allow_methods/allow_headers=["*"]
# previously let any website's JavaScript call this from a user's browser. CORS is only
# relevant for browser-originated requests, so disabling it here has no effect on the
# backend's normal server-to-server calls.
app.add_middleware(
    CORSMiddleware,
    allow_origins=[],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


@app.on_event("startup")
def _startup_check() -> None:
    if not model_service.is_loaded():
        logger.warning(
            "No trained model found at %s. Train one with "
            "'python training/generate_synthetic_dataset.py && python training/train.py', "
            "or POST /retrain once a dataset exists. Predictions will return 503 until then.",
            settings.model_path,
        )
    else:
        logger.info(
            "Loaded model version=%s trained_at=%s",
            model_service.metadata().get("model_version"),
            model_service.metadata().get("trained_at"),
        )


def verify_internal_secret(x_internal_secret: str | None = Header(default=None)) -> None:
    """Defense-in-depth for /retrain (audit issue #4): if ML_INTERNAL_SECRET is
    configured, callers must present it. No-op (disabled) when left blank, e.g. for
    local `uvicorn` development outside Docker."""
    if settings.internal_secret and x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Missing or invalid X-Internal-Secret header")


@app.get("/health", response_model=HealthResponse, tags=["health"])
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        modelLoaded=model_service.is_loaded(),
        modelVersion=model_service.metadata().get("model_version") if model_service.is_loaded() else None,
    )


@app.post("/predict-risk", response_model=RiskPredictionResponse, tags=["prediction"])
def predict_risk(request: RiskPredictionRequest) -> RiskPredictionResponse:
    try:
        result = model_service.predict(request)
    except ModelNotLoadedError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return RiskPredictionResponse(**result)


@app.get("/model-info", response_model=ModelInfoResponse, tags=["mlops"])
def model_info() -> ModelInfoResponse:
    meta = model_service.metadata()
    if not meta:
        raise HTTPException(status_code=503, detail="No model metadata available - train a model first.")
    return ModelInfoResponse(
        modelVersion=meta.get("model_version", "unknown"),
        algorithm=meta.get("algorithm", "unknown"),
        trainedAt=meta.get("trained_at", ""),
        datasetPath=meta.get("dataset_path", ""),
        datasetHash=meta.get("dataset_hash", ""),
        datasetRows=meta.get("dataset_rows", 0),
        featureNames=meta.get("feature_names", []),
        classLabels=meta.get("class_labels", []),
        notes=meta.get("notes", ""),
    )


@app.get("/metrics", response_model=ModelMetricsResponse, tags=["mlops"])
def metrics() -> ModelMetricsResponse:
    meta = model_service.metadata()
    m = meta.get("metrics")
    if not m:
        raise HTTPException(status_code=503, detail="No evaluation metrics available - train a model first.")
    return ModelMetricsResponse(
        accuracy=m["accuracy"],
        precisionWeighted=m["precision_weighted"],
        recallWeighted=m["recall_weighted"],
        f1Weighted=m["f1_weighted"],
        comparison=meta.get("comparison"),
    )


@app.post("/retrain", response_model=RetrainResponse, tags=["mlops"])
def retrain(compare_logreg: bool = False, _: None = Depends(verify_internal_secret)) -> RetrainResponse:
    try:
        meta = model_service.retrain(compare_logreg=compare_logreg)
    except Exception as exc:  # noqa: BLE001 - surfaced to the caller, never crashes the service
        logger.exception("Retrain failed")
        return RetrainResponse(status="FAILED", message=str(exc), metadata=None)

    return RetrainResponse(
        status="SUCCESS",
        message=f"Retrained on {meta.get('dataset_rows', 0)} rows.",
        metadata=ModelInfoResponse(
            modelVersion=meta.get("model_version", "unknown"),
            algorithm=meta.get("algorithm", "unknown"),
            trainedAt=meta.get("trained_at", ""),
            datasetPath=meta.get("dataset_path", ""),
            datasetHash=meta.get("dataset_hash", ""),
            datasetRows=meta.get("dataset_rows", 0),
            featureNames=meta.get("feature_names", []),
            classLabels=meta.get("class_labels", []),
            notes=meta.get("notes", ""),
        ),
    )


@app.get("/prediction-history", response_model=list[PredictionHistoryEntry], tags=["mlops"])
def prediction_history(limit: int = 50) -> list[PredictionHistoryEntry]:
    return [PredictionHistoryEntry(**entry) for entry in model_service.prediction_history(limit=limit)]
