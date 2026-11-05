# ASSQP ML Risk Prediction Service
FROM python:3.11-slim

WORKDIR /app

# System deps kept minimal - scikit-learn/pandas wheels are pre-built for
# this base image, no compiler toolchain needed.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY training ./training

# data/ and models/ are also mounted as volumes in docker-compose (so a
# container rebuild doesn't lose a trained model or dataset) - copying
# them here too means `docker run` on its own still works standalone.
COPY data ./data
COPY models ./models

# If no trained model exists yet, generate the documented synthetic
# dataset and train one at image-build time, so the container is
# immediately useful out of the box. If a real dataset.csv was already
# copied in above, it's used instead and nothing here is overwritten.
RUN if [ ! -f models/risk_model.joblib ]; then \
      if [ ! -f data/dataset.csv ]; then \
        python training/generate_synthetic_dataset.py --rows 2000 --out data/dataset.csv; \
      fi; \
      python training/train.py --dataset data/dataset.csv --models-dir models --compare-logreg; \
    fi

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
