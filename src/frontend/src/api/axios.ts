import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '@/utils/auth';
import type { ErrorResponse } from '@/types/common';
import type { AuthResponse } from '@/types/auth';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30000,
});

// Attach the JWT access token to every outgoing request.
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStorage.getAccessToken();
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalized shape the rest of the app works with, derived from the
// backend's GlobalExceptionHandler ErrorResponse structure.
export interface ApiError {
  status: number;
  message: string;
  validationErrors?: Record<string, string> | null;
}

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

// Auto-refresh on 401. Follows the backend's actual contract:
// POST /api/auth/refresh { refreshToken } -> AuthResponse
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    const status = error.response?.status;
    const isAuthEndpoint = originalRequest?.url?.includes('/auth/login') || originalRequest?.url?.includes('/auth/register');

    if (status === 401 && originalRequest && !originalRequest._retry && !isAuthEndpoint) {
      const refreshToken = tokenStorage.getRefreshToken();

      if (!refreshToken) {
        tokenStorage.clearTokens();
        window.dispatchEvent(new CustomEvent('assqp:unauthorized'));
        return Promise.reject(normalizeError(error));
      }

      originalRequest._retry = true;

      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((newToken: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
            }
            resolve(apiClient(originalRequest));
          });
        });
      }

      isRefreshing = true;

      try {
        const { data } = await axios.post<AuthResponse>(`${BASE_URL}/auth/refresh`, { refreshToken });
        tokenStorage.setTokens(data.accessToken, data.refreshToken);
        isRefreshing = false;
        onRefreshed(data.accessToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        }
        return apiClient(originalRequest);
      } catch {
        isRefreshing = false;
        refreshSubscribers = [];
        tokenStorage.clearTokens();
        window.dispatchEvent(new CustomEvent('assqp:unauthorized'));
        return Promise.reject(normalizeError(error));
      }
    }

    return Promise.reject(normalizeError(error));
  }
);

function normalizeError(error: AxiosError<ErrorResponse>): ApiError {
  if (error.response?.data) {
    return {
      status: error.response.status,
      message: error.response.data.message || 'Something went wrong. Please try again.',
      validationErrors: error.response.data.validationErrors,
    };
  }
  if (error.request) {
    return { status: 0, message: 'Unable to reach the server. Check your connection and try again.' };
  }
  return { status: -1, message: error.message || 'An unexpected error occurred.' };
}
