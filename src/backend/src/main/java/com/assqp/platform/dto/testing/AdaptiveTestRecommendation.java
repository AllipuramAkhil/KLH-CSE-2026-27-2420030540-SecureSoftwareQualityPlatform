package com.assqp.platform.dto.testing;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdaptiveTestRecommendation {
    private Long testCaseId;
    private String title;
    private Double priorityScore;
    private String riskLevel;
    private String reason;
}
