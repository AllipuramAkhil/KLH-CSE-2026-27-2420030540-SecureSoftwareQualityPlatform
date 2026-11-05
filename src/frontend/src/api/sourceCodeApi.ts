import { apiClient } from './axios';
import type { SourceCodeFileResponse } from '@/types/sourceCode';

export const sourceCodeApi = {
  upload: (projectId: number, file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient
      .post<SourceCodeFileResponse>(`/projects/${projectId}/source/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (evt) => {
          if (onProgress && evt.total) {
            onProgress(Math.round((evt.loaded / evt.total) * 100));
          }
        },
      })
      .then((r) => r.data);
  },

  listByProject: (projectId: number) =>
    apiClient.get<SourceCodeFileResponse[]>(`/projects/${projectId}/source`).then((r) => r.data),

  getById: (id: number) => apiClient.get<SourceCodeFileResponse>(`/source/${id}`).then((r) => r.data),

  delete: (id: number) => apiClient.delete<void>(`/source/${id}`).then((r) => r.data),
};

// Supported source extensions per the backend's FileValidationUtil
export const SUPPORTED_SOURCE_EXTENSIONS = [
  'java', 'js', 'jsx', 'ts', 'tsx', 'py', 'cpp', 'c', 'html', 'css', 'sql',
];
