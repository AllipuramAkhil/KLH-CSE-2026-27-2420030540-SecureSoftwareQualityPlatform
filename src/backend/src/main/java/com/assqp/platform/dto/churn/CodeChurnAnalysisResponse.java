package com.assqp.platform.dto.churn;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChurnAnalysisResponse {
    private boolean gitAvailable;
    private int componentsAnalyzed;
    private List<CodeChurnResponse> results;
}
