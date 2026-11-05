// Mirrors TrivyStatusResponse.java / TrivyScanResultResponse.java
import type { SecurityVulnerabilityResponse } from './security';

export interface TrivyStatusResponse {
  cliAvailable: boolean;
  binaryPath: string;
}

export interface TrivyScanResultResponse {
  totalFindingsParsed: number;
  newFindingsStored: number;
  duplicatesSkipped: number;
  newFindings: SecurityVulnerabilityResponse[];
}
