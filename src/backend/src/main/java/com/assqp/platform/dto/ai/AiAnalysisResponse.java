package com.assqp.platform.dto.ai;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisResponse {
    private String summary;
    private List<String> insights;
    private List<String> recommendations;
    private boolean aiGenerated;
    private String source;
}
