import { useState, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PlayCircle, ShieldAlert, UploadCloud, Radar } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { SeverityBadge } from '@/components/ui/SeverityBadge';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { SourceBadge } from '@/components/ui/SourceBadge';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { ErrorState } from '@/components/common/ErrorState';
import { EmptyState } from '@/components/common/EmptyState';
import { ScoreCard } from '@/components/common/ScoreCard';
import { FilterBar } from '@/components/common/FilterBar';
import { useVulnerabilities, useRunSecurityAnalysis } from '@/hooks/useSecurity';
import { useProject } from '@/hooks/useProjects';
import { useTrivyStatus, useScanFilesystem, useImportTrivyFile } from '@/hooks/useTrivy';
import {
  useZapStatus,
  useZapScanStatus,
  useTriggerZapScan,
  useCollectZapResults,
  useImportZapFile,
} from '@/hooks/useZap';
import { useToast } from '@/components/ui/Toast';
import { formatDate } from '@/utils/formatters';
import { SEVERITY_ORDER } from '@/constants/statuses';
import type { Severity, VulnerabilityStatus, VulnerabilitySource } from '@/types/enums';
import type { ApiError } from '@/api/axios';

const STATUS_OPTIONS: VulnerabilityStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'FALSE_POSITIVE', 'ACCEPTED_RISK'];
const SOURCE_OPTIONS: VulnerabilitySource[] = ['RULE_BASED', 'TRIVY', 'ZAP', 'SONARQUBE'];

