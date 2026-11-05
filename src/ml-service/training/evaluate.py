"""
Metric computation for the risk classifier, and a standalone script to
re-evaluate an already-saved model against a (possibly different) test
dataset without retraining.

Usage (standalone):
    python training/evaluate.py --dataset data/dataset.csv --model models/risk_model.joblib
"""
import argparse
import sys
from pathlib import Path

import joblib
from sklearn.metrics import accuracy_score, f1_score, precision_score, recall_score
from sklearn.model_selection import train_test_split

sys.path.insert(0, str(Path(__file__).resolve().parent))
from feature_engineering import load_dataset, split_features_labels  # noqa: E402


def evaluate_predictions(y_true, y_pred) -> dict:
    return {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "precision_weighted": float(precision_score(y_true, y_pred, average="weighted", zero_division=0)),
        "recall_weighted": float(recall_score(y_true, y_pred, average="weighted", zero_division=0)),
        "f1_weighted": float(f1_score(y_true, y_pred, average="weighted", zero_division=0)),
    }


def evaluate_saved_model(dataset_path: Path, model_path: Path, test_size: float, seed: int) -> dict:
    df = load_dataset(dataset_path)
    X, y = split_features_labels(df)
    _, X_test, _, y_test = train_test_split(X, y, test_size=test_size, random_state=seed, stratify=y)

    model = joblib.load(model_path)
    predictions = model.predict(X_test)
    return evaluate_predictions(y_test, predictions)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset", type=str, default="data/dataset.csv")
    parser.add_argument("--model", type=str, default="models/risk_model.joblib")
    parser.add_argument("--test-size", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    metrics = evaluate_saved_model(Path(args.dataset), Path(args.model), args.test_size, args.seed)
    print("Metrics:")
    for k, v in metrics.items():
        print(f"  {k}: {v:.4f}")
