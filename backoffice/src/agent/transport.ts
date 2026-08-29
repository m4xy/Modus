/**
 * The seam between the console UI and whatever is actually producing output.
 *
 * Today the only implementation is `MockStreamTransport`, which replays a canned
 * session with realistic timing. The real one will be an SSE (or WebSocket)
 * client. The console does not know or care which it has: it receives an ordered
 * stream of `StreamEvent`s and holds a `StreamSubscription` it can cancel.
 *
 * Four rules keep the seam honest. Rules 2–4 are measured properties of a real
 * Claude Code transcript, not design preferences; each cites its evidence below
 * rather than citing another document that states the same thing.
 *
 *  1. Events are additive and ordered — never "here is the new full transcript".
 *
 *  2. **A `usage` event reports ONE REQUEST, not a running total.** Usage is
 *     per-request, so a consumer folds it rather than replacing with it. The
 *     same `messageId` may be reported more than once — a partial frame carries
 *     a mid-stream `outputTokens` that a later frame supersedes — so a consumer
 *     MUST deduplicate on `messageId`, keeping the frame with the largest
 *     `outputTokens`, and MUST NOT sum frames of one message.
 *     `keepLargerFrame` is that rule; `foldUsage` applies it.
 *
 *  3. **Cache tokens are first class.** Five kinds are reported, never two, and
 *     they are billed at different rates. A counter that prices only fresh
 *     input and output is not slightly low, it is wrong by orders of magnitude,
 *     because cache reads dominate every agentic loop.
 *
 *  4. **There is no guaranteed terminal event.** Nothing in a Claude Code run
 *     records its own end: a cancelled run's process is killed and emits no
 *     final frame at all. `session-end` is therefore best-effort, and
 *     `session-end{reason:'cancelled'}` is **synthesised by the consumer, by
 *     definition** — a consumer that waits for one before settling hangs
 *     forever. `useAgentSession.cancel()` already owns that transition.
 *
 * Evidence, from the measured corpus rather than from another document:
 *  - `domains/modus/cost/replay/baseline.md` — the committed replay of this
 *    repository's own transcripts, regenerated and re-checked by
 *    `tools/cost-replay.py --check`. Its *Headline* table gives the share of
 *    tokens that are cache reads (rule 3) and its *Self-checks* table reports
 *    both the output recovered by taking the largest frame of a message rather
 *    than the first, and zero disagreements between frames of one message on
 *    the other four token kinds (rule 2). No figure is repeated here: a corpus
 *    number written into merged code is stale by the next session
 *    (`bean:0059`).
 *  - `tools/cost_lib.py` — the reference implementation. `read_messages` is
 *    rule 2, `USAGE_KINDS` and `cost_micros` are rule 3, and
 *    `peak_context_tokens` is `promptTokens` below. The names in this file are
 *    that module's names, deliberately: one concept must not have a Python name
 *    and a different TypeScript one.
 *  - Rule 4 is a property of the transcript format, not of one run. No line
 *    type in a stored session, and no field of a subagent's `*.meta.json`
 *    sidecar, carries an exit code, a status or an end reason; the `<synthetic>`
 *    placeholder messages that do exist are session limits and API errors, and
 *    carry no usage. There is nothing for a consumer to wait for.
 */

export interface PromptRequest {
  domainId: string;
  prompt: string;
  model: string;
  /** Optional work item the run is attributed to, for cost roll-up. */
  workItemKey?: string;
}

/**
 * One request's token usage, split by billing kind.
 *
 * The five kinds and their names are `cost_lib.USAGE_KINDS`, unchanged. They map
 * onto the API `usage` block as `cost_lib._usage_of` maps them: cache creation
 * is split into its 5-minute and 1-hour halves because they are billed at
 * different multipliers, and folding them together mis-prices the dominant term.
 *
 * Money is deliberately absent. Cost is a function of usage and the model that
 * produced it — `costMicros` — not a field a producer can disagree with, and it
 * is integer micro-dollars, never a float (`doc:20-ddd-practices` §3).
 */
