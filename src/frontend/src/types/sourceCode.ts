// Mirrors the Map<String,Object> returned by SourceCodeController
export interface SourceCodeFileResponse {
  id: number;
  projectId: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  linesOfCode: number | null;
  uploadedAt: string;
}
