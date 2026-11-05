import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from threading import Lock
from typing import Optional

import joblib
import pandas as pd

from app.config import settings
from app.schemas import RiskPredictionRequest

FEATURE_ORDER = [
    "code_complexity",
    "code_churn",
    "historical_defects",
    "vulnerability_count",
    "max_vulnerability_severity",
    "test_coverage",
    "code_smells",
    "duplication",
    "historical_failure_rate",
    "technical_debt",
]

_REQUEST_TO_FEATURE = {
    "codeComplexity": "code_complexity",
    "codeChurn": "code_churn",
    "historicalDefects": "historical_defects",
    "vulnerabilityCount": "vulnerability_count",
    "maxVulnerabilitySeverity": "max_vulnerability_severity",
    "testCoverage": "test_coverage",
    "codeSmells": "code_smells",
    "duplication": "duplication",
    "historicalFailureRate": "historical_failure_rate",
    "technicalDebt": "technical_debt",
}


class ModelNotLoadedError(RuntimeError):
    pass


class ModelService:
    """
    Loads the trained RandomForest model + metadata once and serves
    predictions from memory. Thread-safe reload (used by /api/ml/retrain).
    Never raises on construction if the model is missing - callers check
    is_loaded() first, so the FastAPI app can still start and report a
    clear 503 rather than crashing outright.
    """

    def __init__(self) -> None:
        self._lock = Lock()
        self._model = None
        self._metadata: dict = {}
        self._load()

    def _load(self) -> None:
        with self._lock:
            if settings.model_path_obj.exists():
                self._model = joblib.load(settings.model_path_obj)
            else:
                self._model = None

            if settings.metadata_path_obj.exists():
                self._metadata = json.loads(settings.metadata_path_obj.read_text())
            else:
                self._metadata = {}

    def is_loaded(self) -> bool:
        return self._model is not None

    def metadata(self) -> dict:
        return self._metadata

    def predict(self, req: RiskPredictionRequest) -> dict:
        if not self.is_loaded():
            raise ModelNotLoadedError(
                "No trained model is loaded. Run training/train.py, or POST /api/ml/retrain."
            )

        feature_dict = {_REQUEST_TO_FEATURE[k]: v for k, v in req.model_dump().items()}
        row = pd.DataFrame([[feature_dict[f] for f in FEATURE_ORDER]], columns=FEATURE_ORDER)

        prediction = self._model.predict(row)[0]
        probabilities = self._model.predict_proba(row)[0]
        classes = list(self._model.classes_)
        confidence = float(max(probabilities))

        # Global feature importances from the trained RF model - a simple,
        # honest signal (NOT per-instance SHAP; no such dependency is
        # installed, and adding one would be over-engineering for this
        # project's scope). Documented as such in the ml-service README.
        importances = getattr(self._model, "feature_importances_", None)
        contributions = (
            {f: round(float(imp), 4) for f, imp in zip(FEATURE_ORDER, importances)}
            if importances is not None
            else {}
        )

        result = {
            "riskLevel": str(prediction),
            "confidence": round(confidence, 4),
            "modelVersion": self._metadata.get("model_version", "unknown"),
            "featureContributions": contributions,
        }
        self._log_prediction(req, result)
        return result

    def retrain(self, compare_logreg: bool = False) -> dict:
        """
        Runs training/train.py as a subprocess against the currently
        configured dataset, then reloads the model into memory. Kept
        deliberately simple (no async job queue / MLflow) per the "don't
        overengineer" MLOps requirement - this is a student-project-scale
        service, not a production ML platform.
        """
        cmd = [
            sys.executable,
            str(Path(__file__).resolve().parent.parent / "training" / "train.py"),
            "--dataset", settings.dataset_path,
            "--models-dir", str(settings.model_path_obj.parent),
        ]
        if compare_logreg:
            cmd.append("--compare-logreg")

        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
        if proc.returncode != 0:
            raise RuntimeError(f"Retraining failed: {proc.stderr[-2000:]}")

        self._load()
        return self._metadata

    def _log_prediction(self, req: RiskPredictionRequest, result: dict) -> None:
        try:
            settings.prediction_log_path_obj.parent.mkdir(parents=True, exist_ok=True)
            entry = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "request": req.model_dump(),
                "riskLevel": result["riskLevel"],
                "confidence": result["confidence"],
                "modelVersion": result["modelVersion"],
            }
            with settings.prediction_log_path_obj.open("a") as f:
                f.write(json.dumps(entry) + "\n")
        except OSError:
            # Prediction logging is best-effort - never fail a prediction
            # response because the log file couldn't be written.
            pass

    def prediction_history(self, limit: int = 50) -> list[dict]:
        if not settings.prediction_log_path_obj.exists():
            return []
        lines = settings.prediction_log_path_obj.read_text().strip().splitlines()
        entries = [json.loads(line) for line in lines[-limit:]]
        entries.reverse()
        return entries


model_service = ModelService()
