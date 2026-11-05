import { useCallback, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UploadCloud, FileCode2, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Progress } from '@/components/ui/Progress';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { ErrorState } from '@/components/common/ErrorState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { useToast } from '@/components/ui/Toast';
import { sourceCodeApi, SUPPORTED_SOURCE_EXTENSIONS } from '@/api/sourceCodeApi';
import { formatBytes, formatDateTime } from '@/utils/formatters';
import { cn } from '@/components/ui/utils';
import type { ApiError } from '@/api/axios';

export function SourceCodeUpload({ projectId }: { projectId: number }) {
  const { show } = useToast();
  const qc = useQueryClient();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragOver, setDragOver] = useState(false);
  const [progress, setProgress] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const filesQuery = useQuery({
    queryKey: ['source', projectId],
    queryFn: () => sourceCodeApi.listByProject(projectId),
    enabled: !!projectId,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => sourceCodeApi.upload(projectId, file, setProgress),
    onSuccess: () => {
      show('File uploaded successfully', 'success');
      qc.invalidateQueries({ queryKey: ['source', projectId] });
      setProgress(null);
    },
    onError: (err) => {
      show((err as unknown as ApiError).message, 'error');
      setProgress(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (fileId: number) => sourceCodeApi.delete(fileId),
    onSuccess: () => {
      show('File deleted', 'success');
      qc.invalidateQueries({ queryKey: ['source', projectId] });
      setDeleteTarget(null);
    },
    onError: (err) => show((err as unknown as ApiError).message, 'error'),
  });

  const validateAndUpload = useCallback(
    (file: File) => {
      const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
      if (!SUPPORTED_SOURCE_EXTENSIONS.includes(ext)) {
        show(`.${ext} files are not supported. Allowed: ${SUPPORTED_SOURCE_EXTENSIONS.join(', ')}`, 'error');
        return;
      }
      if (file.size > 20 * 1024 * 1024) {
        show('File exceeds the 20MB upload limit', 'error');
        return;
      }
      uploadMutation.mutate(file);
    },
    [uploadMutation, show]
  );

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) validateAndUpload(file);
  };

  return (
    <div className="space-y-4">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className={cn(
          'flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 text-center transition-colors',
          dragOver ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50'
        )}
      >
        <UploadCloud className="h-8 w-8 text-muted-foreground" />
        <p className="text-sm font-medium">Drag &amp; drop a source file, or click to browse</p>
        <p className="text-xs text-muted-foreground">Supported: {SUPPORTED_SOURCE_EXTENSIONS.join(', ')} · Max 20MB</p>
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          accept={SUPPORTED_SOURCE_EXTENSIONS.map((e) => `.${e}`).join(',')}
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) validateAndUpload(file);
            e.target.value = '';
          }}
        />
      </div>

      {progress !== null && (
        <div>
          <div className="mb-1 flex justify-between text-xs text-muted-foreground">
            <span>Uploading…</span>
            <span>{progress}%</span>
          </div>
          <Progress value={progress} />
        </div>
      )}

      {filesQuery.isLoading && <LoadingSpinner label="Loading source files…" />}
      {filesQuery.isError && <ErrorState error={filesQuery.error} onRetry={() => filesQuery.refetch()} />}

      {filesQuery.data && filesQuery.data.length === 0 && (
        <EmptyState icon={<FileCode2 className="h-8 w-8" />} title="No source files uploaded yet" />
      )}

      {filesQuery.data && filesQuery.data.length > 0 && (
        <div className="overflow-x-auto rounded-md border border-border">
          <table className="w-full min-w-[560px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-3 py-2 font-medium">File</th>
                <th className="px-3 py-2 font-medium">Type</th>
                <th className="px-3 py-2 font-medium">Size</th>
                <th className="px-3 py-2 font-medium">Uploaded</th>
                <th className="px-3 py-2 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filesQuery.data.map((f) => (
                <tr key={f.id} className="border-b border-border last:border-0">
                  <td className="flex items-center gap-2 px-3 py-2 font-medium">
                    <FileCode2 className="h-3.5 w-3.5 text-muted-foreground" />
                    {f.fileName}
                  </td>
                  <td className="px-3 py-2 text-xs uppercase text-muted-foreground">{f.fileType}</td>
                  <td className="px-3 py-2 text-xs text-muted-foreground">{formatBytes(f.fileSize)}</td>
                  <td className="px-3 py-2 text-xs text-muted-foreground">{formatDateTime(f.uploadedAt)}</td>
                  <td className="px-3 py-2 text-right">
                    <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(f.id)} aria-label="Delete file">
                      <Trash2 className="h-4 w-4 text-danger" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
        title="Delete source file?"
        description="This file will be permanently removed from the project."
        confirmLabel="Delete"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
