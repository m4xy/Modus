import type {
  PromptRequest,
  StreamEvent,
  StreamHandlers,
  StreamSubscription,
  StreamTransport,
  Usage,
} from './transport';

interface ScriptStep {
  /** Delay before this event, in milliseconds. */
  after: number;
  event: StreamEvent;
}

interface Turn {
  say: string;
  tool?: {
    name: string;
    input: string;
    summary: string;
    detail: string;
    ok: boolean;
    workMs: number;
  };
}

/**
 * Replay pacing is adjustable from the URL (`?replay=0.05`) so a demo can be
 * slowed down and the e2e suite can run the same session in a second.
 */
export function replaySpeedFromLocation(search: string = window.location.search): number {
  const raw = new URLSearchParams(search).get('replay');
  if (raw === null) return 1;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 1;
}

/**
 * Failure shapes a real SSE client produces and the happy-path replay never
 * would. Selected from the URL (`?fault=stream-error`) so the e2e suite can
 * drive the console's terminal paths without a second transport.
 *
 *  - `stream-error`     the server reports a failure mid tool call and stops.
 *                       There is no `session-end`: the run died before one
 *                       could be sent.
 *  - `transport-error`  the connection itself drops mid tool call, surfacing
 *                       through `onError` rather than as a stream event.
 *  - `usage-disagreement`
 *                       two frames of one `messageId` disagree on a cache kind.
 *                       This does not happen in the measured corpus, and that is
 *                       the point: `keepLargerFrame` discards the losing frame
 *                       on the premise that it cannot happen, and a premise no
 *                       test has ever falsified is a premise nobody has watched
 *                       hold (`doc:00-constitution#observed-failing`). This
 *                       fault is how the detector is seen to fire.
 */
export type MockFault = 'none' | 'stream-error' | 'transport-error' | 'usage-disagreement';

export function faultFromLocation(search: string = window.location.search): MockFault {
  const raw = new URLSearchParams(search).get('fault');
  return raw === 'stream-error' || raw === 'transport-error' || raw === 'usage-disagreement'
    ? raw
    : 'none';
}

/** Roughly four characters to a token — good enough for a plausible counter. */
const tokensFor = (text: string) => Math.max(1, Math.round(text.length / 4));

function chunk(text: string): string[] {
  // Split on word boundaries so the transcript types like a person reading it.
  return text.match(/\S+\s*/g) ?? [text];
}

function cannedTurns(prompt: string): Turn[] {
  return [
    {
      say: `Reading the transport seam before touching anything. You asked: “${prompt.trim()}”\n\nThe console already consumes a \`StreamTransport\`, so this is an implementation swap rather than a rewrite.\n\n`,
      tool: {
        name: 'Read',
        input: 'backoffice/src/agent/transport.ts',
        summary: '286 lines · StreamTransport, StreamEvent, BASE_RATES_UPM',
        detail:
          'export interface StreamTransport {\n  readonly kind: "mock" | "sse" | "websocket";\n  start: (request, handlers) => StreamSubscription;\n}',
        ok: true,
        workMs: 620,
      },
    },
    {
      say: 'The event union carries a per-request `usage` event keyed by `messageId`, so the SSE client forwards each frame untouched and the console does the deduplication. Checking whether anything else constructs transports directly.\n\n',
      tool: {
        name: 'Grep',
        input: 'MockStreamTransport --glob "backoffice/src/**/*.tsx"',
        summary: '1 match in src/routes/AgentConsole.tsx',
        detail:
          'src/routes/AgentConsole.tsx:24:  const transport = useMemo(() => new MockStreamTransport(), []);',
        ok: true,
        workMs: 480,
      },
    },
    {
      say: 'One construction site, exactly as intended. I will add `SseStreamTransport` alongside the mock and select it from an environment flag, so the console keeps its single `useMemo`.\n\n',
      tool: {
        name: 'Edit',
        input: 'backoffice/src/agent/sseTransport.ts',
        summary: 'created · 74 lines',
        detail:
          'export class SseStreamTransport implements StreamTransport {\n  readonly kind = "sse";\n  start(request, handlers) { /* EventSource + backoff */ }\n}',
        ok: true,
        workMs: 1400,
      },
    },
    {
      say: 'Running the type checker before I claim anything works.\n\n',
      tool: {
        name: 'Bash',
        input: 'npm run typecheck',
        summary: 'exit 1 · 1 error',
        detail:
          "src/agent/sseTransport.ts(41,7): error TS2412: Type 'string | undefined' is not assignable to type 'string' under exactOptionalPropertyTypes.",
        ok: false,
        workMs: 2600,
      },
    },
    {
      say: '`exactOptionalPropertyTypes` caught a genuine one: the reconnect path can resume without a last event id. Making the field explicitly optional rather than widening the type.\n\n',
      tool: {
        name: 'Bash',
        input: 'npm run typecheck',
        summary: 'exit 0 · clean',
        detail: '> tsc --noEmit -p tsconfig.json && tsc --noEmit -p ../e2e/tsconfig.json',
        ok: true,
        workMs: 2200,
      },
    },
    {
      say: 'Green. The seam held — the console file is untouched, and the mock still drives the e2e suite so tests stay deterministic. Next step is wiring reconnect backoff to the run status badge.',
    },
  ];
}

