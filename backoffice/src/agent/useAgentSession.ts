import { useCallback, useEffect, useRef, useState } from 'react';
import {
  costMicros,
  foldUsage,
  framesDisagree,
  isPricedModel,
  keepLargerFrame,
  peakContextTokens,
  zeroUsage,
} from './transport';
import type {
  PromptRequest,
  StreamEvent,
  StreamSubscription,
  StreamTransport,
  Usage,
} from './transport';

export type TranscriptBlock =
  | { kind: 'prompt'; id: string; text: string }
  | { kind: 'assistant'; id: string; text: string; done: boolean }
  | {
      kind: 'tool';
      id: string;
      name: string;
      input: string;
      status: 'running' | 'ok' | 'failed';
      summary?: string;
      detail?: string;
      durationMs?: number;
    }
  | { kind: 'notice'; id: string; tone: 'error' | 'info'; text: string };

export type SessionStatus = 'idle' | 'streaming' | 'complete' | 'cancelled' | 'error';

export interface AgentSessionState {
  status: SessionStatus;
  blocks: TranscriptBlock[];
  /**
   * Every request's usage, keyed by `message.id`. Keeping the map rather than a
   * running total is what makes the transport's rule 2 expressible: a repeated
   * `messageId` replaces its entry instead of being added to it, and the peak
   * context of the run is a maximum over these entries — neither is recoverable
   * from a scalar that has already been summed.
   */
  usageByMessage: Record<string, Usage>;
  /** Folded from `usageByMessage`; never accumulated from the wire. */
  usage: Usage;
  /**
   * **Integer micro-dollars**, the unit `doc:60-cost-model#spend-record` gives
   * `costUsd` and the unit every record in `domains/<domainId>/cost/*.ndjson`
   * already stores. The name is that field's name on purpose: a `costUsdMicros`
   * here would be a third name for one concept, next to `costUsd`-as-micros in
   * the store and `costUsd`-as-float-dollars in `api/types.ts`. Dollars are
   * produced once, at the render boundary, and stored nowhere.
   *
   * `null` means **not priced**, never zero — usage arrived for a model
   * `BASE_RATES_UPM` does not carry, and no figure is shown rather than a wrong
   * one. A real zero (nothing charged yet) stays `0`; see `totalCostUsd`.
   */
  costUsd: number | null;
  /** `max(promptTokens)` over the run — `doc:00-constitution` §6's figure. */
  peakContextTokens: number;
  sessionId: string | null;
  model: string | null;
}

/**
 * Sum of each message's own cost, as `cost_lib` does — not the cost of the sum.
 *
 * Three states, and conflating any two of them is a defect:
 *  - **0** — nothing has been charged. No usage has arrived, so the run has cost
 *    nothing so far. True before a run starts, and it is a real zero.
 *  - **a total** — usage has arrived and the model is priced.
 *  - **`null`** — usage has arrived and the model is **not** in `BASE_RATES_UPM`,
 *    so what was charged cannot be priced. Never rendered as `$0.00`: reporting
 *    an unpriced run as a free one is the silent default this seam removed.
 */
function totalCostUsd(model: string | null, byMessage: Record<string, Usage>): number | null {
  const usages = Object.values(byMessage);
  if (usages.length === 0) return 0;
  if (model === null || !isPricedModel(model)) return null;
  return usages.reduce((total, usage) => total + (costMicros(model, usage) ?? 0), 0);
}

/**
 * Resolve every tool block still marked `running`.
 *
 * A session can end four ways — completed, cancelled, an `error` event, or a
 * transport failure — and a tool that was in flight must stop spinning in all
 * four. Shared so the terminal paths cannot drift apart: a spinner that never
 * resolves is indistinguishable from work still happening.
 */
function resolveRunningTools(blocks: TranscriptBlock[], summary: string): TranscriptBlock[] {
  return blocks.map((block) =>
    block.kind === 'tool' && block.status === 'running'
      ? { ...block, status: 'failed', summary }
      : block,
  );
}

