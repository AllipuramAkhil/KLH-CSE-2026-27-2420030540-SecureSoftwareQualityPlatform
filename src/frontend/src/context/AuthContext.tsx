import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '@/api/authApi';
import { tokenStorage } from '@/utils/auth';
import type { UserResponse } from '@/types/user';
import type { LoginRequest, RegisterRequest } from '@/types/auth';

interface AuthContextValue {
  user: UserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadCurrentUser = useCallback(async () => {
    if (!tokenStorage.hasSession()) {
      setUser(null);
      setIsLoading(false);
      return;
    }
    try {
      const me = await authApi.me();
      setUser(me);
    } catch {
      tokenStorage.clearTokens();
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCurrentUser();
  }, [loadCurrentUser]);

  // Fired by the axios interceptor when a refresh attempt fails.
  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
    };
    window.addEventListener('assqp:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('assqp:unauthorized', handleUnauthorized);
  }, []);

  const login = useCallback(async (payload: LoginRequest) => {
    const res = await authApi.login(payload);
    tokenStorage.setTokens(res.accessToken, res.refreshToken);
    const me = await authApi.me();
    setUser(me);
  }, []);

  const register = useCallback(async (payload: RegisterRequest) => {
    const res = await authApi.register(payload);
    tokenStorage.setTokens(res.accessToken, res.refreshToken);
    const me = await authApi.me();
    setUser(me);
  }, []);

  const logout = useCallback(() => {
    tokenStorage.clearTokens();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      register,
      logout,
      refreshUser: loadCurrentUser,
    }),
    [user, isLoading, login, register, logout, loadCurrentUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
