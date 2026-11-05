package com.assqp.platform.dto.cicd;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiCdWebhookRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private String branch;
    private String commitHash;
    private String buildNumber;
    private String triggerSource;
}
