package com.assqp.platform.integration.ml;

import com.assqp.platform.dto.ml.ModelInfoResponse;
import com.assqp.platform.dto.ml.ModelMetricsResponse;
import com.assqp.platform.dto.ml.RetrainResponse;
import com.assqp.platform.exception.MlServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class MlPredictionClientImpl implements MlPredictionClient {

    private final WebClient webClient;

    @Value("${ml.service.url:}")
    private String serviceUrl;

    @Value("${ml.service.timeout-seconds:10}")
    private int timeoutSeconds;

    // Retraining runs actual model fitting on the ML service side - give
    // it noticeably longer than a single prediction call before timing out.
    @Value("${ml.service.retrain-timeout-seconds:60}")
    private int retrainTimeoutSeconds;

    // Security fix (audit issue #4, defense-in-depth): matches ML_INTERNAL_SECRET on the
    // ML service side. Blank by default (check disabled) for local development.
    @Value("${ml.service.internal-secret:}")
    private String internalSecret;

    public MlPredictionClientImpl(WebClient mlWebClient) {
        this.webClient = mlWebClient;
    }

    @Override
    public boolean isAvailable() {
        return serviceUrl != null && !serviceUrl.isBlank();
    }

    @Override
    public MlRiskResponse predictRisk(MlRiskRequest request) {
        requireAvailable();
        try {
            return webClient.post()
                    .uri("/predict-risk")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MlRiskResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    // One retry on transient network errors only - a 503/4xx from the
                    // service itself (e.g. "no model trained yet") is not retried since
                    // it won't resolve itself within this request's lifetime.
                    .retryWhen(Retry.max(1).filter(this::isTransient))
                    .block();
        } catch (MlServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ML risk prediction call failed: {}", ex.getMessage());
            throw new MlServiceException("ML service call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ModelInfoResponse getModelInfo() {
        requireAvailable();
        try {
            return webClient.get()
                    .uri("/model-info")
                    .retrieve()
                    .bodyToMono(ModelInfoResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception ex) {
            log.warn("ML model-info call failed: {}", ex.getMessage());
            throw new MlServiceException("Failed to fetch ML model info: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ModelMetricsResponse getMetrics() {
        requireAvailable();
        try {
            return webClient.get()
                    .uri("/metrics")
                    .retrieve()
                    .bodyToMono(ModelMetricsResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (Exception ex) {
            log.warn("ML metrics call failed: {}", ex.getMessage());
            throw new MlServiceException("Failed to fetch ML metrics: " + ex.getMessage(), ex);
        }
    }

    @Override
    public RetrainResponse retrain(boolean compareLogreg) {
        requireAvailable();
        try {
            RetrainResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/retrain")
                            .queryParam("compare_logreg", compareLogreg)
                            .build())
                    .headers(headers -> {
                        if (internalSecret != null && !internalSecret.isBlank()) {
                            headers.set("X-Internal-Secret", internalSecret);
                        }
                    })
                    .retrieve()
                    .bodyToMono(RetrainResponse.class)
                    .timeout(Duration.ofSeconds(retrainTimeoutSeconds))
                    .block();
            if (response != null && "FAILED".equalsIgnoreCase(response.getStatus())) {
                log.warn("ML service reported retrain failure: {}", response.getMessage());
            }
            return response;
        } catch (Exception ex) {
            log.warn("ML retrain call failed: {}", ex.getMessage());
            throw new MlServiceException("Failed to retrain ML model: " + ex.getMessage(), ex);
        }
    }

    private boolean isTransient(Throwable ex) {
        return ex instanceof java.io.IOException || ex instanceof java.util.concurrent.TimeoutException;
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new MlServiceException(
                    "ML risk prediction service is not configured (ML_SERVICE_URL missing). " +
                            "The rule-based risk engine remains the source of truth.");
        }
    }
}