export default function SecurityAnalysis() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();
  const [severity, setSeverity] = useState<Severity | ''>('');
  const [status, setStatus] = useState<VulnerabilityStatus | ''>('');
  const [source, setSource] = useState<VulnerabilitySource | ''>('');

  const { data: project } = useProject(projectId);
  const {
    data: vulnsRaw,
    isLoading,
    isError,
    error,
    refetch,
  } = useVulnerabilities(projectId, {
    severity: severity || undefined,
    status: status || undefined,
  });
  const vulns = source ? vulnsRaw?.filter((v) => (v.source ?? 'RULE_BASED') === source) : vulnsRaw;
  const runAnalysis = useRunSecurityAnalysis(projectId);

  const { data: trivyStatus } = useTrivyStatus();
  const scanFilesystem = useScanFilesystem(projectId);
  const importTrivyFile = useImportTrivyFile(projectId);
  const trivyFileInputRef = useRef<HTMLInputElement>(null);

  const handleTrivyScan = async () => {
    try {
      const result = await scanFilesystem.mutateAsync(undefined);
      show(`Trivy scan found ${result.newFindingsStored} new finding(s), ${result.duplicatesSkipped} already known`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleTrivyFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const result = await importTrivyFile.mutateAsync(file);
      show(`Imported ${result.newFindingsStored} new finding(s) from Trivy report`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const { data: zapStatus } = useZapStatus();
  const [zapTarget, setZapTarget] = useState('');
  const [activeZapScanId, setActiveZapScanId] = useState<string | null>(null);
  const triggerZapScan = useTriggerZapScan(projectId);
  const collectZapResults = useCollectZapResults(projectId);
  const importZapFile = useImportZapFile(projectId);
  const zapFileInputRef = useRef<HTMLInputElement>(null);
  const { data: zapScanStatus } = useZapScanStatus(activeZapScanId);

  const handleZapScan = async () => {
    if (!zapTarget) {
      show('Choose an approved target to scan first', 'error');
      return;
    }
    try {
      const trigger = await triggerZapScan.mutateAsync(zapTarget);
      setActiveZapScanId(trigger.scanId);
      show(`ZAP baseline scan started against ${zapTarget}`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleCollectZapResults = async () => {
    try {
      const result = await collectZapResults.mutateAsync(zapTarget);
      show(`ZAP scan found ${result.newFindingsStored} new finding(s), ${result.duplicatesSkipped} already known`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleZapFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const result = await importZapFile.mutateAsync(file);
      show(`Imported ${result.newFindingsStored} new finding(s) from ZAP report`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const handleAnalyze = async () => {
    try {
      const results = await runAnalysis.mutateAsync();
      show(`Security analysis found ${results.length} finding(s)`, 'success');
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  const severityCounts = SEVERITY_ORDER.reduce<Record<Severity, number>>(
    (acc, s) => {
      acc[s] = vulns?.filter((v) => v.severity === s).length ?? 0;
      return acc;
    },
    { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0, INFO: 0 }
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">Security Analysis</h2>
          <p className="text-sm text-muted-foreground">Rule-based vulnerability scanning of uploaded source files</p>
        </div>
        <Button onClick={handleAnalyze} isLoading={runAnalysis.isPending}>
          <PlayCircle className="h-4 w-4" /> Run Analysis
        </Button>
      </div>

      <Card className="p-4">
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <div>
            <p className="text-sm font-semibold">Trivy Security Scanning</p>
            <p className="text-xs text-muted-foreground">
              Dependency/filesystem vulnerabilities and secret detection - combined into the table below
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {trivyStatus?.cliAvailable && (
              <Button variant="outline" size="sm" onClick={handleTrivyScan} isLoading={scanFilesystem.isPending}>
                Scan now (CLI)
              </Button>
            )}
            <input
              ref={trivyFileInputRef}
              type="file"
              accept=".json"
              className="hidden"
              onChange={handleTrivyFileSelected}
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => trivyFileInputRef.current?.click()}
              isLoading={importTrivyFile.isPending}
            >
              <UploadCloud className="h-4 w-4" /> Import Trivy JSON report
            </Button>
          </div>
        </div>
        {!trivyStatus?.cliAvailable && (
          <p className="mt-2 text-xs text-muted-foreground">
            Trivy CLI isn't installed on this backend, so "Scan now" is unavailable - import a JSON report instead
            (e.g. downloaded from the GitHub Actions pipeline's trivy-filesystem-report artifact).
          </p>
        )}
      </Card>

      <Card className="p-4">
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <div>
            <p className="text-sm font-semibold">OWASP ZAP Dynamic Scanning</p>
            <p className="text-xs text-muted-foreground">
              Baseline scan (spider crawl + passive scan only, no active attacks) against a pre-approved target
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {zapStatus?.apiAvailable && zapStatus.allowedTargets.length > 0 && (
              <>
                <Select value={zapTarget} onChange={(e) => setZapTarget(e.target.value)} className="w-56">
                  <option value="">Select approved target…</option>
                  {zapStatus.allowedTargets.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </Select>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleZapScan}
                  isLoading={triggerZapScan.isPending}
                  disabled={!zapTarget}
                >
                  <Radar className="h-4 w-4" /> Start scan
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCollectZapResults}
                  isLoading={collectZapResults.isPending}
                  disabled={!zapTarget}
                >
                  Pull results
                </Button>
              </>
            )}
            <input
              ref={zapFileInputRef}
              type="file"
              accept=".json"
              className="hidden"
              onChange={handleZapFileSelected}
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => zapFileInputRef.current?.click()}
              isLoading={importZapFile.isPending}
            >
              <UploadCloud className="h-4 w-4" /> Import ZAP JSON report
            </Button>
          </div>
        </div>
        {activeZapScanId && zapScanStatus && (
          <p className="mt-2 text-xs text-muted-foreground">
            Crawling {zapTarget}: {zapScanStatus.progressPercent}%
            {zapScanStatus.complete ? ' - crawl complete, click "Pull results" to import findings' : '…'}
          </p>
        )}
        {(!zapStatus?.apiAvailable || zapStatus.allowedTargets.length === 0) && (
          <p className="mt-2 text-xs text-muted-foreground">
            {!zapStatus?.apiAvailable
              ? "No ZAP daemon is configured on this backend, so live scanning is unavailable - "
              : 'No approved scan targets are configured (ZAP_ALLOWED_TARGETS), so live scanning is unavailable - '}
            import a ZAP JSON report instead (e.g. from the GitHub Actions pipeline).
          </p>
        )}
      </Card>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-6">
        <ScoreCard label="Security Score" score={project?.securityScore} />
        {SEVERITY_ORDER.map((s) => (
          <Card key={s} className="p-4">
            <p className="text-xs text-muted-foreground">{s}</p>
            <p className="text-xl font-bold tabular-nums">{severityCounts[s]}</p>
          </Card>
        ))}
      </div>

      <FilterBar>
        <Select value={severity} onChange={(e) => setSeverity(e.target.value as Severity | '')} className="w-40">
          <option value="">All Severities</option>
          {SEVERITY_ORDER.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </Select>
        <Select value={status} onChange={(e) => setStatus(e.target.value as VulnerabilityStatus | '')} className="w-40">
          <option value="">All Statuses</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </Select>
        <Select value={source} onChange={(e) => setSource(e.target.value as VulnerabilitySource | '')} className="w-40">
          <option value="">All Sources</option>
          {SOURCE_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </Select>
      </FilterBar>

      {isLoading && <LoadingSpinner label="Loading vulnerabilities…" />}
      {isError && <ErrorState error={error} onRetry={() => refetch()} />}

      {vulns && vulns.length === 0 && (
        <EmptyState
          icon={<ShieldAlert className="h-8 w-8" />}
          title="No vulnerabilities found"
          description="Run a security analysis, or adjust your filters."
        />
      )}

      {vulns && vulns.length > 0 && (
        <Card className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-sm">
            <thead>
              <tr className="border-b border-border text-left text-xs text-muted-foreground">
                <th className="px-4 py-3 font-medium">Severity</th>
                <th className="px-4 py-3 font-medium">Title</th>
                <th className="px-4 py-3 font-medium">Category</th>
                <th className="px-4 py-3 font-medium">Source</th>
                <th className="px-4 py-3 font-medium">File</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Detected</th>
              </tr>
            </thead>
            <tbody>
              {vulns.map((v) => (
                <tr key={v.id} className="border-b border-border last:border-0 hover:bg-muted/40">
                  <td className="px-4 py-3">
                    <SeverityBadge severity={v.severity} />
                  </td>
                  <td className="px-4 py-3">
                    <Link to={`/vulnerabilities/${v.id}`} className="font-medium text-foreground hover:text-primary hover:underline">
                      {v.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{v.category}</td>
                  <td className="px-4 py-3">
                    <SourceBadge source={v.source} />
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">
                    {v.affectedFile}
                    {v.lineNumber ? `:${v.lineNumber}` : ''}
                    {v.packageName ? (
                      <span className="block">
                        {v.packageName}
                        {v.installedVersion ? ` @ ${v.installedVersion}` : ''}
                        {v.fixedVersion ? ` → fix: ${v.fixedVersion}` : ''}
                      </span>
                    ) : null}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={v.status} />
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{formatDate(v.detectedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  );
}
