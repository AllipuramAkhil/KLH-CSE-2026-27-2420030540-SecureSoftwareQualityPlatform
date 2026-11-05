import type { ExecutionStatus, TestCaseStatus, TestType } from './enums';

// Mirrors TestCaseRequest.java
export interface TestCaseRequest {
  title: string;
  description?: string;
  testType: TestType;
  priority?: string;
  preconditions?: string;
  testSteps?: string;
  expectedResult?: string;
  relatedComponent?: string;
}

// Mirrors TestCaseResponse.java
export interface TestCaseResponse {
  id: number;
  projectId: number;
  title: string;
  description?: string;
  testType: TestType;
  priority: string;
  preconditions?: string;
  testSteps?: string;
  expectedResult?: string;
  actualResult?: string;
  status: TestCaseStatus;
  relatedComponent?: string;
  historicalFailureRate: number;
  riskScore: number;
  lastExecution?: string | null;
  createdAt: string;
}

// Mirrors TestExecutionRequest.java
export interface TestExecutionRequest {
  status: ExecutionStatus;
  result?: string;
  logs?: string;
  errorMessage?: string;
  environment?: string;
  buildNumber?: string;
  durationMs?: number;
}

// Mirrors TestExecutionResponse.java
export interface TestExecutionResponse {
  id: number;
  testCaseId: number;
  status: ExecutionStatus;
  startTime?: string;
  endTime?: string;
  durationMs?: number;
  result?: string;
  logs?: string;
  errorMessage?: string;
  environment?: string;
  buildNumber?: string;
}
