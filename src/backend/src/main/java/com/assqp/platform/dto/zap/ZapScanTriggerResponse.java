package com.assqp.platform.dto.zap;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZapScanTriggerResponse {
    private String scanId;
    private String targetUrl;
    private String status;
}
