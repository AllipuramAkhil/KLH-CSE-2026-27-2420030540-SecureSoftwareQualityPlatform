export const ROUTES = {
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',
  DASHBOARD: '/dashboard',
  PROJECTS: '/projects',
  PROJECT_NEW: '/projects/new',
  PROJECT_DETAILS: (id: string | number = ':id') => `/projects/${id}`,
  PROJECT_EDIT: (id: string | number = ':id') => `/projects/${id}/edit`,
  PROJECT_TAB: (id: string | number = ':id', tab: string) => `/projects/${id}/${tab}`,
  USERS: '/users',
  PROFILE: '/profile',
  NOT_FOUND: '/404',
  UNAUTHORIZED: '/unauthorized',
} as const;

// Sub-routes rendered inside a project's own layout (ProjectLayout)
export const PROJECT_TABS = [
  'overview',
  'source',
  'quality',
  'security',
  'tests',
  'adaptive-testing',
  'risk',
  'mlops',
  'ai',
  'reports',
  'cicd',
] as const;

export type ProjectTab = (typeof PROJECT_TABS)[number];
