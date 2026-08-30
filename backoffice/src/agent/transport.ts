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
 *  4. **A per-message status exists; a RUN terminal does not.** Do not confuse
 *     them. Every stored assistant message carries a `stop_reason`, and error
 *     lines carry `apiErrorStatus`/`error` — so a status field is visible on
 *     the wire and an implementer WILL see one. It is not a run terminal: on
 *     most stored runs the final assistant message's `stop_reason` is null, and
 *     `end_turn` arrives on only a minority. A consumer that settles on
 *     `end_turn` therefore hangs on most runs, which is the exact failure this
 *     rule exists to prevent. `session-end` is best-effort, and
 *     `session-end{reason:'cancelled'}` is **synthesised by the consumer, by
 *     definition** — a killed process writes nothing. `useAgentSession.cancel()`
 *     already owns that transition.
 *
 * Evidence, from the measured corpus rather than from another of the three
 * documents that used to state this. Bean references below are provenance, not
 * authority — the authority is the corpus.
 *  - `domains/modus/cost/replay/baseline.md` — the committed replay of this
 *    repository's own transcripts. Its *Headline* table gives the share of
 *    tokens that are cache reads (rule 3) and its *Self-checks* table reports
 *    the output recovered by taking the largest frame of a message rather than
 *    the first, and the number of frames of one message that disagreed on the
 *    other four token kinds (rule 2). It **reports**; nothing asserts — see
 *    `framesDisagree` below for what this file does about that. No figure is
 *    repeated here: a corpus number written into merged code is stale by the
 *    next session (`bean:0059`).
 *  - `python3 tools/cost-replay.py --check` re-checks that baseline, but read
 *    what it checks: it hashes the inputs the baseline recorded and reports
 *    drift if any has changed, which any live session causes by appending a
 *    line. A red `--check` means "the committed figures describe a different
 *    input set", not "the figures were wrong". In a worktree it also needs
 *    `--transcripts DIR`, because the project directory is derived from the
 *    checkout path.
 *  - `tools/cost_lib.py` — the reference implementation. `read_messages` is
 *    rule 2, `USAGE_KINDS` and `cost_micros` are rule 3, and
 *    `peak_context_tokens` is `promptTokens` below. The names in this file are
 *    that module's names, deliberately: one concept must not have a Python name
 *    and a different TypeScript one.
 *  - Rule 4 is a property of the transcript format, not of one run, and it has
 *    two halves that are true for different reasons. Per-message status is
 *    present on every stored assistant line. Run-level terminal state is absent
 *    everywhere: no field of a subagent's `*.meta.json` sidecar carries an exit
 *    code, a status or an end reason, and the last assistant line of a run
 *    usually carries a null `stop_reason` — so even the per-message field does
 *    not stand in for one. `bean:0069` records the counts.
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

/** The four kinds that are byte-identical across frames of one message. */
export const NON_OUTPUT_KINDS = USAGE_KINDS.filter((kind) => kind !== 'outputTokens');

/**
 * Do two frames of one message disagree on something other than `outputTokens`?
 *
 * This is `cost_lib.frame_disagreements`' condition, and it is the premise
 * `keepLargerFrame` rests on. If it is ever true, the frames are independent
 * charges rather than snapshots of one request, and discarding the loser
 * **undercounts the bill** — the selection rule is wrong, not merely unlucky.
 *
 * It is checked here because nothing else checks it anywhere. The Python
 * replay *reports* a count into a table; no `raise`, no non-zero exit, and
 * `cost_lib`'s own module docstring records that no Python runs in
 * `./gradlew qualityCheck` at all. So on the corpus the premise is observed to
 * hold and on the wire it is unverified — which is a fails-open premise under a
 * selection rule that silently drops data. `useAgentSession` surfaces a
 * disagreement as a visible notice rather than swallowing it; that is not a
 * gate, and it does not make the Python side safe, but it means the seam never
 * discards a frame it had reason to doubt without saying so.
 */
