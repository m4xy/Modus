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
  onError: (error: Error) => void;
  onClose: () => void;
}

export interface StreamSubscription {
  cancel: () => void;
}

export interface StreamTransport {
  readonly kind: 'mock' | 'sse' | 'websocket';
  start: (request: PromptRequest, handlers: StreamHandlers) => StreamSubscription;
}

/** Opus 5 list pricing, per million tokens. Used by the mock to stay plausible. */
export const PRICING = {
  'claude-opus-5': { inputPerMTok: 15, outputPerMTok: 75 },
  'claude-sonnet-4-5': { inputPerMTok: 3, outputPerMTok: 15 },
  'claude-haiku-4-5': { inputPerMTok: 1, outputPerMTok: 5 },
} as const;

export type PricedModel = keyof typeof PRICING;

export function costOf(model: string, tokensIn: number, tokensOut: number): number {
  const pricing = PRICING[model as PricedModel] ?? PRICING['claude-sonnet-4-5'];
  return (tokensIn * pricing.inputPerMTok + tokensOut * pricing.outputPerMTok) / 1_000_000;
}
