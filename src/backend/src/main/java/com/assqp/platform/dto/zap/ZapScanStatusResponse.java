package com.assqp.platform.dto.zap;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZapScanStatusResponse {
    private String scanId;
    private int progressPercent;
    private boolean complete;
}