export function framesDisagree(a: Usage, b: Usage): boolean {
  return NON_OUTPUT_KINDS.some((kind) => a[kind] !== b[kind]);
}

/**
 * Look one message's usage up, treating **only own properties as present**.
 *
 * `messageId` is producer-controlled wire data (see the `usage` event below), and
 * a plain object literal inherits from `Object.prototype`. So a producer sending
 * `constructor`, `toString`, `valueOf`, `hasOwnProperty` or `__proto__` as a
 * message id gets a *hit* on a map that has never seen that message, and the
 * value is a function rather than a `Usage`.
 *
 * The consequences are silent and they are not cosmetic:
 *  - `framesDisagree` compares `undefined !== 0` on every kind and reports a
 *    disagreement on a stream that contains none.
 *  - `keepLargerFrame` evaluates `n > undefined`, which is `false`, so it keeps
 *    the inherited value, `kept === previous` holds, the map is never updated,
 *    and **the message never enters it at all**. `Object.values` never sees it,
 *    so its tokens are missing from the fold — the run's cost and its peak
 *    context are both understated, with nothing on screen to say so.
 *
 * `Object.create(null)` would fix the initial map and not the invariant: the
 * reducer rebuilds the map by spreading, and a spread produces a fresh object
 * with `Object.prototype` back on the chain. The guard therefore lives at the
 * lookup, which is the one place that can be made unconditionally safe.
 *
 * `isPricedModel` already does this for the model-name namespace, which is
 * producer-controlled in exactly the same way. This function exists because that
 * reasoning was applied there and not here, in the same file.
 */
