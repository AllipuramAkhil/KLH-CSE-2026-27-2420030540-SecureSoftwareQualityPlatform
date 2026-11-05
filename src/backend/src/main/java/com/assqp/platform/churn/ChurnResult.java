package com.assqp.platform.churn;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurnResult {
    private String path;
    private int commitCount;
    private int linesAdded;
    private int linesDeleted;
    private int totalLinesChanged;
    private double changeFrequency;
    private double recentModificationScore;
}
