import type { ProjectStatus } from './enums';

// Mirrors ProjectRequest.java
export interface ProjectRequest {
  name: string;
  description?: string;
  repositoryUrl?: string;
  branch?: string;
  technologyStack?: string;
}

// Mirrors ProjectResponse.java
export interface ProjectResponse {
  id: number;
  name: string;
  description?: string;
  repositoryUrl?: string;
  branch?: string;
  technologyStack?: string;
  status: ProjectStatus;
  ownerName: string;
  qualityScore: number;
  securityScore: number;
  riskScore: number;
  createdAt: string;
  updatedAt: string;
}
