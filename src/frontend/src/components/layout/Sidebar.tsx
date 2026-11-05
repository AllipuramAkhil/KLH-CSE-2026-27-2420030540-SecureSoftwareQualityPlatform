import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  FolderKanban,
  ShieldCheck,
  ShieldAlert,
  ListChecks,
  Zap,
  Gauge,
  Sparkles,
  FileText,
  GitBranch,
  Bell,
  Settings,
  Users,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react';
import { cn } from '@/components/ui/utils';
import { useAuth } from '@/hooks/useAuth';
import { NAV_ACCESS } from '@/constants/roles';
import { ROUTES, type ProjectTab } from '@/constants/routes';
import { appStore } from '@/store/appStore';
import { useToast } from '@/components/ui/Toast';
import type { Role } from '@/types/enums';

interface NavItem {
  key: string;
  label: string;
  icon: typeof LayoutDashboard;
  /** Global route, or a project tab name that requires an active project */
  to: string;
  projectTab?: ProjectTab;
}

const NAV_ITEMS: NavItem[] = [
  { key: 'dashboard', label: 'Dashboard', to: ROUTES.DASHBOARD, icon: LayoutDashboard },
  { key: 'projects', label: 'Projects', to: ROUTES.PROJECTS, icon: FolderKanban },
  { key: 'quality', label: 'Quality', to: '', icon: ShieldCheck, projectTab: 'quality' },
  { key: 'security', label: 'Security', to: '', icon: ShieldAlert, projectTab: 'security' },
  { key: 'testing', label: 'Testing', to: '', icon: ListChecks, projectTab: 'tests' },
  { key: 'adaptive', label: 'Adaptive Testing', to: '', icon: Zap, projectTab: 'adaptive-testing' },
  { key: 'risk', label: 'Risk', to: '', icon: Gauge, projectTab: 'risk' },
  { key: 'ai', label: 'AI Analysis', to: '', icon: Sparkles, projectTab: 'ai' },
  { key: 'reports', label: 'Reports', to: '', icon: FileText, projectTab: 'reports' },
  { key: 'cicd', label: 'CI/CD', to: '', icon: GitBranch, projectTab: 'cicd' },
  { key: 'notifications', label: 'Notifications', to: '/notifications', icon: Bell },
];

export function Sidebar({
  collapsed,
  onToggle,
  onNavigate,
}: {
  collapsed: boolean;
  onToggle: () => void;
  onNavigate?: () => void;
}) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { show } = useToast();
  const role = (user?.role ?? 'DEVELOPER') as Role;

  const visibleItems = NAV_ITEMS.filter((item) => NAV_ACCESS[item.key]?.includes(role));

  const handleProjectScopedNav = (item: NavItem) => {
    const lastProjectId = appStore.getLastProjectId();
    if (!lastProjectId) {
      show('Select a project first to view its ' + item.label.toLowerCase(), 'info');
      navigate(ROUTES.PROJECTS);
      return;
    }
    navigate(ROUTES.PROJECT_TAB(lastProjectId, item.projectTab as string));
    onNavigate?.();
  };

  return (
    <aside
      className={cn(
        'flex h-full flex-col border-r border-border bg-card transition-all duration-200',
        collapsed ? 'w-[68px]' : 'w-60'
      )}
    >
      <div className="flex h-14 items-center gap-2 border-b border-border px-4">
        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-primary text-xs font-bold text-primary-foreground">
          A
        </div>
        {!collapsed && <span className="truncate text-sm font-bold tracking-tight">ASSQP</span>}
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto px-2 py-3 scrollbar-thin">
        {visibleItems.map((item) =>
          item.projectTab ? (
            <button
              key={item.key}
              onClick={() => handleProjectScopedNav(item)}
              className="flex w-full items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
            >
              <item.icon className="h-4 w-4 shrink-0" />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </button>
          ) : (
            <NavLink
              key={item.key}
              to={item.to}
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                )
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </NavLink>
          )
        )}
      </nav>

      <div className="space-y-0.5 border-t border-border px-2 py-3">
        {role === 'ADMIN' && (
          <NavLink
            to={ROUTES.USERS}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors',
                isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              )
            }
          >
            <Users className="h-4 w-4 shrink-0" />
            {!collapsed && <span>Users</span>}
          </NavLink>
        )}
        <NavLink
          to={ROUTES.PROFILE}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors',
              isActive ? 'bg-primary/10 text-primary' : 'text-muted-foreground hover:bg-muted hover:text-foreground'
            )
          }
        >
          <Settings className="h-4 w-4 shrink-0" />
          {!collapsed && <span>Settings</span>}
        </NavLink>
        <button
          onClick={onToggle}
          className="hidden md:flex w-full items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium text-muted-foreground hover:bg-muted hover:text-foreground"
        >
          {collapsed ? <ChevronsRight className="h-4 w-4" /> : <ChevronsLeft className="h-4 w-4" />}
          {!collapsed && <span>Collapse</span>}
        </button>
      </div>
    </aside>
  );
}
