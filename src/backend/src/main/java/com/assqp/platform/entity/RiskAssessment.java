package com.assqp.platform.entity;

import com.assqp.platform.enums.PredictionMethod;
import com.assqp.platform.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "component_name", length = 255)
    private String componentName;

    private Double probability;
    private Double impact;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(length = 1000)
    private String contributingFactors;

    // --- Phase 8 additions: hybrid ML + rule-based risk assessment.
    // predictionMethod defaults to RULE_BASED so pre-existing rows (and
    // any future analysis run while the ML service is unavailable) are
    // unambiguous - riskLevel above is ALWAYS populated (rule-based at
    // minimum), these fields are additive metadata about how it was reached.
    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_method", nullable = false, length = 20)
    @Builder.Default
    private PredictionMethod predictionMethod = PredictionMethod.RULE_BASED;

    @Column(name = "ml_risk_level", length = 20)
    private String mlRiskLevel;

    @Column(name = "ml_confidence")
    private Double mlConfidence;

    @Column(name = "ml_model_version", length = 50)
    private String mlModelVersion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    @PrePersist
    protected void onCreate() {
        assessedAt = LocalDateTime.now();
    }
}
