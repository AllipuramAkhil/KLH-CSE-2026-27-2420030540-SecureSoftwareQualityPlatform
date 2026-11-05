import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { Sparkles, Wand2, Lightbulb, Bug, ShieldAlert } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import { Textarea } from '@/components/ui/Textarea';
import { Badge } from '@/components/ui/Badge';
import { Tabs } from '@/components/ui/Tabs';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { aiApi } from '@/api/aiApi';
import { useToast } from '@/components/ui/Toast';
import type { AiAnalysisResponse } from '@/types/ai';
import type { ApiError } from '@/api/axios';

type Section = 'analyze' | 'generate' | 'defect' | 'vulnerability' | 'recommendations';

const SECTIONS = [
  { key: 'analyze' as Section, label: 'Project Analysis', icon: <Sparkles className="h-3.5 w-3.5" /> },
  { key: 'generate' as Section, label: 'Generate Tests', icon: <Wand2 className="h-3.5 w-3.5" /> },
  { key: 'defect' as Section, label: 'Explain Defect', icon: <Bug className="h-3.5 w-3.5" /> },
  { key: 'vulnerability' as Section, label: 'Explain Vulnerability', icon: <ShieldAlert className="h-3.5 w-3.5" /> },
  { key: 'recommendations' as Section, label: 'Recommendations', icon: <Lightbulb className="h-3.5 w-3.5" /> },
];

export default function AiAnalysis() {
  const { id } = useParams();
  const projectId = Number(id);
  const { show } = useToast();
  const [section, setSection] = useState<Section>('analyze');

  return (
    <div className="space-y-4">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold">
          <Sparkles className="h-5 w-5 text-primary" /> AI Analysis Workspace
        </h2>
        <p className="text-sm text-muted-foreground">
          AI-assisted insights with automatic rule-based fallback when no AI provider is configured
        </p>
      </div>

      <Tabs items={SECTIONS.map((s) => ({ key: s.key, label: s.label, icon: s.icon }))} active={section} onChange={(k) => setSection(k as Section)} />

      {section === 'analyze' && <AnalyzeSection projectId={projectId} onError={(m) => show(m, 'error')} />}
      {section === 'generate' && <GenerateTestsSection projectId={projectId} onError={(m) => show(m, 'error')} />}
      {section === 'defect' && <ExplainDefectSection projectId={projectId} onError={(m) => show(m, 'error')} />}
      {section === 'vulnerability' && <ExplainVulnerabilitySection projectId={projectId} onError={(m) => show(m, 'error')} />}
      {section === 'recommendations' && <RecommendationsSection projectId={projectId} onError={(m) => show(m, 'error')} />}
    </div>
  );
}

