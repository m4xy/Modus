import { useEffect, useMemo, useRef, useState } from 'react';
import { useDomain } from '../app/DomainContext';
import { useAgentRuns } from '../api/queries';
import {
  MockStreamTransport,
  faultFromLocation,
  replaySpeedFromLocation,
} from '../agent/mockTransport';
import { useAgentSession } from '../agent/useAgentSession';
import type { TranscriptBlock } from '../agent/useAgentSession';
import { PageHeader } from '../components/PageHeader';
import {
  Badge,
  Button,
  Card,
  CardBody,
  CardHeader,
  EmptyState,
  ErrorState,
  Select,
  SkeletonList,
  Table,
  Td,
  Textarea,
  Th,
  useToast,
} from '../ui';
import type { BadgeTone } from '../ui';
import {
  formatDateTime,
  formatDuration,
  formatTokens,
  formatUsd,
  formatUsdPrecise,
} from '../lib/format';
import styles from './AgentConsole.module.css';

const models = [
  { value: 'claude-opus-5', label: 'Opus 5 — deep work' },
  { value: 'claude-sonnet-5', label: 'Sonnet 5 — balanced' },
  { value: 'claude-haiku-4-5', label: 'Haiku 4.5 — cheap and fast' },
];

const statusTone: Record<string, BadgeTone> = {
  idle: 'neutral',
  streaming: 'accent',
  complete: 'good',
  cancelled: 'neutral',
  error: 'danger',
  running: 'accent',
  succeeded: 'good',
  failed: 'danger',
  queued: 'info',
};

const statusText: Record<string, string> = {
  idle: 'Idle',
  streaming: 'Streaming',
  complete: 'Complete',
  cancelled: 'Cancelled',
  error: 'Error',
};

function ToolBlock({ block }: { block: Extract<TranscriptBlock, { kind: 'tool' }> }) {
  const tone: BadgeTone =
    block.status === 'ok' ? 'good' : block.status === 'failed' ? 'danger' : 'accent';
  return (
    <div className={styles.tool}>
      <div className={styles.toolHead}>
        <span className={styles.toolName}>{block.name}</span>
        <span className={styles.toolInput}>{block.input}</span>
        {block.durationMs !== undefined && (
          <span className={styles.toolDuration}>{formatDuration(block.durationMs)}</span>
        )}
        <Badge tone={tone} dot>
          {block.status === 'running' ? 'running' : block.status}
        </Badge>
      </div>
      <div className={styles.toolBody}>
        {block.summary ?? 'Waiting for the tool to return…'}
        {block.detail && (
          <details>
            <summary className={styles.toolSummary}>Output</summary>
            <pre className={styles.toolDetail}>{block.detail}</pre>
          </details>
        )}
      </div>
    </div>
  );
}

