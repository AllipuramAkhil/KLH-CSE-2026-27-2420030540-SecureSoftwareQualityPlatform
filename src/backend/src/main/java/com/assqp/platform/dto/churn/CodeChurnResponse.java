package com.assqp.platform.dto.churn;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChurnResponse {
    private Long id;
    private Long projectId;
    private String componentName;
    private Integer commitCount;
    private Integer linesAdded;
    private Integer linesDeleted;
    private Integer totalLinesChanged;
    private Double changeFrequency;
    private Double recentModificationScore;
    private LocalDateTime computedAt;
}
