import type { Role } from '@/types/enums';

export const ROLES: Record<Role, Role> = {
  ADMIN: 'ADMIN',
  DEVELOPER: 'DEVELOPER',
  TESTER: 'TESTER',
  SECURITY_ANALYST: 'SECURITY_ANALYST',
};

// UX-only role-based navigation. The backend remains the actual
// authorization authority for every request.
export const NAV_ACCESS: Record<string, Role[]> = {
  dashboard: ['ADMIN', 'DEVELOPER', 'TESTER', 'SECURITY_ANALYST'],
  projects: ['ADMIN', 'DEVELOPER', 'TESTER', 'SECURITY_ANALYST'],
  quality: ['ADMIN', 'DEVELOPER'],
  security: ['ADMIN', 'SECURITY_ANALYST'],
  testing: ['ADMIN', 'DEVELOPER', 'TESTER'],
  adaptive: ['ADMIN', 'DEVELOPER', 'TESTER'],
  risk: ['ADMIN', 'SECURITY_ANALYST'],
  ai: ['ADMIN', 'DEVELOPER'],
  reports: ['ADMIN', 'DEVELOPER', 'TESTER', 'SECURITY_ANALYST'],
  cicd: ['ADMIN', 'DEVELOPER'],
  users: ['ADMIN'],
};