function AiResponseCard({ response }: { response: AiAnalysisResponse }) {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>Result</CardTitle>
          <Badge variant="outline" className={response.aiGenerated ? 'border-primary/40 text-primary' : 'border-warning/40 text-warning'}>
            {response.aiGenerated ? 'AI-generated' : 'Rule-based fallback'} · {response.source}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-foreground">{response.summary}</p>
        {response.insights.length > 0 && (
          <div>
            <p className="mb-2 text-xs font-semibold uppercase text-muted-foreground">Insights</p>
            <ul className="space-y-2">
              {response.insights.map((insight, i) => (
                <li key={i} className="rounded-md border border-border bg-muted/30 p-3 text-sm">
                  {insight}
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function AnalyzeSection({ projectId, onError }: { projectId: number; onError: (m: string) => void }) {
  const [context, setContext] = useState('');
  const mutation = useMutation({
    mutationFn: () => aiApi.analyze(projectId, { analysisType: 'GENERAL', context }),
    onError: (err) => onError((err as unknown as ApiError).message),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Run project analysis</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="context">Additional context (optional)</Label>
          <Textarea id="context" value={context} onChange={(e) => setContext(e.target.value)} placeholder="Focus on payment-related components…" />
        </div>
        <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending}>
          <Sparkles className="h-4 w-4" /> Analyze Project
        </Button>
        {mutation.isPending && <LoadingSpinner label="Generating analysis…" />}
        {mutation.data && <AiResponseCard response={mutation.data} />}
      </CardContent>
    </Card>
  );
}

function GenerateTestsSection({ projectId, onError }: { projectId: number; onError: (m: string) => void }) {
  const [componentName, setComponentName] = useState('');
  const [snippet, setSnippet] = useState('');
  const [count, setCount] = useState(3);
  const mutation = useMutation({
    mutationFn: () => aiApi.generateTests(projectId, { componentName, sourceCodeSnippet: snippet, numberOfTests: count }),
    onError: (err) => onError((err as unknown as ApiError).message),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Generate test cases</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="componentName">Component name</Label>
          <Input id="componentName" value={componentName} onChange={(e) => setComponentName(e.target.value)} placeholder="PaymentService" />
        </div>
        <div>
          <Label htmlFor="snippet">Source code snippet (optional)</Label>
          <Textarea id="snippet" rows={5} value={snippet} onChange={(e) => setSnippet(e.target.value)} className="font-mono text-xs" />
        </div>
        <div>
          <Label htmlFor="count">Number of tests</Label>
          <Input id="count" type="number" min={1} max={10} value={count} onChange={(e) => setCount(Number(e.target.value))} className="w-24" />
        </div>
        <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending}>
          <Wand2 className="h-4 w-4" /> Generate Tests
        </Button>
        {mutation.data && (
          <Card>
            <CardContent className="pt-4">
              <ul className="space-y-2">
                {mutation.data.map((title, i) => (
                  <li key={i} className="flex items-start gap-2 rounded-md border border-border bg-muted/30 p-3 text-sm">
                    <span className="font-mono text-xs text-muted-foreground">{i + 1}.</span> {title}
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        )}
      </CardContent>
    </Card>
  );
}

function ExplainDefectSection({ projectId, onError }: { projectId: number; onError: (m: string) => void }) {
  const [qualityAnalysisId, setQualityAnalysisId] = useState('');
  const mutation = useMutation({
    mutationFn: () => aiApi.explainDefect(projectId, Number(qualityAnalysisId)),
    onError: (err) => onError((err as unknown as ApiError).message),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Explain a quality defect</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="qaId">Quality analysis ID</Label>
          <Input id="qaId" type="number" value={qualityAnalysisId} onChange={(e) => setQualityAnalysisId(e.target.value)} placeholder="e.g. 12" />
        </div>
        <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending} disabled={!qualityAnalysisId}>
          <Bug className="h-4 w-4" /> Explain Defect
        </Button>
        {mutation.data && <AiResponseCard response={mutation.data} />}
      </CardContent>
    </Card>
  );
}

function ExplainVulnerabilitySection({ projectId, onError }: { projectId: number; onError: (m: string) => void }) {
  const [vulnerabilityId, setVulnerabilityId] = useState('');
  const mutation = useMutation({
    mutationFn: () => aiApi.explainVulnerability(projectId, Number(vulnerabilityId)),
    onError: (err) => onError((err as unknown as ApiError).message),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Explain a vulnerability</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="vulnId">Vulnerability ID</Label>
          <Input id="vulnId" type="number" value={vulnerabilityId} onChange={(e) => setVulnerabilityId(e.target.value)} placeholder="e.g. 8" />
        </div>
        <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending} disabled={!vulnerabilityId}>
          <ShieldAlert className="h-4 w-4" /> Explain Vulnerability
        </Button>
        {mutation.data && <AiResponseCard response={mutation.data} />}
      </CardContent>
    </Card>
  );
}

function RecommendationsSection({ projectId, onError }: { projectId: number; onError: (m: string) => void }) {
  const mutation = useMutation({
    mutationFn: () => aiApi.recommendations(projectId),
    onError: (err) => onError((err as unknown as ApiError).message),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Risk-based recommendations</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <Button onClick={() => mutation.mutate()} isLoading={mutation.isPending}>
          <Lightbulb className="h-4 w-4" /> Get Recommendations
        </Button>
        {mutation.data && <AiResponseCard response={mutation.data} />}
      </CardContent>
    </Card>
  );
}
