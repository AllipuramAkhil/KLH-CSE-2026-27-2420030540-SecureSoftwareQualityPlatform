package com.assqp.platform.service;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.zap.ZapImportResultResponse;
import com.assqp.platform.dto.zap.ZapScanStatusResponse;
import com.assqp.platform.dto.zap.ZapScanTriggerResponse;
import com.assqp.platform.dto.zap.ZapStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ZapService {
    ZapStatusResponse getStatus();

    /** Validates targetUrl against the allow-list, then starts a spider (baseline) scan. */
    ZapScanTriggerResponse triggerBaselineScan(Long projectId, String targetUrl);

    ZapScanStatusResponse getScanStatus(String scanId);

    /** Pulls currently-accumulated alerts for targetUrl and persists new ones (dedup by plugin+url). */
    ZapImportResultResponse collectResults(Long projectId, String targetUrl);

    ZapImportResultResponse importFromFile(Long projectId, MultipartFile file);

    List<SecurityVulnerabilityResponse> getFindings(Long projectId);
}
