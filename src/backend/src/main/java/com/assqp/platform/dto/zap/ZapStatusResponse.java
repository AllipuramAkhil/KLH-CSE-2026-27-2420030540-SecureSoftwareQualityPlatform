package com.assqp.platform.dto.zap;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZapStatusResponse {
    private boolean apiAvailable;
    private List<String> allowedTargets;
}
