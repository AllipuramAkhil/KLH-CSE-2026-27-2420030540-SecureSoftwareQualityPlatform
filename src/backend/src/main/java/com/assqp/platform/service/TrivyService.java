package com.assqp.platform.service;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.trivy.TrivyScanResultResponse;
import com.assqp.platform.dto.trivy.TrivyStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TrivyService {

    /** Whether the trivy CLI is available on this backend's PATH. */
    TrivyStatusResponse getStatus();

    /**
     * Runs `trivy fs` locally (only works if the CLI is available - see
     * getStatus()) and imports the findings, skipping ones already stored
     * for this project.
     */
    TrivyScanResultResponse triggerFilesystemScan(Long projectId, String targetPathOverride);

    /**
     * Runs `trivy image <imageRef>` locally and imports the findings.
     */
    TrivyScanResultResponse triggerImageScan(Long projectId, String imageRef);

    /**
     * Imports a Trivy JSON report uploaded directly - the portable path
     * that always works, regardless of whether the CLI is installed on
     * this backend (e.g. a report downloaded from the GitHub Actions
     * pipeline's `trivy-filesystem-report` / `trivy-image-report` artifact).
     */
    TrivyScanResultResponse importFromFile(Long projectId, MultipartFile file);

    /** All Trivy-sourced findings for a project (source=TRIVY), newest first. */
    List<SecurityVulnerabilityResponse> getFindings(Long projectId);
}
