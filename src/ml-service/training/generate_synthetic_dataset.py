"""
SYNTHETIC / SAMPLE DATA FOR DEVELOPMENT
========================================
Generates a synthetic labeled dataset for training the ASSQP risk
prediction model, since no real historical defect/incident dataset
exists yet for this project.

This is NOT real project data. It exists so the ML pipeline (Phase 7)
is runnable and testable end-to-end before real historical data is
available. Replace data/dataset.csv with a real export once ASSQP has
enough historical AnalysisHistory / SecurityVulnerability / CiCdPipeline
rows to build one (see ml-service/README.md "Replacing synthetic data").

The labeling rule below is a simple, documented heuristic (not a real
statistical model) that combines the ten input features into a risk
score, so the resulting relationships are plausible enough for the
classifier to learn something meaningful, without pretending to be
real-world ground truth.
"""
import argparse
import csv
import random
from pathlib import Path

FEATURE_NAMES = [
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

RISK_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]


def sample_features(rng: random.Random) -> dict:
    return {
        "code_complexity": round(rng.uniform(1, 50), 1),
        "code_churn": round(rng.uniform(0, 40), 1),
        "historical_defects": rng.randint(0, 20),
        "vulnerability_count": rng.randint(0, 15),
        "max_vulnerability_severity": round(rng.uniform(0, 10), 1),
        "test_coverage": round(rng.uniform(0, 100), 1),
        "code_smells": rng.randint(0, 60),
        "duplication": round(rng.uniform(0, 40), 1),
        "historical_failure_rate": round(rng.uniform(0, 1), 3),
        "technical_debt": round(rng.uniform(0, 30), 1),
    }


def raw_score(row: dict, rng: random.Random) -> float:
    """
    Documented heuristic used ONLY to rank rows for synthetic labeling.
    Weights are illustrative, not derived from real data: higher
    complexity/churn/defects/vulnerabilities/smells/duplication/debt push
    risk up; higher test coverage pulls it down. A little random noise is
    added so classes aren't perfectly separable by the raw features alone.
    """
    score = (
        row["code_complexity"] * 0.9
        + row["code_churn"] * 0.8
        + row["historical_defects"] * 2.2
        + row["vulnerability_count"] * 2.5
        + row["max_vulnerability_severity"] * 2.0
        + row["code_smells"] * 0.5
        + row["duplication"] * 0.6
        + row["historical_failure_rate"] * 25
        + row["technical_debt"] * 1.1
        - row["test_coverage"] * 0.6
    )
    score += rng.uniform(-15, 15)
    return score


def generate(n_rows: int, seed: int, out_path: Path) -> None:
    rng = random.Random(seed)
    rows = [sample_features(rng) for _ in range(n_rows)]
    scores = [raw_score(r, rng) for r in rows]

    # Quantile-based thresholds -> a roughly balanced 4-class dataset
    # regardless of the exact heuristic weights above, rather than fixed
    # score cutoffs (which skewed heavily toward HIGH/CRITICAL in practice).
    sorted_scores = sorted(scores)

    def quantile(p: float) -> float:
        idx = min(len(sorted_scores) - 1, max(0, int(p * (len(sorted_scores) - 1))))
        return sorted_scores[idx]

    q25, q50, q75 = quantile(0.25), quantile(0.50), quantile(0.75)

    for row, score in zip(rows, scores):
        if score < q25:
            row["risk_level"] = "LOW"
        elif score < q50:
            row["risk_level"] = "MEDIUM"
        elif score < q75:
            row["risk_level"] = "HIGH"
        else:
            row["risk_level"] = "CRITICAL"

    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=FEATURE_NAMES + ["risk_level"])
        writer.writeheader()
        writer.writerows(rows)

    counts = {level: sum(1 for r in rows if r["risk_level"] == level) for level in RISK_LEVELS}
    print(f"Wrote {n_rows} SYNTHETIC rows to {out_path}")
    print(f"Class distribution: {counts}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rows", type=int, default=2000)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--out", type=str, default="data/dataset.csv")
    args = parser.parse_args()
    generate(args.rows, args.seed, Path(args.out))