/**
 * Replays a canned Claude Code session with realistic pacing: bursts of typed
 * text, pauses while a tool "runs", and a usage counter that climbs the whole
 * time. It exists so the console can be built, demoed and tested before the
 * server exists — and so the e2e suite has a deterministic stream to assert on.
 */
export class MockStreamTransport implements StreamTransport {
  readonly kind = 'mock' as const;

  /** Multiplier on every delay: tests pass a small value to speed the replay up. */
  private readonly speed: number;

  private readonly fault: MockFault;

  constructor(speed = 1, fault: MockFault = 'none') {
    this.speed = speed;
    this.fault = fault;
  }

  start(request: PromptRequest, handlers: StreamHandlers): StreamSubscription {
    const script = this.faulted(this.buildScript(request));
    const timers: number[] = [];
    let cancelled = false;
    let elapsed = 0;

    for (const step of script) {
      elapsed += step.after * this.speed;
      timers.push(
        window.setTimeout(() => {
          if (cancelled) return;
          handlers.onEvent(step.event);
          // Both are terminal. `error` closes without a `session-end` because
          // that is what a server that just fell over actually does.
          if (step.event.type === 'session-end' || step.event.type === 'error') handlers.onClose();
        }, elapsed),
      );
    }

    if (this.fault === 'transport-error') {
      elapsed += 400 * this.speed;
      timers.push(
        window.setTimeout(() => {
          if (cancelled) return;
          handlers.onError(new Error('The connection to the agent service dropped.'));
          handlers.onClose();
        }, elapsed),
      );
    }

    return {
      cancel: () => {
        // Deliberately no synthetic `session-end`: this is exactly what a real
        // `EventSource.close()` does. The console owns the cancelled state.
        if (cancelled) return;
        cancelled = true;
        for (const timer of timers) window.clearTimeout(timer);
        handlers.onClose();
      },
    };
  }

  /** Truncates the happy path at the first tool call and injects the failure. */
  private faulted(script: ScriptStep[]): ScriptStep[] {
    if (this.fault === 'none') return script;

    if (this.fault === 'usage-disagreement') return this.withDisagreeingFrame(script);

    const firstToolCall = script.findIndex((step) => step.event.type === 'tool-call');
    const upToTheToolCall = script.slice(0, firstToolCall + 1);

    if (this.fault === 'transport-error') return upToTheToolCall;
    return [
      ...upToTheToolCall,
      {
        after: 400,
        event: { type: 'error', message: 'The model stream dropped mid tool call.' },
      },
    ];
  }

  /**
   * Corrupt one repeated frame so it disagrees with its predecessor on a cache
   * kind, leaving everything else intact.
   *
   * The run still completes; only the premise is broken. `cacheReadTokens` is
   * changed rather than `outputTokens` precisely because `outputTokens` is
   * *expected* to differ between frames — that is what the selection rule is
   * for. A cache kind differing means the two frames are not the same request,
   * and the consumer must say so rather than quietly keeping one.
   */
  private withDisagreeingFrame(script: ScriptStep[]): ScriptStep[] {
    let seen = 0;
    return script.map((step) => {
      if (step.event.type !== 'usage') return step;
      // The second frame of the first message: the first one established the
      // entry, so this is the earliest point a disagreement can be detected.
      if (++seen !== 2) return step;
      return {
        ...step,
        event: {
          ...step.event,
          usage: { ...step.event.usage, cacheReadTokens: step.event.usage.cacheReadTokens + 4_096 },
        },
      };
    });
  }

