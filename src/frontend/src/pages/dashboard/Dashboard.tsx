import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShieldCheck,
  ShieldAlert,
  Gauge,
  TestTube2,
  CheckCircle2,
  XCircle,
  AlertOctagon,
  Bug,
  GitCommit,
  FolderKanban,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { ScoreCard } from '@/components/common/ScoreCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { useProjectList } from '@/hooks/useProjects';
import { useDashboard } from '@/hooks/useDashboard';
import { sonarRatingToLetter } from '@/types/sonarqube';
import { appStore } from '@/store/appStore';
import { ROUTES } from '@/constants/routes';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  Legend,
} from 'recharts';

const RISK_COLORS: Record<string, string> = {
  CRITICAL: '#ef4444',
  HIGH: '#f97316',
  MEDIUM: '#eab308',
  LOW: '#22c55e',
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { data: projectsPage, isLoading: projectsLoading } = useProjectList({ size: 50 });
  const [projectId, setProjectId] = useState<number | null>(appStore.getLastProjectId());

  useEffect(() => {
    if (!projectId && projectsPage?.content.length) {
      setProjectId(projectsPage.content[0].id);
    }
  }, [projectsPage, projectId]);

  useEffect(() => {
    if (projectId) appStore.setLastProjectId(projectId);
  }, [projectId]);

  const { data: dashboard, isLoading, isError, error, refetch } = useDashboard(projectId ?? undefined);

  if (projectsLoading) return <LoadingSpinner label="Loading your projects…" />;

  if (!projectsPage?.content.length) {
    return (
      <EmptyState
        icon={<FolderKanban className="h-8 w-8" />}
        title="No projects yet"
        description="Create your first project to start running quality, security, and adaptive testing analysis."
        action={{ label: 'Create Project', onClick: () => navigate(ROUTES.PROJECT_NEW) }}
      />
    );
  }

  const riskDistribution = dashboard
    ? Object.entries(dashboard.riskDistribution).map(([level, count]) => ({ name: level, value: count }))
    : [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-sm text-muted-foreground">Quality, security, and risk overview</p>
        </div>
        <Select value={projectId ?? ''} onChange={(e) => setProjectId(Number(e.target.value))} className="sm:w-64">
          {projectsPage.content.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </Select>
      </div>

      {isLoading && <LoadingSpinner label="Loading dashboard…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {dashboard && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <ScoreCard label="Quality Score" score={dashboard.qualityScore} icon={<ShieldCheck className="h-4 w-4" />} />
            <ScoreCard label="Security Score" score={dashboard.securityScore} icon={<ShieldAlert className="h-4 w-4" />} />
            <ScoreCard label="Risk Score" score={dashboard.riskScore} icon={<Gauge className="h-4 w-4" />} invert />
            <ScoreCard label="Test Coverage" score={dashboard.testCoverage} suffix="%" icon={<TestTube2 className="h-4 w-4" />} />
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <StatTile icon={<TestTube2 className="h-4 w-4 text-info" />} label="Total Tests" value={dashboard.totalTests} />
            <StatTile icon={<XCircle className="h-4 w-4 text-danger" />} label="Failed Tests" value={dashboard.failedTests} />
            <StatTile icon={<AlertOctagon className="h-4 w-4 text-danger" />} label="Critical Vulns" value={dashboard.criticalVulnerabilities} />
            <StatTile icon={<Bug className="h-4 w-4 text-warning" />} label="Open Defects" value={dashboard.openDefects} />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Test Execution Trend</CardTitle>
              </CardHeader>
              <CardContent className="h-64">
                {dashboard.testExecutionTrends.length === 0 ? (
                  <EmptyState title="No execution data yet" description="Run tests to see trends here." />
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={dashboard.testExecutionTrends}>
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                      <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                      <YAxis tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" allowDecimals={false} />
                      <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                      <Line type="monotone" dataKey="executions" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Risk Distribution</CardTitle>
              </CardHeader>
              <CardContent className="h-64">
                {riskDistribution.every((r) => r.value === 0) ? (
                  <EmptyState title="No risk data yet" description="Run a risk analysis to populate this chart." />
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={riskDistribution} dataKey="value" nameKey="name" innerRadius={50} outerRadius={80} paddingAngle={2}>
                        {riskDistribution.map((entry) => (
                          <Cell key={entry.name} fill={RISK_COLORS[entry.name] ?? '#94a3b8'} />
                        ))}
                      </Pie>
                      <Legend wrapperStyle={{ fontSize: 11 }} />
                      <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Real Test Coverage (JaCoCo)</CardTitle>
            </CardHeader>
            <CardContent>
              {dashboard.realCoverageAvailable ? (
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                  <ScoreCard label="Overall Coverage" score={dashboard.jacocoOverallCoveragePercent} suffix="%" />
                  <ScoreCard label="Line Coverage" score={dashboard.jacocoLineCoveragePercent} suffix="%" />
                  <ScoreCard label="Branch Coverage" score={dashboard.jacocoBranchCoveragePercent} suffix="%" />
                </div>
              ) : (
                <EmptyState
                  icon={<TestTube2 className="h-8 w-8" />}
                  title="No JaCoCo report imported yet"
                  description="Run the backend's Maven build (mvn verify) and import target/site/jacoco/jacoco.xml from the Quality Analysis page, or let the CI/CD pipeline push it automatically, to see real line/branch coverage here."
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>SonarQube Analysis</CardTitle>
            </CardHeader>
            <CardContent>
              {dashboard.sonarQubeAvailable ? (
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <StatTile icon={<Bug className="h-4 w-4 text-danger" />} label="Bugs" value={dashboard.sonarBugs ?? 0} />
                  <StatTile
                    icon={<ShieldAlert className="h-4 w-4 text-danger" />}
                    label="Vulnerabilities"
                    value={dashboard.sonarVulnerabilities ?? 0}
                  />
                  <StatTile icon={<AlertOctagon className="h-4 w-4 text-warning" />} label="Code Smells" value={dashboard.sonarCodeSmells ?? 0} />
                  <RatingTile label="Maintainability" rating={dashboard.sonarMaintainabilityRating} />
                </div>
              ) : (
                <EmptyState
                  icon={<ShieldCheck className="h-8 w-8" />}
                  title="No SonarQube analysis imported yet"
                  description="Configure SONAR_HOST_URL / SONAR_TOKEN / SONAR_PROJECT_KEY on the backend, then import from the Quality Analysis page."
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Git Code Churn</CardTitle>
            </CardHeader>
            <CardContent>
              {dashboard.codeChurnAvailable && dashboard.mostChurnedComponents?.length ? (
                <div>
                  <p className="mb-2 text-xs text-muted-foreground">Most-changed files (recent-weighted, from local git history)</p>
                  <ul className="space-y-2">
                    {dashboard.mostChurnedComponents.map((path) => (
                      <li key={path} className="flex items-center gap-2 text-sm">
                        <GitCommit className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                        <span className="truncate font-mono text-xs text-foreground">{path}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : (
                <EmptyState
                  icon={<GitCommit className="h-8 w-8" />}
                  title="No code churn data yet"
                  description="Run churn analysis from the Risk Assessment page to compute per-file git activity from local repository history - this also feeds the ML risk prediction's code_churn feature."
                />
              )}
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Vulnerability Overview</CardTitle>
              </CardHeader>
              <CardContent className="h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={[
                      { name: 'Critical', value: dashboard.criticalVulnerabilities },
                      { name: 'High', value: dashboard.highVulnerabilities },
                      { name: 'Other', value: Math.max(0, dashboard.totalVulnerabilities - dashboard.criticalVulnerabilities - dashboard.highVulnerabilities) },
                    ]}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" allowDecimals={false} />
                    <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                    <Bar dataKey="value" fill="hsl(var(--danger))" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Recent Activity</CardTitle>
              </CardHeader>
              <CardContent>
                {dashboard.recentAnalyses.length === 0 ? (
                  <EmptyState title="No recent activity" description="Analysis cycles will appear here." />
                ) : (
                  <ul className="space-y-3">
                    {dashboard.recentAnalyses.map((entry, i) => (
                      <li key={i} className="flex items-start gap-2 text-sm">
                        <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0 text-success" />
                        <span className="text-foreground">{entry}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="flex justify-end">
            <Link to={ROUTES.PROJECT_DETAILS(projectId ?? '')}>
              <Button variant="outline">Open project workspace</Button>
            </Link>
          </div>
        </>
      )}
    </div>
  );
}

function RatingTile({ label, rating }: { label: string; rating: number | null | undefined }) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3 pt-4">
        <div className="rounded-md bg-muted p-2">
          <span className="block h-4 w-4 text-center text-xs font-bold leading-4">{sonarRatingToLetter(rating)}</span>
        </div>
        <div>
          <p className="text-lg font-bold tabular-nums">{sonarRatingToLetter(rating)}</p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}

function StatTile({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <Card>
      <CardContent className="flex items-center gap-3 pt-4">
        <div className="rounded-md bg-muted p-2">{icon}</div>
        <div>
          <p className="text-lg font-bold tabular-nums">{value}</p>
          <p className="text-xs text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}