export interface Usage {
  inputTokens: number;
  outputTokens: number;
  cacheReadTokens: number;
  cacheWrite5mTokens: number;
  cacheWrite1hTokens: number;
}

export const USAGE_KINDS = [
  'inputTokens',
  'outputTokens',
  'cacheReadTokens',
  'cacheWrite5mTokens',
  'cacheWrite1hTokens',
] as const;

export function zeroUsage(): Usage {
  return {
    inputTokens: 0,
    outputTokens: 0,
    cacheReadTokens: 0,
    cacheWrite5mTokens: 0,
    cacheWrite1hTokens: 0,
  };
}

export function addUsage(acc: Usage, other: Usage): Usage {
  const sum = zeroUsage();
  for (const kind of USAGE_KINDS) sum[kind] = acc[kind] + other[kind];
  return sum;
}

/**
 * Rule 2's selection, for one `messageId`.
 *
 * Frames of one message agree on everything except `outputTokens`, where a
 * partial frame carries a mid-stream count that a later frame supersedes.
 * Keeping the first frame therefore loses output; summing frames double-counts
 * everything else. The authoritative frame is the one with the largest
 * `outputTokens` (`cost_lib.read_messages`).
 */
export function keepLargerFrame(previous: Usage | undefined, frame: Usage): Usage {
  if (previous === undefined) return frame;
  return frame.outputTokens > previous.outputTokens ? frame : previous;
}

/** Rule 2 over a whole run: dedupe by `messageId`, then sum what survives. */
export function foldUsage(byMessage: Readonly<Record<string, Usage>>): Usage {
  return Object.values(byMessage).reduce<Usage>(addUsage, zeroUsage());
}

/**
 * The prompt one request carried — fresh input plus everything read from or
 * written to the cache. `cost_lib.peak_context_tokens` is the maximum of this
 * across a run, and that maximum is the figure `doc:00-constitution` §6's
 * ceiling is about.
 *
 * It is only computable because usage is per-request and carries the cache
 * kinds. Under a cumulative two-field counter there is no single request to take
 * the maximum of, which is why rule 2 and rule 3 are what make peak context
 * observable at all.
 */
export function promptTokens(usage: Usage): number {
  return (
    usage.inputTokens + usage.cacheReadTokens + usage.cacheWrite5mTokens + usage.cacheWrite1hTokens
  );
}

export function peakContextTokens(byMessage: Readonly<Record<string, Usage>>): number {
  return Object.values(byMessage).reduce((peak, usage) => Math.max(peak, promptTokens(usage)), 0);
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
  /**
   * One request's usage (rule 2). `messageId` is the API `message.id` and is the
   * deduplication key: repeats are frames of the same request, not new charges.
   */
  | { type: 'usage'; messageId: string; usage: Usage }
  | { type: 'error'; message: string }
  /** Best-effort (rule 4). `cancelled` is always consumer-synthesised. */
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
   * one when `.close()` is called or the socket drops, and rule 4 says the
   * producer has none to forward either. Consumers must therefore own their own
   * terminal state rather than waiting for `session-end`.
   */
  onClose: () => void;
}

export interface StreamSubscription {
  /**
   * Stop the stream. Implementations are *not* required to emit a terminal
   * event first — `cancel()` may simply close the connection and call
   * `onClose()`. A killed process emits nothing (rule 4), so
   * `useAgentSession.cancel()` moves the UI to `cancelled` itself and no
   * transport has to be polite for the stop button to work.
   */
  cancel: () => void;
}

export interface StreamTransport {
  readonly kind: 'mock' | 'sse' | 'websocket';
  start: (request: PromptRequest, handlers: StreamHandlers) => StreamSubscription;
}