  /**
   * One turn is one REQUEST, and the whole conversation so far is re-sent on it.
   *
   * That re-send is why cache reads dominate real spend: everything already in
   * the prompt is read back from cache on every subsequent request, while only
   * the newly-appended tail is written. The script models that rather than a
   * single growing `tokensIn`, because a counter with two fields cannot show
   * where the money goes — see the transport's rules 2 and 3.
   */
  private buildScript(request: PromptRequest): ScriptStep[] {
    const steps: ScriptStep[] = [];
    // The system preamble, the repository map and the prompt — the prefix that
    // request one writes to cache and every later request reads back.
    let prefixTokens = 4_820 + tokensFor(request.prompt);
    let appended = prefixTokens;
    let cachedPrefix = 0;
    let messageIndex = 0;

    let messageId = '';
    let usage: Usage = {
      inputTokens: 0,
      outputTokens: 0,
      cacheReadTokens: 0,
      cacheWrite5mTokens: 0,
      cacheWrite1hTokens: 0,
    };

    /** Start a new request: read the cached prefix back, write the new tail. */
    const beginRequest = (): void => {
      messageId = `msg_${(++messageIndex).toString().padStart(2, '0')}`;
      usage = {
        // A few tokens are never cacheable — the turn's own framing.
        inputTokens: 12,
        outputTokens: 0,
        cacheReadTokens: cachedPrefix,
        cacheWrite5mTokens: appended,
        cacheWrite1hTokens: 0,
      };
      cachedPrefix = prefixTokens;
      appended = 0;
    };

    /**
     * A usage frame for the request in flight. The same `messageId` is reported
     * more than once — a partial frame mid-generation, the finished one at
     * `assistant-end`, and the finished one again alongside a tool block, which
     * is the frame multiplicity a stored transcript actually has. A consumer
     * that summed these would multiply the bill; one that kept the first would
     * lose most of the output. Deduplicating on `messageId` and keeping the
     * largest `outputTokens` is the only reading that gets both right.
     */
    const usageEvent = (): StreamEvent => ({
      type: 'usage',
      messageId,
      usage: { ...usage },
    });

    steps.push({
      after: 220,
      event: {
        type: 'session-start',
        sessionId: `sess_${Math.random().toString(36).slice(2, 10)}`,
        model: request.model,
        startedAt: new Date().toISOString(),
      },
    });

    let callIndex = 0;
    for (const turn of cannedTurns(request.prompt)) {
      beginRequest();
      steps.push({ after: 40, event: usageEvent() });

      const pieces = chunk(turn.say);
      const partialAt = Math.floor(pieces.length / 2);
      pieces.forEach((piece, index) => {
        usage.outputTokens += tokensFor(piece);
        steps.push({
          after: 18 + Math.round(piece.length * 4),
          event: { type: 'assistant-delta', text: piece },
        });
        // The partial frame: a real transcript writes one mid-generation, with
        // an output count a later frame supersedes.
        if (index === partialAt) steps.push({ after: 10, event: usageEvent() });
      });
      steps.push({ after: 60, event: { type: 'assistant-end' } });
      steps.push({ after: 20, event: usageEvent() });

      prefixTokens += usage.outputTokens;
      appended += usage.outputTokens;

      if (turn.tool) {
        const callId = `call_${++callIndex}`;
        steps.push({
          after: 180,
          event: {
            type: 'tool-call',
            callId,
            name: turn.tool.name,
            input: turn.tool.input,
          },
        });
        // The duplicate frame the transcript writes per content block: identical
        // to the one above, and a no-op for a consumer that dedupes correctly.
        steps.push({ after: 10, event: usageEvent() });
        steps.push({
          after: turn.tool.workMs,
          event: {
            type: 'tool-result',
            callId,
            ok: turn.tool.ok,
            summary: turn.tool.summary,
            detail: turn.tool.detail,
            durationMs: turn.tool.workMs,
          },
        });
        // Tool output joins the prompt, so the NEXT request writes it to cache
        // and every request after that reads it back.
        const grew = tokensFor(turn.tool.detail) + 320;
        prefixTokens += grew;
        appended += grew;
      }
    }

    steps.push({ after: 260, event: { type: 'session-end', reason: 'complete' } });
    return steps;
  }
}
