import type { Severity, VulnerabilityStatus, VulnerabilitySource } from './enums';

// Mirrors SecurityVulnerabilityResponse.java
export interface SecurityVulnerabilityResponse {
  id: number;
  projectId: number;
  title: string;
  category: string;
  cwe?: string;
  owaspCategory?: string;
  affectedFile?: string;
  lineNumber?: number;
  description?: string;
  impact?: string;
  recommendation?: string;
  severity: Severity;
  status: VulnerabilityStatus;
  detectedAt: string;

  // Phase 5 additions - undefined/RULE_BASED for pre-existing findings.
  source?: VulnerabilitySource;
  externalId?: string;
  packageName?: string;
  installedVersion?: string;
  fixedVersion?: string;

  // Phase 6 addition - ZAP's confidence label. Undefined for non-ZAP findings.
  confidence?: string;
}

// Mirrors VulnerabilityStatusUpdateRequest.java
export interface VulnerabilityStatusUpdateRequest {
  status: VulnerabilityStatus;
}
