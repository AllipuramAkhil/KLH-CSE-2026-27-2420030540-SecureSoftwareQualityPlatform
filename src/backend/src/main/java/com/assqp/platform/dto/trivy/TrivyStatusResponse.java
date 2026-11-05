package com.assqp.platform.dto.trivy;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrivyStatusResponse {
    private boolean cliAvailable;
    private String binaryPath;
}
