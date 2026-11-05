"""
Feature engineering and dataset validation shared by train.py and
evaluate.py. Kept deliberately simple (no scaling/encoding needed for
tree-based RandomForest) - LogisticRegression, which does need scaled
inputs, applies its own StandardScaler inside its sklearn Pipeline in
train.py rather than duplicating that logic here.
"""
from pathlib import Path

import pandas as pd

FEATURE_COLUMNS = [
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

LABEL_COLUMN = "risk_level"
VALID_RISK_LEVELS = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}


class DatasetValidationError(ValueError):
    pass


def load_dataset(csv_path: Path) -> pd.DataFrame:
    if not csv_path.exists():
        raise DatasetValidationError(
            f"Dataset not found at {csv_path}. Run "
            f"'python training/generate_synthetic_dataset.py' to create a "
            f"synthetic one for development, or supply a real dataset."
        )
    df = pd.read_csv(csv_path)
    validate_dataset(df)
    return df


def validate_dataset(df: pd.DataFrame) -> None:
    missing_cols = set(FEATURE_COLUMNS + [LABEL_COLUMN]) - set(df.columns)
    if missing_cols:
        raise DatasetValidationError(f"Dataset is missing required columns: {sorted(missing_cols)}")

    if df.empty:
        raise DatasetValidationError("Dataset has zero rows.")

    if df[FEATURE_COLUMNS].isnull().any().any():
        bad_cols = df[FEATURE_COLUMNS].columns[df[FEATURE_COLUMNS].isnull().any()].tolist()
        raise DatasetValidationError(f"Dataset has null values in feature column(s): {bad_cols}")

    invalid_labels = set(df[LABEL_COLUMN].unique()) - VALID_RISK_LEVELS
    if invalid_labels:
        raise DatasetValidationError(
            f"Dataset contains risk_level value(s) outside {sorted(VALID_RISK_LEVELS)}: {sorted(invalid_labels)}"
        )

    for col in FEATURE_COLUMNS:
        if not pd.api.types.is_numeric_dtype(df[col]):
            raise DatasetValidationError(f"Feature column '{col}' must be numeric.")


def split_features_labels(df: pd.DataFrame):
    X = df[FEATURE_COLUMNS].copy()
    y = df[LABEL_COLUMN].copy()
    return X, y
