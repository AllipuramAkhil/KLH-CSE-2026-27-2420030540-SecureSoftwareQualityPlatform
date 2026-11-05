// Exact mirror of backend com.assqp.platform.enums.*

export type Role = 'ADMIN' | 'DEVELOPER' | 'TESTER' | 'SECURITY_ANALYST';

export type ProjectStatus = 'ACTIVE' | 'ARCHIVED' | 'ON_HOLD' | 'COMPLETED';

export type Severity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export type VulnerabilityStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'FALSE_POSITIVE'
  | 'ACCEPTED_RISK';

export type TestType =
  | 'FUNCTIONAL'
  | 'UNIT'
  | 'INTEGRATION'
  | 'API'
  | 'REGRESSION'
  | 'SECURITY'
  | 'PERFORMANCE';

export type TestCaseStatus = 'ACTIVE' | 'DRAFT' | 'DEPRECATED' | 'UNDER_REVIEW';

export type ExecutionStatus = 'PASSED' | 'FAILED' | 'SKIPPED' | 'BLOCKED' | 'ERROR';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type NotificationType =
  | 'QUALITY_ALERT'
  | 'SECURITY_ALERT'
  | 'RISK_ALERT'
  | 'TEST_FAILURE'
  | 'CICD_EVENT'
  | 'SYSTEM';

export type PipelineStatus = 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export type VulnerabilitySource = 'RULE_BASED' | 'TRIVY' | 'ZAP' | 'SONARQUBE';
