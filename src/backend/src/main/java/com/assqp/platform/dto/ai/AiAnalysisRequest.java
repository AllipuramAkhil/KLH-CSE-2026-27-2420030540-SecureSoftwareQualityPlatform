package com.assqp.platform.dto.ai;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisRequest {
    private String analysisType;
    private String context;
}