/**
 * Base rates in **micro-dollars per million tokens**, so every figure is an
 * exact integer and `tokens * rate / 1_000_000` is exact integer arithmetic.
 * $5.00/1M is `5_000_000`. This is `cost_lib.BASE_RATES_UPM` under its own name
 * and in its own unit; the two halves must not drift into two vocabularies.
 *
 * Source: `doc:60-cost-model#price-book`, checked 2026-08-29. Only the three
 * models this console offers are listed — an unknown id is a defaulting
 * decision, not a pricing one, and is handled in `ratesUpm`.
 *
 * Two caveats a reader needs:
 *  - Claude Sonnet 5 carries introductory pricing of $2 / $10 per MTok through
 *    2026-08-31; the rate below is the standard one that applies from
 *    2026-09-01. `cost_lib.BASE_RATES_UPM` deliberately carries the *intro*
 *    rate instead, because every run in the replayed corpus predates the lapse
 *    and a spend record computed before it must stay computable. Both are
 *    right for their own effective date; neither is a price book, which is why
 *    `doc:60#price-book` §2.1 puts `effectiveFrom`/`effectiveTo` in the store.
 *  - Pricing is server-side data that drifts, and a constant compiled into the
 *    bundle rots quietly. When the real backend lands (`bean:0014`,
 *    `bean:0020`) the run's cost should be computed server-side against the
 *    stored price book and this table should go.
 */
export const BASE_RATES_UPM = {
  'claude-opus-5': { input: 5_000_000, output: 25_000_000 },
  'claude-sonnet-5': { input: 3_000_000, output: 15_000_000 },
  'claude-haiku-4-5': { input: 1_000_000, output: 5_000_000 },
} as const;

export type PricedModel = keyof typeof BASE_RATES_UPM;

/**
 * Cache multipliers on the base *input* rate: read 0.1x, write 1.25x at the
 * 5-minute TTL, 2x at the 1-hour TTL. `doc:60#price-book` deliberately does not
 * carry them ("they must not be written from memory"); these are the same
 * figures `cost_lib` cites, from the `claude-api` skill's
 * `shared/prompt-caching.md` — "Cache reads cost ~0.1x base input price. Cache
 * writes cost 1.25x for 5-minute TTL, 2x for 1-hour TTL" — re-read at CLI
 * version 2.1.236 on 2026-08-29. Written as integer numerator/denominator pairs
 * so no rate is ever a float.
 */
const CACHE_READ_MULT = [1, 10] as const;
const CACHE_WRITE_5M_MULT = [5, 4] as const;
const CACHE_WRITE_1H_MULT = [2, 1] as const;

function scale(rate: number, [numerator, denominator]: readonly [number, number]): number {
  return Math.floor((rate * numerator) / denominator);
}

/** The five per-million rates in micro-dollars for one model (`cost_lib.rates_upm`). */
export function ratesUpm(model: string): Record<keyof Usage, number> {
  const base = BASE_RATES_UPM[model as PricedModel] ?? BASE_RATES_UPM['claude-sonnet-5'];
  return {
    inputTokens: base.input,
    outputTokens: base.output,
    cacheReadTokens: scale(base.input, CACHE_READ_MULT),
    cacheWrite5mTokens: scale(base.input, CACHE_WRITE_5M_MULT),
    cacheWrite1hTokens: scale(base.input, CACHE_WRITE_1H_MULT),
  };
}

/**
 * Integer micro-dollars for one request's usage. Never a float
 * (`doc:20-ddd-practices` §3), and never a dollar amount: callers divide by
 * 1,000,000 at the render boundary and nowhere else.
 *
 * Priced per message and per token kind, then floored, so a run total is the sum
 * of its message totals rather than the floor of a sum — `cost_lib.cost_micros`
 * makes the same trade and states why. Every product here stays far inside
 * `Number.MAX_SAFE_INTEGER`: the largest rate is 2.5e7 micro-dollars and a
 * single request's largest token kind is bounded by the context window, so the
 * arithmetic is exact. Summing at the message level rather than over a whole
 * corpus is what keeps it that way.
 */
export function costMicros(model: string, usage: Usage): number {
  const rates = ratesUpm(model);
  let total = 0;
  for (const kind of USAGE_KINDS) {
    total += Math.floor((usage[kind] * rates[kind]) / 1_000_000);
  }
  return total;
}
