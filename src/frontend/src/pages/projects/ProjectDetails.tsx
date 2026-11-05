import { useParams } from 'react-router-dom';
import { ShieldCheck, ShieldAlert, Gauge, Calendar, GitBranch, Code2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { ScoreCard } from '@/components/common/ScoreCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { useProject } from '@/hooks/useProjects';
import { formatDateTime } from '@/utils/formatters';
import { SourceCodeUpload } from '@/components/projects/SourceCodeUpload';

export default function ProjectDetails() {
  const { id } = useParams();
  const projectId = Number(id);
  const { data: project, isLoading, isError, error, refetch } = useProject(projectId);

  if (isLoading) return <LoadingSpinner label="Loading project overview…" />;
  if (isError) return <ErrorState error={error} onRetry={() => refetch()} />;
  if (!project) return null;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <ScoreCard label="Quality Score" score={project.qualityScore} icon={<ShieldCheck className="h-4 w-4" />} />
        <ScoreCard label="Security Score" score={project.securityScore} icon={<ShieldAlert className="h-4 w-4" />} />
        <ScoreCard label="Risk Score" score={project.riskScore} icon={<Gauge className="h-4 w-4" />} invert />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Project information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          {project.description && <p className="text-muted-foreground">{project.description}</p>}
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <InfoRow icon={<GitBranch className="h-4 w-4" />} label="Repository" value={project.repositoryUrl || '—'} />
            <InfoRow icon={<Code2 className="h-4 w-4" />} label="Technology" value={project.technologyStack || '—'} />
            <InfoRow icon={<GitBranch className="h-4 w-4" />} label="Branch" value={project.branch || '—'} />
            <InfoRow icon={<Calendar className="h-4 w-4" />} label="Last updated" value={formatDateTime(project.updatedAt)} />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Source Code</CardTitle>
        </CardHeader>
        <CardContent>
          <SourceCodeUpload projectId={projectId} />
        </CardContent>
      </Card>
    </div>
  );
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-muted-foreground">{icon}</span>
      <span className="text-muted-foreground">{label}:</span>
      <span className="truncate font-medium text-foreground">{value}</span>
    </div>
  );
}
