// Minimal client-side UI state (not server state — that lives in TanStack
// Query). Tracks the last project the user opened so the global sidebar
// links (Quality, Security, Testing, etc.) can deep-link into that
// project's tabs, since every one of those backend endpoints is
// project-scoped (e.g. /api/projects/{projectId}/quality).

const LAST_PROJECT_KEY = 'assqp_last_project_id';

export const appStore = {
  getLastProjectId: (): number | null => {
    const raw = localStorage.getItem(LAST_PROJECT_KEY);
    return raw ? Number(raw) : null;
  },
  setLastProjectId: (id: number) => {
    localStorage.setItem(LAST_PROJECT_KEY, String(id));
  },
};