function reduce(state: AgentSessionState, event: StreamEvent): AgentSessionState {
  switch (event.type) {
    case 'session-start':
      return {
        ...state,
        status: 'streaming',
        sessionId: event.sessionId,
        model: event.model,
        // Cost is a function of usage and the model that produced it, so
        // learning the model late re-prices what has already arrived rather
        // than leaving it priced against the fallback.
        costUsd: totalCostUsd(event.model, state.usageByMessage),
      };

    case 'assistant-delta': {
      const last = state.blocks[state.blocks.length - 1];
      // Deltas append to the open assistant block; a closed one starts a new turn.
      if (last?.kind === 'assistant' && !last.done) {
        const blocks = state.blocks.slice(0, -1);
        blocks.push({ ...last, text: last.text + event.text });
        return { ...state, blocks };
      }
      return {
        ...state,
        blocks: [
          ...state.blocks,
          {
            kind: 'assistant',
            id: `a${state.blocks.length}`,
            text: event.text,
            done: false,
          },
        ],
      };
    }

    case 'assistant-end': {
      const last = state.blocks[state.blocks.length - 1];
      if (last?.kind !== 'assistant') return state;
      const blocks = state.blocks.slice(0, -1);
      blocks.push({ ...last, done: true });
      return { ...state, blocks };
    }

    case 'tool-call':
      return {
        ...state,
        blocks: [
          ...state.blocks,
          {
            kind: 'tool',
            id: event.callId,
            name: event.name,
            input: event.input,
            status: 'running',
          },
        ],
      };

    case 'tool-result':
      return {
        ...state,
        blocks: state.blocks.map((block) =>
          block.kind === 'tool' && block.id === event.callId
            ? {
                ...block,
                status: event.ok ? 'ok' : 'failed',
                summary: event.summary,
                ...(event.detail !== undefined ? { detail: event.detail } : {}),
                durationMs: event.durationMs,
              }
            : block,
        ),
      };

    case 'usage': {
      // Rule 2 of the transport seam. A `usage` event is ONE REQUEST, so it is
      // folded, not assigned; and a repeated `messageId` is another frame of a
      // request already counted, so it replaces its entry — keeping the frame
      // with the largest `outputTokens` — rather than adding to the total.
      // Assigning here, as this reducer used to, silently required the producer
      // to send cumulative totals, which no real producer does.
      const previous = state.usageByMessage[event.messageId];

      // The premise the selection rests on, checked rather than assumed. Frames
      // of one message must agree on the four non-output kinds; if they do not,
      // they are independent charges and discarding the loser undercounts the
      // bill. Checked BEFORE the no-op return below, or a disagreeing frame that
      // happened to carry the smaller `outputTokens` would be dropped in
      // silence. Nothing else checks this anywhere — see `framesDisagree`.
      //
      // Reported once per MESSAGE, not once per frame. A message that disagreed
      // keeps whichever frame was retained, so every later frame of it disagrees
      // with that one too and would raise the same notice again. The id is
      // derived from `messageId` so the check is the presence of that block.
      //
      // `block.kind === 'notice'` is load-bearing, not tidiness. Block ids share
      // one namespace across kinds, and a tool block takes its id from the
      // producer's `callId` — external input once the transport is a real SSE
      // client rather than the mock. Without the kind check, a `callId` equal to
      // `disagreement-<messageId>` suppresses a genuine disagreement and the
      // detector reports nothing. A detector that a producer can silence by
      // choosing an id fails open, which is the one way this must not fail.
      const noticeId = `disagreement-${event.messageId}`;
      const disagreed =
        previous !== undefined &&
        framesDisagree(previous, event.usage) &&
        !state.blocks.some((block) => block.kind === 'notice' && block.id === noticeId);

      const kept = keepLargerFrame(previous, event.usage);
      const usageByMessage =
        kept === previous
          ? state.usageByMessage
          : { ...state.usageByMessage, [event.messageId]: kept };
      if (!disagreed && usageByMessage === state.usageByMessage) return state;

      return {
        ...state,
        blocks: disagreed
          ? [
              ...state.blocks,
              {
                kind: 'notice',
                id: noticeId,
                tone: 'error',
                text: `Usage frames for ${event.messageId} disagree on input or cache tokens. They are not snapshots of one request, so the cost below is understated.`,
              },
            ]
          : state.blocks,
        usageByMessage,
        usage: foldUsage(usageByMessage),
        costUsd: totalCostUsd(state.model, usageByMessage),
        peakContextTokens: peakContextTokens(usageByMessage),
      };
    }

    case 'error':
      // Terminal: a stream that dies mid tool call never sends `session-end`,
      // because the server never got the chance to.
      return {
        ...state,
        status: 'error',
        blocks: [
          ...resolveRunningTools(state.blocks, 'Interrupted'),
          {
            kind: 'notice',
            id: `e${state.blocks.length}`,
            tone: 'error',
            text: event.message,
          },
        ],
      };

    case 'session-end':
      return {
        ...state,
        status:
          event.reason === 'complete'
            ? 'complete'
            : event.reason === 'cancelled'
              ? 'cancelled'
              : 'error',
        blocks: resolveRunningTools(state.blocks, 'Interrupted'),
      };

    default:
      return state;
  }
}

const INITIAL: AgentSessionState = {
  status: 'idle',
  blocks: [],
  usageByMessage: {},
  usage: zeroUsage(),
  costUsd: 0,
  peakContextTokens: 0,
  sessionId: null,
  model: null,
};

/**
 * Owns the transcript and the running cost for one console session. It talks to
 * a `StreamTransport` and nothing else, so swapping the mock for SSE changes
 * exactly one argument at the call site.
 */
export function useAgentSession(transport: StreamTransport) {
  const [state, setState] = useState<AgentSessionState>(INITIAL);
  const subscription = useRef<StreamSubscription | null>(null);

  useEffect(() => () => subscription.current?.cancel(), []);

  const send = useCallback(
    (request: PromptRequest) => {
      subscription.current?.cancel();
      setState({
        ...INITIAL,
        status: 'streaming',
        blocks: [{ kind: 'prompt', id: 'p0', text: request.prompt }],
      });

      subscription.current = transport.start(request, {
        onEvent: (event) => setState((current) => reduce(current, event)),
        onError: (error) =>
          setState((current) => reduce(current, { type: 'error', message: error.message })),
        onClose: () => {
          subscription.current = null;
        },
      });
    },
    [transport],
  );

  /**
   * Stopping is our state change, not the transport's. `StreamSubscription`
   * only promises to stop the stream — a real `EventSource` closes the socket
   * and emits nothing — so the console moves itself to `cancelled` and resolves
   * in-flight tools rather than waiting for a courtesy `session-end`.
   */
  const cancel = useCallback(() => {
    subscription.current?.cancel();
    subscription.current = null;
    setState((current) =>
      current.status === 'streaming'
        ? {
            ...current,
            status: 'cancelled',
            blocks: resolveRunningTools(current.blocks, 'Interrupted'),
          }
        : current,
    );
  }, []);

  const reset = useCallback(() => {
    subscription.current?.cancel();
    subscription.current = null;
    setState(INITIAL);
  }, []);

  return { ...state, send, cancel, reset };
}
