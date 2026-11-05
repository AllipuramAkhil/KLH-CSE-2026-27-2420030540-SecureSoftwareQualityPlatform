package com.assqp.platform.dto.testing;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private String testType;
    private String priority;
    private String preconditions;
    private String testSteps;
    private String expectedResult;
    private String actualResult;
    private String status;
    private String relatedComponent;
    private Double historicalFailureRate;
    private Double riskScore;
    private LocalDateTime lastExecution;
    private LocalDateTime createdAt;
}
