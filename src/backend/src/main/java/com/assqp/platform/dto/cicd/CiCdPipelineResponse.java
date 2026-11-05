package com.assqp.platform.dto.cicd;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiCdPipelineResponse {
    private Long id;
    private Long projectId;
    private String branch;
    private String commitHash;
    private String buildNumber;
    private String status;
    private String triggerSource;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