export function AgentConsole() {
  const { domain, can } = useDomain();
  const runs = useAgentRuns(domain.id);
  const { notify } = useToast();

  // The seam: one construction site. Swapping in SseStreamTransport changes
  // this line and nothing else in the console.
  const transport = useMemo(
    () => new MockStreamTransport(replaySpeedFromLocation(), faultFromLocation()),
    [],
  );
  const session = useAgentSession(transport);

  const [prompt, setPrompt] = useState(
    'Replace the mock stream transport with a real SSE client, keeping the console untouched.',
  );
  const [model, setModel] = useState('claude-opus-5');
  const transcriptRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = transcriptRef.current;
    if (element) element.scrollTop = element.scrollHeight;
  }, [session.blocks]);

  const streaming = session.status === 'streaming';
  const canRun = can('agents.run');

  const run = () => {
    if (!prompt.trim()) {
      notify({
        tone: 'warning',
        title: 'Write a prompt first',
        body: 'An empty run costs money and returns nothing.',
      });
      return;
    }
    session.send({ domainId: domain.id, prompt, model });
  };

  return (
    <>
      <PageHeader
        eyebrow={`${domain.id} · agents`}
        title="Agent console"
        description="Start a Claude Code session against this domain and watch it work. Output streams in as it happens; the cost counter moves with it."
        actions={
          <Badge tone={statusTone[session.status] ?? 'neutral'} dot>
            {statusText[session.status] ?? session.status}
          </Badge>
        }
      />

      <div className={styles.console}>
        <div className={styles.meters} data-testid="agent-meters">
          <div className={styles.meter}>
            <span className={styles.meterLabel}>Session cost</span>
            <span
              className={`${styles.meterValue} ${styles.costValue}`}
              data-testid="agent-cost"
              aria-live="off"
            >
              {/*
                Micros are the stored unit; dollars exist only for this render.
                `null` is not zero — it means the model is not in
                `BASE_RATES_UPM`, and showing "$0.0000" for an unpriced run
                would be the silent-default this seam removed.
              */}
              {session.costUsd === null ? '—' : formatUsdPrecise(session.costUsd / 1_000_000)}
            </span>
            <span className={styles.meterNote}>{model.replace('claude-', '')}</span>
          </div>
          {/*
            Cache reads, not fresh input, are where an agentic run's tokens go —
            the meter that used to sit here showed "tokens in" and so showed
            almost none of the spend.
          */}
          <div className={styles.meter}>
            <span className={styles.meterLabel}>Cache read</span>
            <span className={styles.meterValue} data-testid="agent-cache-read">
              {formatTokens(session.usage.cacheReadTokens)}
            </span>
            <span className={styles.meterNote}>
              {formatTokens(session.usage.inputTokens)} fresh in
            </span>
          </div>
          <div className={styles.meter}>
            <span className={styles.meterLabel}>Tokens out</span>
            <span className={styles.meterValue} data-testid="agent-tokens-out">
              {formatTokens(session.usage.outputTokens)}
            </span>
            <span className={styles.meterNote}>generated</span>
          </div>
          <div className={styles.meter}>
            <span className={styles.meterLabel}>Peak context</span>
            <span className={styles.meterValue} data-testid="agent-peak-context">
              {formatTokens(session.peakContextTokens)}
            </span>
            <span className={styles.meterNote}>
              largest single request · {session.sessionId ?? 'not started'}
            </span>
          </div>
        </div>

        <div className={styles.panel}>
          <div className={styles.panelHead}>
            <span className={styles.panelTitle}>
              Transcript
              <Badge tone={statusTone[session.status] ?? 'neutral'} dot>
                {statusText[session.status] ?? session.status}
              </Badge>
            </span>
            <Button size="sm" variant="ghost" onClick={session.reset} disabled={streaming}>
              Clear
            </Button>
          </div>

          <div
            className={styles.transcript}
            ref={transcriptRef}
            data-testid="agent-transcript"
            role="log"
            aria-label="Agent output"
            aria-live="polite"
            tabIndex={0}
          >
            {session.blocks.length === 0 ? (
              <EmptyState
                title="Nothing running"
                description="Describe what you want done in this domain. The agent reads the domain's memories and repositories before it starts."
              />
            ) : (
              session.blocks.map((block) => (
                <div className={styles.block} key={block.id}>
                  <span
                    className={`${styles.gutter} ${block.kind === 'prompt' ? styles.gutterYou : ''}`}
                  >
                    {block.kind === 'prompt'
                      ? 'You'
                      : block.kind === 'assistant'
                        ? 'Claude'
                        : block.kind === 'tool'
                          ? 'Tool'
                          : 'Error'}
                  </span>
                  <div>
                    {block.kind === 'prompt' && <p className={styles.promptText}>{block.text}</p>}
                    {block.kind === 'assistant' && (
                      <p className={styles.assistantText}>
                        {block.text}
                        {!block.done && streaming && (
                          <span className={styles.caret} aria-hidden="true" />
                        )}
                      </p>
                    )}
                    {block.kind === 'tool' && <ToolBlock block={block} />}
                    {block.kind === 'notice' && <p className={styles.notice}>{block.text}</p>}
                  </div>
                </div>
              ))
            )}
          </div>

          <div className={styles.composer}>
            <Textarea
              label="Prompt"
              hideLabel
              rows={3}
              value={prompt}
              placeholder="What should the agent do in this domain?"
              data-testid="agent-prompt"
              disabled={streaming}
              onChange={(event) => setPrompt(event.target.value)}
            />
            <div className={styles.composerRow}>
              <div className={styles.composerField}>
                <Select
                  label="Model"
                  options={models}
                  value={model}
                  disabled={streaming}
                  onChange={(event) => setModel(event.target.value)}
                />
              </div>
              <span className={styles.hint}>
                {canRun ? 'Runs are billed to this domain' : 'You cannot start runs in this domain'}
              </span>
              <div className={styles.composerActions}>
                {streaming ? (
                  <Button variant="danger" onClick={session.cancel} data-testid="agent-stop">
                    Stop
                  </Button>
                ) : (
                  <Button
                    variant="primary"
                    onClick={run}
                    disabled={!canRun}
                    data-testid="agent-run"
                  >
                    Run session
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>

        <Card>
          <CardHeader
            eyebrow="History"
            title="Recent runs"
            description="Every run in this domain, with what it cost."
          />
          <CardBody flush>
            {runs.isError ? (
              <ErrorState
                title="Run history could not be loaded"
                description="The request for this domain's runs failed, so this is not a report that no runs happened. Reload to try again."
              />
            ) : runs.isPending ? (
              <div style={{ padding: 'var(--space-5)' }}>
                <SkeletonList rows={3} label="Loading run history" />
              </div>
            ) : (runs.data ?? []).length === 0 ? (
              <EmptyState
                title="No runs recorded"
                description="Once a session finishes, it lands here with its duration, model and spend."
              />
            ) : (
              <Table caption={`Recent agent runs in ${domain.name}`}>
                <thead>
                  <tr>
                    <Th>Run</Th>
                    <Th>Trigger</Th>
                    <Th>Model</Th>
                    <Th>Status</Th>
                    <Th numeric>Tokens</Th>
                    <Th numeric>Cost</Th>
                    <Th numeric>Started</Th>
                  </tr>
                </thead>
                <tbody>
                  {/*
                    PRE-CORRECTION SHAPE, on the same screen as the corrected one.
                    The meters above show a live run priced from five integer-micros
                    token kinds; these rows show past runs from `api.AgentRun`, whose
                    `tokensIn`/`tokensOut`/`costUsd` are two token kinds and
                    floating-point dollars — the model `bean:0069` removed from the
                    stream seam. It is left standing deliberately, not overlooked:
                    `AgentRun` is the `execution` context's own aggregate and
                    `bean:0014` defines its published shape, so correcting it here
                    would pre-empt that bean and drag the REST DTOs, the mock
                    fixtures and the charts into a change that is about the stream.
                    Until then these two blocks disagree about what a run costs, and
                    the rows are the ones that are wrong.
                  */}
                  {(runs.data ?? []).map((item) => (
                    <tr key={item.id}>
                      <Td mono>{item.id}</Td>
                      <Td mono>{item.trigger}</Td>
                      <Td mono>{item.model.replace('claude-', '')}</Td>
                      <Td>
                        <Badge tone={statusTone[item.status] ?? 'neutral'} dot>
                          {item.status}
                        </Badge>
                      </Td>
                      <Td numeric mono>
                        {formatTokens(item.tokensIn)} / {formatTokens(item.tokensOut)}
                      </Td>
                      <Td numeric>{formatUsd(item.costUsd)}</Td>
                      <Td numeric mono>
                        {formatDateTime(item.startedAt)}
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )}
          </CardBody>
        </Card>
      </div>
    </>
  );
}
