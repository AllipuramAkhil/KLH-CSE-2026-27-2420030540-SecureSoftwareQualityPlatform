package com.assqp.platform.dto.testing;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestExecutionResponse {
    private Long id;
    private Long testCaseId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String result;
    private String logs;
    private String errorMessage;
    private String environment;
    private String buildNumber;
}
