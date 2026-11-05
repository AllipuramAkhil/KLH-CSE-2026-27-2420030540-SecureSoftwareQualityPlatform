package com.assqp.platform.integration.trivy;

import com.assqp.platform.enums.Severity;
import lombok.Builder;
import lombok.Getter;

/**
 * Plain result of parsing one entry from a Trivy JSON report - either a
 * dependency/package vulnerability or a detected secret. Both map into
 * the same shape since they both ultimately become a SecurityVulnerability
 * row with source=TRIVY.
 */
@Getter
@Builder
public class TrivyFinding {
    /** CVE/GHSA ID for vulnerabilities; a synthetic "SECRET-<rule>" id for secret findings. */
    private String vulnerabilityId;
    private String packageName;
    private String installedVersion;
    private String fixedVersion;
    private Severity severity;
    private String title;
    private String description;
    /** The scanned file/target Trivy reported this against, e.g. "pom.xml" or "package-lock.json". */
    private String target;
    private String primaryUrl;
    private boolean secretFinding;
}
