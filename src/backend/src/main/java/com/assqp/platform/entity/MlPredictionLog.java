package com.assqp.platform.entity;

import com.assqp.platform.enums.PredictionMethod;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per real call to the ML service made during risk analysis
 * (Phase 10 - "basic performance monitoring"). Deliberately separate
 * from RiskAssessment: this is an append-only audit log of ML calls
 * specifically (never updated/upserted), whereas RiskAssessment rows
 * are the current risk state per component (upserted-in-spirit via
 * fresh inserts each analysis run). Lets GET /api/ml/prediction-history
 * show real project/component context, which the ML service's own
 * context-free JSONL log can't.
 */
@Entity
@Table(name = "ml_prediction_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlPredictionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "component_name", length = 255)
    private String componentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_method", nullable = false, length = 20)
    private PredictionMethod predictionMethod;

    @Column(name = "rule_based_risk_level", length = 20)
    private String ruleBasedRiskLevel;

    @Column(name = "ml_risk_level", length = 20)
    private String mlRiskLevel;

    @Column(name = "final_risk_level", length = 20)
    private String finalRiskLevel;

    @Column(name = "ml_confidence")
    private Double mlConfidence;

    @Column(name = "ml_model_version", length = 50)
    private String mlModelVersion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