export function usageOf(
  byMessage: Readonly<Record<string, Usage>>,
  messageId: string,
): Usage | undefined {
  return Object.prototype.hasOwnProperty.call(byMessage, messageId)
    ? byMessage[messageId]
    : undefined;
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
 * Source: `doc:60-cost-model#price-book`, read 2026-08-29. **This is not a price
 * book.** It carries 3 of the 8 models `cost_lib.BASE_RATES_UPM` prices — only
 * the ones this console's model picker offers — and it has no effective dates.
 * The price book proper is `domains/<domainId>/cost/price-book.md`
 * (`doc:60#price-book` §2.1), which does not exist yet.
 *
 * **Sonnet 5's rate is correct as at 2026-08-30 and goes stale silently on
 * 2026-09-01, and nothing will notice.** Read that literally; it is the honest
 * state of this constant, not a caveat. Absolute dates, not "in N days": a
 * relative duration in merged code is wrong the day after it is written.
 *
 *  - The rate below is the introductory $2 / $10 per MTok, in force through
 *    2026-08-31. It matches `cost_lib.BASE_RATES_UPM` for the same model id,
 *    which is the point: the Python and TypeScript halves of one seam must not
 *    disagree about a live rate. The previous value here was the standard
 *    $3 / $15, defended in a comment as "the rate from 2026-09-01" — which
 *    priced Sonnet 5 50% high for every day until then.
 *  - The introductory rate holds **through 2026-08-31**; from **2026-09-01**
 *    the standard $3 / $15 applies. This
 *    table will then be 33% low, and **no gate will go red**. The e2e test that
 *    looks like it covers this prices the mock's tokens from this same table and
 *    then asserts a ratio derived from this same table: it compares the code to
 *    itself. It catches an *internally inconsistent* table — one entry moved
 *    relative to the others — and can never catch a uniformly *stale* one.
 *    Worse, on 2026-09-01 it goes red for whoever correctly sets $3 / $15,
 *    because the assertion is a hardcoded literal. Nothing anywhere compares
 *    these numbers to `doc:60#price-book`; `bean:0090` carries that gap.
 *  - The inconsistent-table case is not hypothetical: `bean:0002` records Opus 5
 *    shipped at $15 / $75 in PR #3 and caught in review cycle 1, and that test
 *    was written as the guard so it cannot recur. Note *caught in review* — the
 *    fix predates the merge, so the wrong value appears in no commit and
 *    `git log -S` finds nothing. That is what a pre-merge fix looks like, and it
 *    is why `.beans/` and not `git log` is where this project's review history
 *    lives (`adr:0005-evidence-lives-in-the-work-item`).
 *  - Effective dating deliberately does NOT live here. Adding it would build a
 *    second, private price book with its own effective windows, which is what
 *    `doc:60#price-book` §2.1 puts in the store and what `bean:0014`/`bean:0020`
 *    exist to replace. When the run's cost is computed server-side against the
 *    stored price book, this table should be deleted rather than dated.
 */
export const BASE_RATES_UPM = {
  'claude-opus-5': { input: 5_000_000, output: 25_000_000 },
  'claude-sonnet-5': { input: 2_000_000, output: 10_000_000 },
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

/**
 * The five per-million rates in micro-dollars for one model.
 *
 * The keys are `cost_lib.rates_upm`'s keys — `input`, `output`, `cacheRead`,
 * `cacheWrite5m`, `cacheWrite1h` — and deliberately NOT the `Usage` field names.
 * Python keeps the rate namespace separate from the count namespace, and it is
 * right to: `cacheReadTokens` is a number of tokens and `cacheRead` is a price,
 * and a `Record<keyof Usage, number>` that holds prices invites adding one to
 * the other. `RATE_OF` below is the only bridge between the two namespaces.
 */
export type Rates = Record<
  'input' | 'output' | 'cacheRead' | 'cacheWrite5m' | 'cacheWrite1h',
  number
>;

export function isPricedModel(model: string): model is PricedModel {
  return Object.prototype.hasOwnProperty.call(BASE_RATES_UPM, model);
}

export function ratesUpm(model: PricedModel): Rates {
  const base = BASE_RATES_UPM[model];
  return {
    input: base.input,
    output: base.output,
    cacheRead: scale(base.input, CACHE_READ_MULT),
    cacheWrite5m: scale(base.input, CACHE_WRITE_5M_MULT),
    cacheWrite1h: scale(base.input, CACHE_WRITE_1H_MULT),
  };
}

/** The one mapping from a token kind to the rate that prices it. */
const RATE_OF: Record<keyof Usage, keyof Rates> = {
  inputTokens: 'input',
  outputTokens: 'output',
  cacheReadTokens: 'cacheRead',
  cacheWrite5mTokens: 'cacheWrite5m',
  cacheWrite1hTokens: 'cacheWrite1h',
};

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
 *
 * **Returns `null` for a model this table does not price.** It does not fall
 * back to a default. `cost_lib.normalise_model` raises on an unpriced id for
 * the same reason — "add it to BASE_RATES_UPM from the price book" — and a
 * silent default is worse than either: an `claude-opus-4-8` run defaulting onto
 * Sonnet 5 would price 60% low with nothing on screen to say so, and it would
 * default onto the one entry whose rate has an expiry. `null` is not zero and
 * must never be rendered as `$0.00`; the console shows no figure at all. This
 * diverges from Python only in mechanism — a reducer running inside React
 * cannot throw without taking the console down mid-stream — never in outcome:
 * neither side ever returns a wrong price.
 */
export function costMicros(model: string, usage: Usage): number | null {
  return isPricedModel(model) ? costMicrosOf(model, usage) : null;
}

/**
 * The same arithmetic for a model the type system already knows is priced.
 *
 * This exists so a caller that has *already* checked `isPricedModel` does not
 * receive a `number | null` it must then dispose of. The only ways to dispose of
 * it are a `?? 0`, which prices an unpriced message as free and is precisely the
 * silent default this seam removed, or a non-null assertion, which is the same
 * thing written more confidently. Narrowing the parameter instead makes the
 * null unrepresentable on that path rather than handled on it.
 */
export function costMicrosOf(model: PricedModel, usage: Usage): number {
  const rates = ratesUpm(model);
  let total = 0;
  for (const kind of USAGE_KINDS) {
    total += Math.floor((usage[kind] * rates[RATE_OF[kind]]) / 1_000_000);
  }
  return total;
}
