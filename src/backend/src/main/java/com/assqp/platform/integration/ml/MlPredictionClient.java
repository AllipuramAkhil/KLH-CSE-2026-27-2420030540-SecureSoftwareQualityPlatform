package com.assqp.platform.integration.ml;

import com.assqp.platform.dto.ml.ModelInfoResponse;
import com.assqp.platform.dto.ml.ModelMetricsResponse;
import com.assqp.platform.dto.ml.RetrainResponse;

/**
 * Client for the standalone ASSQP ML risk-prediction service (FastAPI,
 * see /ml-service). Configured via ML_SERVICE_URL. Must never throw for
 * "not configured" - callers check {@link #isAvailable()} first - and
 * must wrap any network/parse/timeout failure in
 * {@link com.assqp.platform.exception.MlServiceException} so
 * RiskAssessmentServiceImpl's rule-based scoring keeps working
 * regardless of the ML service's availability.
 */
public interface MlPredictionClient {

    /** True only when an ML service URL is configured. Does not guarantee reachability. */
    boolean isAvailable();

    MlRiskResponse predictRisk(MlRiskRequest request);

    // --- Phase 10 (MLOps) - direct proxies to the ML service's own endpoints ---

    ModelInfoResponse getModelInfo();

    ModelMetricsResponse getMetrics();

    RetrainResponse retrain(boolean compareLogreg);
}
