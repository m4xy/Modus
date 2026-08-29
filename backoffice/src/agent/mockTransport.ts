import { costOf } from './transport';
import type {
    PromptRequest,
    StreamEvent,
    StreamHandlers,
    StreamSubscription,
    StreamTransport,
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
 */
export type MockFault = 'none' | 'stream-error' | 'transport-error';

export function faultFromLocation(search: string = window.location.search): MockFault {
    const raw = new URLSearchParams(search).get('fault');
    return raw === 'stream-error' || raw === 'transport-error' ? raw : 'none';
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
                summary: '96 lines · StreamTransport, StreamEvent, PRICING',
                detail: 'export interface StreamTransport {\n  readonly kind: "mock" | "sse" | "websocket";\n  start: (request, handlers) => StreamSubscription;\n}',
                ok: true,
                workMs: 620,
            },
        },
        {
            say: 'The event union already carries a cumulative `usage` event, so the SSE client can forward server totals untouched. Checking whether anything else constructs transports directly.\n\n',
            tool: {
                name: 'Grep',
                input: 'MockStreamTransport --glob "backoffice/src/**/*.tsx"',
                summary: '1 match in src/routes/AgentConsole.tsx',
                detail: 'src/routes/AgentConsole.tsx:24:  const transport = useMemo(() => new MockStreamTransport(), []);',
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
                detail: 'export class SseStreamTransport implements StreamTransport {\n  readonly kind = "sse";\n  start(request, handlers) { /* EventSource + backoff */ }\n}',
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
                detail: "src/agent/sseTransport.ts(41,7): error TS2412: Type 'string | undefined' is not assignable to type 'string' under exactOptionalPropertyTypes.",
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
                    if (step.event.type === 'session-end' || step.event.type === 'error')
                        handlers.onClose();
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

    private buildScript(request: PromptRequest): ScriptStep[] {
        const steps: ScriptStep[] = [];
        // The prompt plus a system preamble and the repository map is the input cost
        // you actually pay on turn one — it is not free, so we show it.
        let tokensIn = 4_820 + tokensFor(request.prompt);
        let tokensOut = 0;

        const usageEvent = (): StreamEvent => ({
            type: 'usage',
            usage: {
                tokensIn,
                tokensOut,
                costUsd: costOf(request.model, tokensIn, tokensOut),
            },
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
        steps.push({ after: 40, event: usageEvent() });

        let callIndex = 0;
        for (const turn of cannedTurns(request.prompt)) {
            for (const piece of chunk(turn.say)) {
                tokensOut += tokensFor(piece);
                steps.push({
                    after: 18 + Math.round(piece.length * 4),
                    event: { type: 'assistant-delta', text: piece },
                });
            }
            steps.push({ after: 60, event: { type: 'assistant-end' } });
            steps.push({ after: 20, event: usageEvent() });

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
                // Tool output is re-read on the next turn: that is where input cost grows.
                tokensIn += tokensFor(turn.tool.detail) + 320;
                steps.push({ after: 30, event: usageEvent() });
            }
        }

        steps.push({ after: 260, event: { type: 'session-end', reason: 'complete' } });
        return steps;
    }
}
