import type { PipelineStatus } from './enums';

// Mirrors CiCdWebhookRequest.java
export interface CiCdWebhookRequest {
  projectId: number;
  branch?: string;
  commitHash?: string;
  buildNumber?: string;
  triggerSource?: string;
}

// Mirrors CiCdPipelineResponse.java
export interface CiCdPipelineResponse {
  id: number;
  projectId: number;
  branch?: string;
  commitHash?: string;
  buildNumber?: string;
  status: PipelineStatus;
  triggerSource?: string;
  startTime?: string;
  endTime?: string;
}
