import { useCallback, useEffect, useRef, useState } from 'react';
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
    usage: Usage;
    sessionId: string | null;
    model: string | null;
}

const EMPTY_USAGE: Usage = { tokensIn: 0, tokensOut: 0, costUsd: 0 };

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

        case 'usage':
            return { ...state, usage: event.usage };

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
    usage: EMPTY_USAGE,
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
                    setState((current) =>
                        reduce(current, { type: 'error', message: error.message }),
                    ),
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
