package com.assqp.platform.dto.testing;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionRequest {
    private String status;
    private String result;
    private String logs;
    private String errorMessage;
    private String environment;
    private String buildNumber;
    private Long durationMs;
}
