import { useParams } from 'react-router-dom';
import { Brain, RefreshCw, History, Info } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { ScoreCard } from '@/components/common/ScoreCard';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { useMlStatus, useModelInfo, useModelMetrics, usePredictionHistory, useRetrainModel } from '@/hooks/useMlOps';
import { useToast } from '@/components/ui/Toast';
import { formatDate } from '@/utils/formatters';
import { cn } from '@/components/ui/utils';
import type { ApiError } from '@/api/axios';

export default function MlOps() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();

  const { data: status, isLoading: statusLoading } = useMlStatus();
  const available = !!status?.available;

  const { data: modelInfo, isError: modelInfoError } = useModelInfo(available);
  const { data: metrics, isError: metricsError } = useModelMetrics(available);
  const { data: history } = usePredictionHistory(projectId, available);
  const retrain = useRetrainModel();

  const handleRetrain = async () => {
    try {
      const result = await retrain.mutateAsync(true);
      show(
        result.status === 'SUCCESS'
          ? `Retrained on ${result.metadata?.datasetRows ?? '?'} rows - model version ${result.metadata?.modelVersion}`
          : `Retrain failed: ${result.message}`,
        result.status === 'SUCCESS' ? 'success' : 'error'
      );
    } catch (err) {
      show((err as ApiError).message, 'error');
    }
  };

  if (statusLoading) return <LoadingSpinner label="Checking ML service…" />;

  if (!available) {
    return (
      <div className="space-y-4">
        <h2 className="text-lg font-semibold">MLOps</h2>
        <EmptyState
          icon={<Brain className="h-8 w-8" />}
          title="ML service not configured"
          description="Set ML_SERVICE_URL on the backend to enable hybrid ML risk prediction and this dashboard - see ml-service/README.md. The rest of ASSQP works fully without it."
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 className="text-lg font-semibold">MLOps</h2>
          <p className="text-sm text-muted-foreground">
            Model version, evaluation metrics, and prediction history for the ML risk-prediction service
          </p>
        </div>
        <Button onClick={handleRetrain} isLoading={retrain.isPending}>
          <RefreshCw className="h-4 w-4" /> Retrain model
        </Button>
      </div>

      <div className="flex items-start gap-2 rounded-lg border border-info/30 bg-info/10 p-3 text-xs text-muted-foreground">
        <Info className="mt-0.5 h-4 w-4 shrink-0 text-info" />
        <p>
          The model itself isn't project-specific - it's shared across all of ASSQP. Prediction history below is
          scoped to this project (component-level ML calls made during risk analysis).
        </p>
      </div>

      {metricsError || modelInfoError ? (
        <EmptyState
          icon={<Brain className="h-8 w-8" />}
          title="No trained model yet"
          description="The ML service is reachable but hasn't trained a model. Click 'Retrain model', or run training/train.py directly in ml-service/."
          action={{ label: 'Retrain model', onClick: handleRetrain }}
        />
      ) : (
        <>
          {metrics && (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <ScoreCard label="Accuracy" score={metrics.accuracy * 100} suffix="%" />
              <ScoreCard label="Precision (weighted)" score={metrics.precisionWeighted * 100} suffix="%" />
              <ScoreCard label="Recall (weighted)" score={metrics.recallWeighted * 100} suffix="%" />
              <ScoreCard label="F1 (weighted)" score={metrics.f1Weighted * 100} suffix="%" />
            </div>
          )}

          {modelInfo && (
            <Card>
              <CardHeader>
                <CardTitle>Model Info</CardTitle>
              </CardHeader>
              <CardContent className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm sm:grid-cols-3">
                <div>
                  <p className="text-xs text-muted-foreground">Version</p>
                  <p className="font-mono font-medium">{modelInfo.modelVersion}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Algorithm</p>
                  <p className="font-medium">{modelInfo.algorithm}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Trained At</p>
                  <p className="font-medium">{formatDate(modelInfo.trainedAt)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Dataset Rows</p>
                  <p className="font-medium">{modelInfo.datasetRows.toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Dataset Hash</p>
                  <p className="font-mono text-xs">{modelInfo.datasetHash}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Dataset Path</p>
                  <p className="font-mono text-xs">{modelInfo.datasetPath}</p>
                </div>
                <div className="col-span-2 sm:col-span-3">
                  <p className="text-xs text-muted-foreground">Notes</p>
                  <p className="text-xs">{modelInfo.notes}</p>
                </div>
              </CardContent>
            </Card>
          )}
        </>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <History className="h-4 w-4" /> Prediction History
          </CardTitle>
        </CardHeader>
        <CardContent>
          {!history || history.length === 0 ? (
            <EmptyState
              icon={<History className="h-8 w-8" />}
              title="No ML predictions logged yet for this project"
              description="Run risk analysis from the Risk tab with the ML service configured to populate this."
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] text-sm">
                <thead>
                  <tr className="border-b border-border text-left text-xs text-muted-foreground">
                    <th className="px-2 py-2 font-medium">Component</th>
                    <th className="px-2 py-2 font-medium">Rule-based</th>
                    <th className="px-2 py-2 font-medium">ML</th>
                    <th className="px-2 py-2 font-medium">Final</th>
                    <th className="px-2 py-2 font-medium">Confidence</th>
                    <th className="px-2 py-2 font-medium">When</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((h) => (
                    <tr key={h.id} className="border-b border-border last:border-0">
                      <td className="px-2 py-2 font-mono text-xs">{h.componentName}</td>
                      <td className="px-2 py-2 text-xs">{h.ruleBasedRiskLevel}</td>
                      <td className="px-2 py-2 text-xs">{h.mlRiskLevel}</td>
                      <td className={cn('px-2 py-2 text-xs font-semibold')}>{h.finalRiskLevel}</td>
                      <td className="px-2 py-2 tabular-nums text-xs">{Math.round(h.mlConfidence * 100)}%</td>
                      <td className="px-2 py-2 text-xs text-muted-foreground">{formatDate(h.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
