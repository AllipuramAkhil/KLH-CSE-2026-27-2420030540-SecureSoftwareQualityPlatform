// Mirrors ZapStatusResponse.java / ZapScanTriggerResponse.java /
// ZapScanStatusResponse.java / ZapImportResultResponse.java
import type { SecurityVulnerabilityResponse } from './security';

export interface ZapStatusResponse {
  apiAvailable: boolean;
  allowedTargets: string[];
}

export interface ZapScanTriggerResponse {
  scanId: string;
  targetUrl: string;
  status: string;
}

export interface ZapScanStatusResponse {
  scanId: string;
  progressPercent: number;
  complete: boolean;
}

export interface ZapImportResultResponse {
  totalFindingsParsed: number;
  newFindingsStored: number;
  duplicatesSkipped: number;
  newFindings: SecurityVulnerabilityResponse[];
}
