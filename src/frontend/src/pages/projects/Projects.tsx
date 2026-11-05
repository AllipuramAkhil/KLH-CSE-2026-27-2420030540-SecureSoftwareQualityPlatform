import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, FolderKanban, Trash2, ExternalLink, Pencil } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SearchInput } from '@/components/common/SearchInput';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Pagination } from '@/components/ui/Pagination';
import { useProjectList, useDeleteProject } from '@/hooks/useProjects';
import { useToast } from '@/components/ui/Toast';
import { formatDate, formatScore } from '@/utils/formatters';
import { scoreToRiskLevel } from '@/utils/risk';
import { RiskBadge } from '@/components/ui/RiskBadge';
import { ROUTES } from '@/constants/routes';
import { appStore } from '@/store/appStore';
import type { ApiError } from '@/api/axios';

export default function Projects() {
  const navigate = useNavigate();
  const { show } = useToast();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const { data, isLoading, isError, error, refetch } = useProjectList({ page, size: 10 });
  const deleteProject = useDeleteProject();

  const filtered = data?.content.filter((p) => p.name.toLowerCase().includes(search.toLowerCase())) ?? [];

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteProject.mutateAsync(deleteTarget);
      show('Project deleted successfully', 'success');
      setDeleteTarget(null);
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const openProject = (id: number) => {
    appStore.setLastProjectId(id);
    navigate(ROUTES.PROJECT_DETAILS(id));
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Projects</h1>
          <p className="text-sm text-muted-foreground">Manage your quality &amp; security analysis projects</p>
        </div>
        <Button onClick={() => navigate(ROUTES.PROJECT_NEW)}>
          <Plus className="h-4 w-4" /> New Project
        </Button>
      </div>

      <SearchInput value={search} onChange={setSearch} placeholder="Search projects…" className="max-w-sm" />

      {isLoading && <LoadingSpinner label="Loading projects…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered.length === 0 && (
        <EmptyState
          icon={<FolderKanban className="h-8 w-8" />}
          title="No projects found"
          description={search ? 'Try a different search term.' : 'Create your first project to get started.'}
          action={!search ? { label: 'Create Project', onClick: () => navigate(ROUTES.PROJECT_NEW) } : undefined}
        />
      )}

      {filtered.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[820px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Project</th>
                <th className="px-4 py-3 font-medium">Technology</th>
                <th className="px-4 py-3 font-medium">Quality</th>
                <th className="px-4 py-3 font-medium">Security</th>
                <th className="px-4 py-3 font-medium">Risk</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Updated</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((p) => (
                <tr key={p.id} className="border-b border-border last:border-0 hover:bg-muted/40">
                  <td className="px-4 py-3">
                    <button onClick={() => openProject(p.id)} className="font-medium text-foreground hover:text-primary hover:underline">
                      {p.name}
                    </button>
                    <p className="max-w-[220px] truncate text-xs text-muted-foreground">{p.description}</p>
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{p.technologyStack || '—'}</td>
                  <td className="px-4 py-3 tabular-nums">{formatScore(p.qualityScore)}</td>
                  <td className="px-4 py-3 tabular-nums">{formatScore(p.securityScore)}</td>
                  <td className="px-4 py-3">
                    <RiskBadge level={scoreToRiskLevel(p.riskScore)} />
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDate(p.updatedAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-1">
                      <Button variant="ghost" size="icon" onClick={() => openProject(p.id)} aria-label="Open project">
                        <ExternalLink className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => navigate(ROUTES.PROJECT_EDIT(p.id))} aria-label="Edit project">
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => setDeleteTarget(p.id)} aria-label="Delete project">
                        <Trash2 className="h-4 w-4 text-danger" />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="px-4 pb-3">
            <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete project?"
        description="This will permanently delete the project and all associated analysis data. This action cannot be undone."
        confirmLabel="Delete"
        isLoading={deleteProject.isPending}
      />
    </div>
  );
}
