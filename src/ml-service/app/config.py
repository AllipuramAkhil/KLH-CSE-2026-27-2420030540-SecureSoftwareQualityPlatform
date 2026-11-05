from pathlib import Path

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    All configurable via environment variables (see docker-compose.yml /
    .env.example: ML_MODEL_PATH, ML_METADATA_PATH, ML_DATASET_PATH).
    """
    model_path: str = "models/risk_model.joblib"
    metadata_path: str = "models/model_metadata.json"
    dataset_path: str = "data/dataset.csv"
    prediction_log_path: str = "models/prediction_log.jsonl"

    # Security fix (audit issue #4, defense-in-depth): even with the Docker Compose port
    # mapping removed so this service isn't reachable from outside the Docker network,
    # anything else on that internal network could still call /retrain. When set
    # (env var ML_INTERNAL_SECRET), callers must send it as X-Internal-Secret. Left blank
    # by default for local `uvicorn` development outside Docker; docker-compose.yml sets it.
    internal_secret: str = ""

    class Config:
        env_prefix = "ML_"
        protected_namespaces = ("settings_",)

    @property
    def model_path_obj(self) -> Path:
        return Path(self.model_path)

    @property
    def metadata_path_obj(self) -> Path:
        return Path(self.metadata_path)

    @property
    def dataset_path_obj(self) -> Path:
        return Path(self.dataset_path)

    @property
    def prediction_log_path_obj(self) -> Path:
        return Path(self.prediction_log_path)


settings = Settings()
