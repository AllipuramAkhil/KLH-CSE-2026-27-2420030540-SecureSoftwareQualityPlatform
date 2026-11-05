package com.assqp.platform.dto.ai;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiGenerateTestsRequest {
    private String componentName;
    private String sourceCodeSnippet;
    private Integer numberOfTests;
}
