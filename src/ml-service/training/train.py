"""
Trains the ASSQP risk classifier.

Usage:
    python training/train.py
    python training/train.py --dataset data/dataset.csv --compare-logreg

Pipeline: load dataset -> validate -> feature/label split -> train/test
split -> train RandomForestClassifier -> evaluate (accuracy/precision/
recall/F1) -> optionally also train+evaluate LogisticRegression for
comparison -> save the RandomForest model + metadata + feature list via
joblib to models/.
"""
import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

sys.path.insert(0, str(Path(__file__).resolve().parent))
from feature_engineering import FEATURE_COLUMNS, LABEL_COLUMN, load_dataset, split_features_labels  # noqa: E402
from evaluate import evaluate_predictions  # noqa: E402

MODEL_VERSION = "1.0.0"


def dataset_hash(csv_path: Path) -> str:
    return hashlib.sha256(csv_path.read_bytes()).hexdigest()[:16]


def train(dataset_path: Path, models_dir: Path, compare_logreg: bool, test_size: float, seed: int) -> dict:
    df = load_dataset(dataset_path)
    X, y = split_features_labels(df)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=seed, stratify=y
    )

    rf_model = RandomForestClassifier(
        n_estimators=200,
        max_depth=12,
        min_samples_leaf=3,
        random_state=seed,
        class_weight="balanced",
    )
    rf_model.fit(X_train, y_train)
    rf_predictions = rf_model.predict(X_test)
    rf_metrics = evaluate_predictions(y_test, rf_predictions)

    print("RandomForestClassifier metrics:")
    for k, v in rf_metrics.items():
        print(f"  {k}: {v:.4f}")

    comparison = None
    if compare_logreg:
        logreg_pipeline = Pipeline([
            ("scaler", StandardScaler()),
            ("clf", LogisticRegression(max_iter=1000, class_weight="balanced", random_state=seed)),
        ])
        logreg_pipeline.fit(X_train, y_train)
        logreg_predictions = logreg_pipeline.predict(X_test)
        logreg_metrics = evaluate_predictions(y_test, logreg_predictions)
        comparison = {"logistic_regression": logreg_metrics}
        print("\nLogisticRegression metrics (comparison only, not saved as the serving model):")
        for k, v in logreg_metrics.items():
            print(f"  {k}: {v:.4f}")

    models_dir.mkdir(parents=True, exist_ok=True)
    model_path = models_dir / "risk_model.joblib"
    joblib.dump(rf_model, model_path)

    metadata = {
        "model_version": MODEL_VERSION,
        "algorithm": "RandomForestClassifier",
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "dataset_path": str(dataset_path),
        "dataset_hash": dataset_hash(dataset_path),
        "dataset_rows": int(len(df)),
        "test_size": test_size,
        "random_seed": seed,
        "feature_names": FEATURE_COLUMNS,
        "label_column": LABEL_COLUMN,
        "class_labels": sorted(y.unique().tolist()),
        "metrics": rf_metrics,
        "comparison": comparison,
        "notes": (
            "Trained on SYNTHETIC/SAMPLE data unless dataset_path points to a "
            "real historical export - check dataset_path above."
        ),
    }
    metadata_path = models_dir / "model_metadata.json"
    metadata_path.write_text(json.dumps(metadata, indent=2))

    print(f"\nSaved model to {model_path}")
    print(f"Saved metadata to {metadata_path}")
    return metadata


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=str, default="data/dataset.csv")
    parser.add_argument("--models-dir", type=str, default="models")
    parser.add_argument("--compare-logreg", action="store_true")
    parser.add_argument("--test-size", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    train(
        dataset_path=Path(args.dataset),
        models_dir=Path(args.models_dir),
        compare_logreg=args.compare_logreg,
        test_size=args.test_size,
        seed=args.seed,
    )
