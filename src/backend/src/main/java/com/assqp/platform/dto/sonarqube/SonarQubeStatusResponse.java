package com.assqp.platform.dto.sonarqube;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SonarQubeStatusResponse {
    private boolean configured;
    private String hostUrl;
    private String defaultProjectKey;
}
