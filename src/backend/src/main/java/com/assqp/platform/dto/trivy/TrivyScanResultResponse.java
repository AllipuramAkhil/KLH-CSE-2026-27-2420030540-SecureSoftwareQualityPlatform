package com.assqp.platform.dto.trivy;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrivyScanResultResponse {
    private int totalFindingsParsed;
    private int newFindingsStored;
    private int duplicatesSkipped;
    private List<SecurityVulnerabilityResponse> newFindings;
}
