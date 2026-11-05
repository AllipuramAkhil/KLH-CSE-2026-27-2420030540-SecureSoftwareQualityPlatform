import { useParams } from 'react-router-dom';
import { useRef } from 'react';
import { PlayCircle, ShieldCheck, UploadCloud } from 'lucide-react';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { ScoreCard } from '@/components/common/ScoreCard';
import { useQualityHistory, useQualityLatest, useRunQualityAnalysis } from '@/hooks/useQuality';
import { useCoverageLatest, useCoverageTrend, useImportCoverage, useImportCoverageFile } from '@/hooks/useCoverage';
import { useSonarQubeStatus, useSonarQubeLatest, useImportSonarQube } from '@/hooks/useSonarQube';
import { sonarRatingToLetter } from '@/types/sonarqube';
import { useToast } from '@/components/ui/Toast';
import { formatDateTime, formatPercent } from '@/utils/formatters';
import type { ApiError } from '@/api/axios';

export default function QualityAnalysis() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();

  const { data: latest, isLoading, isError, error, refetch } = useQualityLatest(projectId);
  const { data: history } = useQualityHistory(projectId);
  const runAnalysis = useRunQualityAnalysis(projectId);

  const { data: coverage, isLoading: coverageLoading } = useCoverageLatest(projectId);
  const { data: coverageTrend } = useCoverageTrend(projectId);
  const importCoverage = useImportCoverage(projectId);
  const importCoverageFile = useImportCoverageFile(projectId);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data: sonarStatus } = useSonarQubeStatus();
  const { data: sonar, isLoading: sonarLoading, isError: sonarError } = useSonarQubeLatest(projectId);
  const importSonar = useImportSonarQube(projectId);

  const handleImportSonar = async () => {
    try {
      await importSonar.mutateAsync(undefined);
      show('SonarQube analysis imported', 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleImportCoverage = async () => {
    try {
      await importCoverage.mutateAsync();
      show('JaCoCo coverage imported', 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      await importCoverageFile.mutateAsync(file);
      show('JaCoCo coverage report uploaded and imported', 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleAnalyze = async () => {
    try {
      await runAnalysis.mutateAsync();
      show('Quality analysis completed', 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const trend = (history ?? [])
    .slice()
    .reverse()
    .map((h) => ({ date: formatDateTime(h.analyzedAt).split(',')[0], score: h.overallQualityScore }));

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">Quality Analysis</h2>
          <p className="text-sm text-muted-foreground">Rule-based static analysis of uploaded source files</p>
        </div>
        <Button onClick={handleAnalyze} isLoading={runAnalysis.isPending}>
          <PlayCircle className="h-4 w-4" /> Run Analysis
        </Button>
      </div>

      {isLoading && <LoadingSpinner label="Loading quality data…" />}
      {isError && (error as unknown as ApiError)?.status !== 404 && <ErrorState error={error} onRetry={() => refetch()} />}
      {isError && (error as unknown as ApiError)?.status === 404 && (
        <EmptyState
          icon={<ShieldCheck className="h-8 w-8" />}
          title="No quality analysis yet"
          description="Upload source files and run an analysis to see quality metrics."
          action={{ label: 'Run Analysis', onClick: handleAnalyze }}
        />
      )}

      {latest && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <ScoreCard label="Quality Score" score={latest.overallQualityScore} />
            <ScoreCard label="Maintainability" score={latest.maintainabilityIndex} />
            <ScoreCard label="Test Coverage" score={latest.testCoveragePercent} suffix="%" />
            <ScoreCard label="Duplication" score={latest.codeDuplicationPercent} suffix="%" invert />
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <MetricTile label="Lines of Code" value={latest.linesOfCode} />
            <MetricTile label="Methods" value={latest.numberOfMethods} />
            <MetricTile label="Cyclomatic Complexity" value={latest.cyclomaticComplexity} />
            <MetricTile label="Code Smells" value={latest.codeSmells} />
            <MetricTile label="Potential Bugs" value={latest.potentialBugs} />
            <MetricTile label="Technical Debt (hrs)" value={latest.technicalDebtHours} />
            <MetricTile label="Last Analyzed" value={formatDateTime(latest.analyzedAt)} isText />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Quality Trend</CardTitle>
            </CardHeader>
            <CardContent className="h-64">
              {trend.length < 2 ? (
                <EmptyState title="Not enough history yet" description="Run analysis multiple times to see a trend." />
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={trend}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                    <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                    <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                    <Line type="monotone" dataKey="score" stroke="hsl(var(--primary))" strokeWidth={2} dot />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </>
      )}

      <Card>
        <CardHeader>
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <CardTitle>Real Coverage (JaCoCo)</CardTitle>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={handleImportCoverage} isLoading={importCoverage.isPending}>
                Import from backend build
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                accept=".xml"
                className="hidden"
                onChange={handleFileSelected}
              />
              <Button
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
                isLoading={importCoverageFile.isPending}
              >
                <UploadCloud className="h-4 w-4" /> Upload jacoco.xml
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {coverageLoading && <LoadingSpinner label="Loading coverage…" />}
          {!coverageLoading && !coverage && (
            <EmptyState
              title="No JaCoCo report imported yet"
              description="Run `mvn verify` in the backend to generate target/site/jacoco/jacoco.xml, then import it from the backend's local path or upload the file directly (e.g. as a GitHub Actions artifact)."
            />
          )}
          {coverage && (
            <>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                <ScoreCard label="Overall Coverage" score={coverage.overallCoveragePercent} suffix="%" />
                <ScoreCard label="Line Coverage" score={coverage.lineCoveragePercent} suffix="%" />
                <ScoreCard label="Branch Coverage" score={coverage.branchCoveragePercent} suffix="%" />
              </div>
              <p className="text-xs text-muted-foreground">
                Source: {coverage.reportSource} · Imported {formatDateTime(coverage.importedAt)} ·{' '}
                {coverage.linesCovered}/{coverage.linesCovered + coverage.linesMissed} lines,{' '}
                {coverage.branchesCovered}/{coverage.branchesCovered + coverage.branchesMissed} branches covered
              </p>
            </>
          )}

          <div className="h-56">
            {(coverageTrend?.length ?? 0) < 2 ? (
              <EmptyState title="Not enough history yet" description="Import coverage multiple times to see a trend." />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={coverageTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                  <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} stroke="hsl(var(--muted-foreground))" />
                  <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                  <Line type="monotone" dataKey="lineCoveragePercent" name="Line %" stroke="hsl(var(--primary))" strokeWidth={2} dot />
                  <Line type="monotone" dataKey="branchCoveragePercent" name="Branch %" stroke="hsl(var(--info))" strokeWidth={2} dot />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <CardTitle>SonarQube Analysis</CardTitle>
            {sonarStatus?.configured && (
              <Button variant="outline" size="sm" onClick={handleImportSonar} isLoading={importSonar.isPending}>
                Import latest analysis
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {sonarStatus && !sonarStatus.configured && (
            <EmptyState
              title="SonarQube is not configured"
              description="Set SONAR_HOST_URL, SONAR_TOKEN, and SONAR_PROJECT_KEY on the backend to enable this. The rule-based analysis above continues to work either way."
            />
          )}
          {sonarStatus?.configured && sonarLoading && <LoadingSpinner label="Loading SonarQube data…" />}
          {sonarStatus?.configured && !sonarLoading && sonarError && !sonar && (
            <EmptyState
              title="No SonarQube analysis imported yet"
              description="Click “Import latest analysis” to fetch current measures from your SonarQube server."
              action={{ label: 'Import latest analysis', onClick: handleImportSonar }}
            />
          )}
          {sonar && (
            <>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                <MetricTile label="Bugs" value={sonar.bugs ?? 0} />
                <MetricTile label="Vulnerabilities" value={sonar.vulnerabilities ?? 0} />
                <MetricTile label="Code Smells" value={sonar.codeSmells ?? 0} />
                <MetricTile
                  label="Technical Debt"
                  value={sonar.technicalDebtMinutes != null ? `${Math.round(sonar.technicalDebtMinutes / 60)}h` : '-'}
                  isText
                />
              </div>
              <div className="grid grid-cols-3 gap-3 sm:grid-cols-3">
                <MetricTile label="Maintainability" value={sonarRatingToLetter(sonar.maintainabilityRating)} isText />
                <MetricTile label="Reliability" value={sonarRatingToLetter(sonar.reliabilityRating)} isText />
                <MetricTile label="Security" value={sonarRatingToLetter(sonar.securityRating)} isText />
              </div>
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-2">
                <ScoreCard label="Coverage" score={sonar.coveragePercent ?? 0} suffix="%" />
                <ScoreCard label="Duplication" score={sonar.duplicatedLinesDensity ?? 0} suffix="%" invert />
              </div>
              <p className="text-xs text-muted-foreground">
                Project: {sonar.sonarProjectKey} · Imported {formatDateTime(sonar.importedAt)}
              </p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function MetricTile({ label, value, isText }: { label: string; value: number | string; isText?: boolean }) {
  return (
    <Card>
      <CardContent className="pt-4">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className={isText ? 'text-sm font-semibold' : 'text-xl font-bold tabular-nums'}>
          {isText ? value : typeof value === 'number' ? value : formatPercent(Number(value))}
        </p>
      </CardContent>
    </Card>
  );
}
