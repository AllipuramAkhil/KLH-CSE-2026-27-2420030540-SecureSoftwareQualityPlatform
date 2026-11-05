package com.assqp.platform.dto.zap;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZapImportResultResponse {
    private int totalFindingsParsed;
    private int newFindingsStored;
    private int duplicatesSkipped;
    private List<SecurityVulnerabilityResponse> newFindings;
}
