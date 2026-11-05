// Centralized token storage. Only JWT access/refresh tokens are stored;
// no passwords are ever persisted client-side.

const ACCESS_TOKEN_KEY = 'assqp_access_token';
const REFRESH_TOKEN_KEY = 'assqp_refresh_token';

export const tokenStorage = {
  getAccessToken: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clearTokens: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
  hasSession: (): boolean => !!localStorage.getItem(ACCESS_TOKEN_KEY),
};
