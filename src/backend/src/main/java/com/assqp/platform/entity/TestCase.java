package com.assqp.platform.entity;

import com.assqp.platform.enums.TestCaseStatus;
import com.assqp.platform.enums.TestType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TestType testType;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(length = 2000)
    private String preconditions;

    @Column(length = 4000)
    private String testSteps;

    @Column(length = 2000)
    private String expectedResult;

    @Column(length = 2000)
    private String actualResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TestCaseStatus status = TestCaseStatus.ACTIVE;

    @Column(name = "related_component", length = 255)
    private String relatedComponent;

    @Builder.Default
    @Column(name = "historical_failure_rate")
    private Double historicalFailureRate = 0.0;

    @Builder.Default
    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @Column(name = "last_execution")
    private LocalDateTime lastExecution;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
