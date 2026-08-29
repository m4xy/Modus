/**
 * The seam between the console UI and whatever is actually producing output.
 *
 * Today the only implementation is `MockStreamTransport`, which replays a canned
 * session with realistic timing. The real one will be an SSE (or WebSocket)
 * client. The console does not know or care which it has: it receives an ordered
 * stream of `StreamEvent`s and holds a `StreamSubscription` it can cancel.
 *
 * Two rules keep the seam honest:
 *  1. Events are additive and ordered — never "here is the new full transcript".
 *  2. Usage is reported as a running cumulative total, so a dropped event costs
 *     accuracy for one frame rather than corrupting the counter permanently.
 */

export interface PromptRequest {
    domainId: string;
    prompt: string;
    model: string;
    /** Optional work item the run is attributed to, for cost roll-up. */
    workItemKey?: string;
}

export interface Usage {
    tokensIn: number;
    tokensOut: number;
    costUsd: number;
}

export type StreamEvent =
    | { type: 'session-start'; sessionId: string; model: string; startedAt: string }
    | { type: 'assistant-delta'; text: string }
    | { type: 'assistant-end' }
    | { type: 'tool-call'; callId: string; name: string; input: string }
    | {
          type: 'tool-result';
          callId: string;
          ok: boolean;
          summary: string;
          detail?: string;
          durationMs: number;
      }
    /** Cumulative totals for the session so far. */
    | { type: 'usage'; usage: Usage }
    | { type: 'error'; message: string }
    | { type: 'session-end'; reason: 'complete' | 'cancelled' | 'error' };

export interface StreamHandlers {
    onEvent: (event: StreamEvent) => void;
    /**
     * A transport-level failure — the connection died, not the run. The console
     * treats this exactly like an `error` event: terminal, with any in-flight
     * tool blocks resolved.
     */
    onError: (error: Error) => void;
    /**
     * The stream is finished and will emit nothing further. It may fire *without*
     * a preceding `session-end`: a real `EventSource` has no way to synthesise
     * one when `.close()` is called or the socket drops. Consumers must therefore
     * own their own terminal state rather than waiting for `session-end`.
     */
    onClose: () => void;
}

export interface StreamSubscription {
    /**
     * Stop the stream. Implementations are *not* required to emit a terminal
     * event first — `cancel()` may simply close the connection and call
     * `onClose()`. `useAgentSession.cancel()` moves the UI to `cancelled` itself
     * so no transport has to be polite for the stop button to work.
     */
    cancel: () => void;
}

export interface StreamTransport {
    readonly kind: 'mock' | 'sse' | 'websocket';
    start: (request: PromptRequest, handlers: StreamHandlers) => StreamSubscription;
}

/**
 * Anthropic list pricing in US dollars per million tokens, checked against the
 * published model pricing on 2026-08-28. Used by the mock so the cost counter
 * is plausible rather than decorative.
 *
 * Two caveats a reader needs:
 *  - Claude Sonnet 5 carries introductory pricing of $2 / $10 per MTok through
 *    2026-08-31. The list price below is what applies from 2026-09-01, so the
 *    counter reads slightly high for the last few days of the introductory
 *    window rather than silently halving the day it lapses.
 *  - Pricing is server-side data that drifts, and a constant compiled into the
 *    bundle rots quietly. When the real backend lands (0003) the `usage` event
 *    should carry `costUsd` computed server-side and this table should go.
 */
export const PRICING = {
    'claude-opus-5': { inputPerMTok: 5, outputPerMTok: 25 },
    'claude-sonnet-5': { inputPerMTok: 3, outputPerMTok: 15 },
    'claude-haiku-4-5': { inputPerMTok: 1, outputPerMTok: 5 },
} as const;

export type PricedModel = keyof typeof PRICING;

export function costOf(model: string, tokensIn: number, tokensOut: number): number {
    const pricing = PRICING[model as PricedModel] ?? PRICING['claude-sonnet-5'];
    return (tokensIn * pricing.inputPerMTok + tokensOut * pricing.outputPerMTok) / 1_000_000;
}
