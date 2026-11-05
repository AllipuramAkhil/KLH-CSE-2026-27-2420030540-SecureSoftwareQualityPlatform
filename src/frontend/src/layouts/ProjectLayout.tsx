import { NavLink, Outlet, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { cn } from '@/components/ui/utils';
import { useProject } from '@/hooks/useProjects';
import { Skeleton } from '@/components/ui/Skeleton';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ROUTES, PROJECT_TABS } from '@/constants/routes';
import { appStore } from '@/store/appStore';
import { useEffect } from 'react';

const TAB_LABELS: Record<(typeof PROJECT_TABS)[number], string> = {
  overview: 'Overview',
  source: 'Source Code',
  quality: 'Quality',
  security: 'Security',
  tests: 'Tests',
  'adaptive-testing': 'Adaptive Testing',
  risk: 'Risk',
  mlops: 'MLOps',
  ai: 'AI Analysis',
  reports: 'Reports',
  cicd: 'CI/CD',
};

export function ProjectLayout() {
  const { id } = useParams();
  const projectId = Number(id);
  const navigate = useNavigate();
  const { data: project, isLoading } = useProject(projectId);

  useEffect(() => {
    if (projectId) appStore.setLastProjectId(projectId);
  }, [projectId]);

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(ROUTES.PROJECTS)} aria-label="Back to projects">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        {isLoading ? (
          <Skeleton className="h-6 w-48" />
        ) : (
          <div className="flex items-center gap-2">
            <h1 className="text-lg font-bold">{project?.name}</h1>
            {project && <StatusBadge status={project.status} />}
          </div>
        )}
      </div>

      <div className="flex items-center gap-1 overflow-x-auto border-b border-border scrollbar-thin" role="tablist">
        {PROJECT_TABS.map((tab) => (
          <NavLink
            key={tab}
            to={tab === 'overview' ? ROUTES.PROJECT_DETAILS(projectId) : ROUTES.PROJECT_TAB(projectId, tab)}
            end={tab === 'overview'}
            className={({ isActive }) =>
              cn(
                'shrink-0 whitespace-nowrap border-b-2 px-3 py-2.5 text-sm font-medium transition-colors',
                isActive ? 'border-primary text-foreground' : 'border-transparent text-muted-foreground hover:text-foreground'
              )
            }
          >
            {TAB_LABELS[tab]}
          </NavLink>
        ))}
      </div>

      <Outlet />
    </div>
  );
}
