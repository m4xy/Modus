---
# modus-0069
title: Correct the agent-run usage vocabulary before bean:0014 publishes it
status: in-progress
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Correct the agent-run usage vocabulary before `bean:0014` publishes it

Why now: `bean:0014` publishes the execution context's language, and
`doc:10-architecture#bounded-contexts` makes published language expensive to change — a
change to a published contract **needs an ADR** (`doc:20-ddd-practices` §4.1.5). It is
currently specified to publish a **wrong** usage model, and the wrong model is written in
three places that cite **each other** rather than any observation:

| where | what it says | its authority |
|---|---|---|
| `backoffice/src/agent/transport.ts` | rule 2 of the seam: "usage is reported as a running cumulative total"; `Usage` is `tokensIn`/`tokensOut`/`costUsd` | none stated |
| `bean:0020` | "Usage reported as a running cumulative total, so a dropped event costs one frame's accuracy" | "the rule `backoffice/src/agent/transport.ts` already states" |
| `bean:0014` | "Token counts are a value object, never a raw `Long`" — silent on shape | none |

A circle of two citations and one silence. `bean:0014` being silent is how the wrong model
gets published by default: the runner implements what the console already assumed.

`doc:00-constitution#evidence-rule` says an assertion needs an observation. The observation
exists and has since `bean:0054`: `tools/cost_lib.py` over this repository's own transcripts,
replayed into `domains/modus/cost/replay/baseline.md` and re-checkable with
`python3 tools/cost-replay.py --check`. It says something different in every particular.

## What the corpus says

**Every figure below comes from ONE scan, and the scan is stamped.** That is not ceremony. An
earlier revision of this table measured the `stop_reason` row roughly 22 minutes before the
rest, while both rows cited "the 82 transcripts present on 2026-08-29". The corpus is **live**
— sessions append to it while it is being read, at roughly 20 lines a minute — so the two rows
described different corpora under one citation. No conclusion moved and every count was
individually correct; the **sourcing** was wrong, in a bean whose whole subject is that a
figure must be traceable to the corpus it claims.

**On a live corpus, "82 transcripts" is not a citation.** A count of files is not a set of
files. Two honest scans minutes apart legitimately disagree without either being wrong, so a
figure is only traceable if the input is identified — and a count cannot identify it. What
identifies it is the stamp below: the instant, the total lines read, and a hash over the file
set.

```
scan started         2026-08-30T09:24:58Z   (finished 09:24:59Z — single pass)
transcript files     91
total lines read     29290
file-set sha256[16]  73394a799c88dad4
meta sidecars        89
```

That file set is a superset of the 65 runs the committed baseline was generated from, and of
the 82 the previous revision of this table read. Figures are evidence of a point in time, not
constants; a re-run will differ in the digits and not in the shape, and nothing here is copied
into merged code (`bean:0059`).

| claim | observed, in the scan stamped above |
|---|---|
| usage is **per-request**, not cumulative | `outputTokens` **falls** from one message to the next on 3,891 of 8,422 consecutive pairs (46.2%). The denominator is *pairs*: a fall is a property of two adjacent messages, and the first message of each run has no predecessor, so pairs = 8,513 messages − 91 runs. That formula has a precondition — no run has zero assistant messages — which holds here, all 91 having at least one. A cumulative counter never decreases even once. |
| frames of one message must be **deduplicated** on `message.id` | 15,413 frames for 8,513 deduplicated messages, multiplicity 1.81x. Frames equal assistant *lines* (15,413), which is the consistency check that the dedupe read everything the files contain. Summing frames multiplies the bill. |
| the frame to keep is the one with the **largest `outputTokens`** | 0 disagreements between frames of one message on the other four kinds. The committed baseline separately reports 1,129,285 output tokens (47.91% of all output) recovered by taking the largest frame rather than the first. **This premise is observed, not asserted** — see the detection gap below. |
| **cache tokens are the bill** | cache reads are 97.73% of all tokens. Fresh input is 16,950 tokens of 1,542,913,629 — 0.00%. Input plus output together are 0.23%. A model pricing only those two prices almost nothing. |
| **peak context is observable** | `cost_lib.peak_context_tokens` = `max(inputTokens + cacheReadTokens + cacheWrite5mTokens + cacheWrite1hTokens)`; largest single request 865,375 tokens, 2.9x `doc:00-constitution#context-budget`'s 300k ceiling. What makes it derivable is the **cache kinds**: successive differences of a cumulative in/out counter would recover per-request input, but no arithmetic recovers a cache-read figure that was never reported, and cache reads are almost the whole prompt. |
| **a per-message status exists; a run terminal does not** | `message.stop_reason` is present on **all 15,413** assistant lines and non-null on 5,167 (`tool_use` 4,882, `end_turn` 247, `stop_sequence` 38); `apiErrorStatus: 429` with `error: "rate_limit"` on 30 lines, `server_error` on 8. A status field IS on the wire. It is not a run terminal: the **last** assistant line of a run carries `stop_reason` null on **64 of 91** runs, `end_turn` on 19, `stop_sequence` on 6, `tool_use` on 2 — so a consumer settling on `end_turn` hangs on 72 of 91. Across all 89 `*.meta.json` sidecars the union of keys is `agentType`, `description`, `inheritedWorktreePath`, `isFork`, `model`, `parentAgentId`, `spawnDepth`, `spawnedWithWorktree`, `toolUseId`, `worktreeBranch`, `worktreeCleanlyRemoved`, `worktreePath` — no exit code, no status, no end reason. A cancelled run's process is killed before writing anything. `session-end{reason:'cancelled'}` is consumer-synthesised **by definition**. |

Every row above comes from one script that walks the file set once, computing the usage
figures through `cost_lib.read_messages`, `frame_disagreements` and `peak_context_tokens` —
the same functions `tools/cost-replay.py` calls — while scanning `message.stop_reason`,
`apiErrorStatus` and the sidecar key sets in the same pass, and emitting the stamp with the
figures so the two cannot drift apart again. `python3 tools/cost-replay.py --check
--transcripts <dir>` independently re-checks the committed baseline.

**Read what `--check` checks before citing it.** It hashes the inputs the baseline recorded
and reports **drift** if any has changed, which any live session causes by appending a line;
it returns before comparing figures at all in that case, and the comparison it would reach is
of file *text*. So a red `--check` says "the committed figures describe a different input
set", not "the figures were wrong". It also needs `--transcripts DIR` in a worktree, because
the project directory is derived from the checkout path. Observed here:
`baseline inputs have moved on: 5 changed, 0 gone.`

**The baseline is deliberately NOT regenerated.** It is `bean:0054`'s merged evidence record;
`adr:0005-evidence-lives-in-the-work-item` puts evidence in the work item, and regenerating it
from a corpus that now contains this very session would make the record describe the act of
measuring it.

## The correction

Five token kinds, `cost_lib.USAGE_KINDS` **verbatim** — `inputTokens`, `outputTokens`,
`cacheReadTokens`, `cacheWrite5mTokens`, `cacheWrite1hTokens`. The 5m and 1h halves stay split
because they are billed at different multipliers (1.25x and 2x of base input; cache reads
0.1x), so folding them mis-prices the dominant term. Two names for one concept across the
Python and TypeScript halves would be a new defect, so there are none: `zeroUsage`,
`addUsage`, `costMicros`, `ratesUpm`, `BASE_RATES_UPM`, `peakContextTokens` are `cost_lib`'s
names.

Money is integer micro-dollars end to end (`doc:20-ddd-practices#value-objects` §3). The
dollar `costUsd: number` that `transport.ts` carried was the floating-point money that rule
forbids; the division by 1,000,000 now happens once, at the render boundary. **The field
keeps the name `costUsd`** — the name `doc:60-cost-model#spend-record` gives it and the name
every record in `domains/modus/cost/0001.ndjson` already carries for the same integer-micros
quantity. A `costUsdMicros` here would have been a *third* name for one concept, which is the
defect this bean exists to remove rather than to commit.

An unpriced model id yields **no price**, never a defaulted one. `cost_lib.normalise_model`
raises on one; `costMicros` returns `null` and the console shows no figure. The mechanism
differs because a reducer inside React cannot throw without taking the console down
mid-stream; the outcome does not: neither side ever returns a wrong price. The previous
`?? BASE_RATES_UPM['claude-sonnet-5']` would have priced a `claude-opus-4-8` run 60% low with
nothing on screen to say so, defaulting onto the one entry whose rate has an expiry.

Each of the three artefacts states this and cites the corpus. **None cites another of the
three as its authority for the usage model** — bean references remain, as provenance, which is
a different thing and is said so in the text.

## Success criteria and evidence

Every row below was re-derived by opening the artefact it cites, not from memory of having
fixed it. That is not diligence theatre: a "no bean" claim in row 1 survived a full review
round *because* it was checked against recollection, which is what made the previous version
of this table unusable as a checklist. Commands run from the repository root; elisions are
marked `[...]` (`bean:0091`).

| # | criterion | evidence |
|---|---|---|
| 1 | `transport.ts` states per-request usage, `messageId` deduplication keeping max `outputTokens`, five token kinds, and a run terminal that is synthesised — citing the corpus, and citing **no other of the three artefacts** as its authority | `cmd`: `grep -n "bean:" backoffice/src/agent/transport.ts` → `51: (bean:0059).` `70: bean:0069 records the counts.` `274: bean:0090 carries` `278: bean:0014/bean:0020`. So the file **does** name beans, and the earlier cell claiming it named "no bean" was false. What is true is narrower and is what the criterion says: none of them is cited as authority for the usage model. The file states this itself at line 41 — `Bean references below are provenance, not authority — the authority is the corpus.` |
| 2 | `bean:0020`'s cumulative criterion is replaced, and its peak-context criterion is made satisfiable rather than claimed closed | `cmd`: `grep -n "cumulative" .beans/modus-0020--claude-code-runner.md` → no output. `cmd`: `grep -n "cache kinds" .beans/modus-0020--claude-code-runner.md` → the criterion now attributes derivability to the cache kinds, not to per-request reporting alone |
| 3 | `bean:0014` is no longer silent: it states the published usage shape, and states the premise's enforcement status honestly | `cmd`: `grep -c "" .beans/modus-0014--execution-bounded-context.md` → the file carries a six-clause `## The usage vocabulary this context publishes` section. `cmd`: `grep -n "observed to hold\|not asserted" .beans/modus-0014--execution-bounded-context.md` → clause 2 reads `It is *observed to hold* on every run of the replayed corpus, and it is **not asserted**.` The previous version of this cell named a filename and nothing else, which asserts that a file exists rather than that a criterion is met |
| 4 | No floating-point money survives in the seam, and no third name for it is introduced | `cmd`: `grep -n "1_000_000" backoffice/src/agent/transport.ts backoffice/src/routes/AgentConsole.tsx` → four hits: `248` (prose in a KDoc), `285` (a rate literal, `input: 1_000_000`), `376` (`costMicros`, integer micros in and integer micros out), `AgentConsole.tsx:157` (the render). So there are **two** arithmetic sites, not one, and the earlier cell saying "the only division is the render" was false. The claim that holds: exactly one site converts **to dollars**, and it is the render. `cmd`: `grep -rn "costUsdMicros" backoffice/src` → no output; the field is `costUsd`, integer micros, the name `doc:60#spend-record` already uses |
| 5 | The console implements the rule it publishes — the reducer folds and dedupes rather than assigning | `cmd`: `grep -n "case 'usage'" -A6 backoffice/src/agent/useAgentSession.ts` → the branch computes `keepLargerFrame`, folds via `foldUsage`, and returns `state` unchanged when the frame is not larger. `[...]` the mock emits a partial frame, a finished frame and a repeated finished frame per request, so the dedupe path runs on every session |
| 6 | The premise `keepLargerFrame` rests on is **detected**, the detector cannot be silenced by producer-chosen input, and it is observed firing, firing exactly once, and not firing on a clean run | `test-run`: `./gradlew e2eTest` → `36 passed (7.6s)`, covering `a usage frame that disagrees on cache tokens is reported, not discarded` (`toHaveCount(1)`), `a tool id colliding with the notice id does not suppress the detector` (`toHaveCount(1)`), and `an ordinary session reports no frame disagreement` (`toHaveCount(0)`). Each was observed failing before its fix — the collision test by reverting only the `block.kind === 'notice'` clause, which gave `expect(locator).toHaveCount(expected) failed` with 0 notices where 1 was expected |
| 7 | Sonnet 5 is priced at the rate actually in force, matching `cost_lib` for the same model id | `cmd`: `grep -n "claude-sonnet-5" backoffice/src/agent/transport.ts tools/cost_lib.py` → `transport.ts:284: 'claude-sonnet-5': { input: 2_000_000, output: 10_000_000 }` and `cost_lib.py:51: "claude-sonnet-5": (2_000_000, 10_000_000)`. Both halves of the seam now agree on a live rate; the previous `$3/$15` priced it 50% high for every day up to the 2026-08-31 lapse |
| 8 | The gate is green | `command`: `./gradlew backofficeTypecheck backofficeLint backofficeFormatCheck` → `BUILD SUCCESSFUL`; `./gradlew docsLint` → `docs-lint: OK — [...] 0 failure(s)`; `./gradlew e2eTest` → `36 passed` |

## A third defect: a claim repeated without checking, and a wrong correction nearly shipped

Two errors here, and the second is worse than the first.

**The first.** `bean:0090` and `e2e/tests/agent-console.spec.ts` both said the ratio test "once
caught" Opus 5 priced at $15/$75. The *mechanism* is wrong: a reviewer caught it, in review
cycle 1 of PR #3, and the test was written afterwards as the regression guard. This bean read
that sentence in a docstring it was editing, carried it forward, and promoted it into
`bean:0090` as the defence of keeping the test — without checking it. A claim already in the
file has passed an implicit review by merely existing, and an author repeating one feels like
they are describing the codebase rather than asserting something.

**The second, which this bean nearly committed.** Checked against `git log`, the claim looks
fabricated — Opus 5 is $5/$25 in all three commits that ever touched `transport.ts`, and
`git log --all -S` finds the value on none of 88 refs. On that basis a correction was written
saying the incident **never happened**, and it was one edit from landing in both artefacts.

It did happen. `.beans/modus-0002--backoffice-foundation.md:130` records it with a before/after
table and pre-computes this very test's response to it. The defect was fixed **before the
merge**, so it appears in no commit — which is what a pre-merge fix looks like, and under
squash-merge is the normal case rather than an edge one.

```
cmd:      grep -rn 'once caught\|15/\$75' backoffice/src e2e/tests .beans
observed: .beans/modus-0002--backoffice-foundation.md:132: `PRICING` claimed $15/$75 per
          MTok for `claude-opus-5`. The list price is [...]
```

**A defect caught in review is invisible to committed history by construction**, which is why
`adr:0005-evidence-lives-in-the-work-item` puts the record in `.beans/`. The search that
"disproved" it was run against the one store guaranteed not to hold it, by two readers
independently, in a repository that cites `.beans/` as its evidence home in every other
paragraph.

So the corrective was going to be a false claim replacing a true one, in a bean about
unverified claims — strictly worse than the original, because the original was wrong only about
*who* caught the error while the correction denied the error existed at all, and it would have
arrived wearing git output as proof.

**How it was caught, which is the transferable part.** It was not caught by re-reading the
correction. It was caught because a *sweep* was run instead of a fix-list: this bean had
already reported the claim as living in two artefacts, and `grep` across the tree found a
**third** (`transport.ts`) — and, on the same pass, `bean:0002`.

That is the general shape, and it is why the fix-list could not have worked: **a re-read is
selected by the same belief it is meant to test.** The list of sites was built by the belief
that there were two, so consulting it would have confirmed the error rather than exposed it.
Only a search whose scope is independent of that belief — the whole tree, not the remembered
subset — can falsify it. The criteria table above encodes the same rule for a different reason
(`bean:0091`): re-derive against the artefact, never against your record of having fixed it. It
turns out to govern a claim's *truth* exactly as it governs its *location*.

The near-miss is recorded deliberately rather than smoothed into a clean fix. The wrong
correction was written, was in both artefacts, and was one edit from being committed — the
sweep is what stopped it. A check that has never changed an outcome is indistinguishable from
ceremony; this one is now known to be load-bearing, and that is worth more here than a tidy
history would be.

## Two defects this bean committed, found by running the gate## Two defects this bean committed, found by running the gate

Recorded rather than quietly fixed, because both are instances of the class this bean is
about and one of them is a class nobody here had named.

**A value meaning "not priced" was also made to mean "nothing yet" — F3 in miniature, on
myself.** `costUsd: null` was intended for "the model is not in `BASE_RATES_UPM`", and was
also returned before any usage had arrived, so an idle console rendered `—` where the honest
answer is `$0.0000`: nothing has been charged, and that is a real zero. Two existing e2e tests
asserted `$0.0000` at idle and failed. **The tests were right and the change was wrong**; they
were left untouched and the code fixed, which is the only order that keeps a suite meaning
anything. Now three explicit states: `0` is a real zero, a number is a priced total, `null` is
usage that cannot be priced. Making one name carry two concepts is exactly the defect this
bean removes from the seam, and it took about an hour to reintroduce.

**A detector whose own retention policy corrupts the state it detects against.** The general
form, because it is not specific to usage frames: *anything that keeps a running "best" value
and compares later values to that retained value, rather than to the source, will report a
single fault repeatedly.* Here, once one frame of a message disagrees, `keepLargerFrame`
retains whichever frame won — possibly the corrupted one — and every subsequent frame of that
message then disagrees **with the retention**, not with the data. One planted fault produced
N−1 notices, and the count was an artefact of the mechanism rather than a property of the
stream. Fixed by reporting once per `messageId`. Any deduplicating detector that keeps a
representative has this shape.

Two things about how it surfaced are worth keeping:

- It failed as a **strict-mode violation** — Playwright resolving two elements where one was
  expected — not as an absence. The detector worked; there was *too much* of it. A weaker
  assertion (`toBeVisible` alone) would have passed and shipped the defect, which is why the
  test now asserts `toHaveCount(1)` and pins the count rather than the presence.
- The evidence deliberately carries **three** observations, not one: the detector fires on a
  planted violation, fires **exactly once**, and does **not** fire on a clean run.
  `doc:00-constitution#observed-failing` currently requires only the first. But a mechanism
  that fires on every run has also been "observed firing" and is worthless, and one that fires
  N times for one fault is worse than useless because the count lies. The negative observation
  is not a courtesy; it is half the evidence.

## Two claims checked and **not** encoded

`doc:00-constitution#evidence-rule` cuts both ways, so both were checked before being written
down anywhere, and both turned out weaker than asserted.

**"An earlier spike claimed peak context was unobservable."** Not found. Nothing in
`documentation/`, `.beans/`, `backoffice/`, `tools/` or `domains/` asserts it; the committed
baseline's own "What these transcripts cannot tell you" table already lists
`peakContextTokens` as **obtainable**. There is no such claim to correct, so none was
corrected. What *is* true, and is now stated, is that observability depends on rules 2 and 3 —
the cumulative two-field counter this bean removes is what would have made it unobservable.

**"This closes `bean:0020`'s peak-context criterion and a constitution `Enforcement gap:`."**
It closes neither, and claiming otherwise would be the failure this bean exists to prevent.
`doc:00-constitution` §6.2's gap reads "the recorder is not yet implemented" and closes when
the recorder exists — `bean:0014` and `bean:0020`'s work, not a specification correction's.
`bean:0020`'s criterion is likewise still open. What changed is that the criterion was
**unsatisfiable** against the old specification and is now satisfiable: peak context cannot be
derived from a cumulative `tokensIn`/`tokensOut` pair, so a runner built to that spec could not
have met it. That document is also owned by another agent this sprint, so no edit to it was
made from here.

## What is detected, and what still is not

Stated plainly so nobody reads criterion 6 as more than it is.

`framesDisagree` covers the **TypeScript consumer only**. It is a **detector, not a gate**: it
reports into the transcript and does not fail a build. The producing side — `tools/cost_lib.py`
and `tools/cost-replay.py`, which generate the committed baseline every figure above comes
from — remains **unasserted**.

```
cmd:      grep -n "frame_disagreements\|frameDisagreements" tools/cost-replay.py
observed: 109:            "frameDisagreements": C.frame_disagreements(messages),
          345:    frame_disagreements = 0
          350:        frame_disagreements += r["frameDisagreements"]
          398:        "frameDisagreements": frame_disagreements,
          504:        "| frames disagreeing on input or cache tokens | %d |" % s[...]
          514:        "            | %d disagreement(s)" % s["frameDisagreements"]
          673:    w("| repeated frames of one `message.id` agree on input and cache [...]
```

**Seven occurrences, of which three render** — 504, 514 and 673. The other four accumulate the
count and carry it through the summary structure. (An earlier revision of this paragraph said
"all six uses render a table row", which was wrong in both numbers and would have told a reader
checking it that the paragraph had not been checked. The conclusion is unaffected: what matters
is that **none** of the seven raises.) There is no `raise` and no non-zero exit; `--check`
returns on input drift before reaching any comparison, and compares file text when it does; and
`./gradlew qualityCheck` runs **no Python at all** (`cost_lib.py`'s own module docstring records
that gap). So the premise is observed to hold on this corpus and enforced nowhere. An
implementation of `bean:0014` must not treat it as guaranteed by tooling.

## Findings for others, not acted on here

- **`tools/cost_lib.py` asserts, in prose, the opposite of the specification this bean
  publishes — in the file a reader would open to check it.** Two docstrings claim the
  enforcement that the section above establishes does not exist:

  ```
  cmd:      sed -n '168,171p;249,255p' tools/cost_lib.py
  observed: 170:    `cache_creation_input_tokens` are byte-identical across every frame of a
            171:    message; the replay asserts that on every run and reports the count, [...]
            254:    undercounts. Asserted on every run, never assumed.
  ```

  Neither is true: nothing raises, and no Python runs in `qualityCheck`. This is the most
  load-bearing instance of the wrong claim left in the tree, because `cost_lib.py` is the
  reference implementation `bean:0014`, `bean:0020` and `transport.ts` all point a reader at —
  so a reader who follows the citation to check the premise finds it *reassured* by the very
  file whose silence is the gap. `tools/cost_lib.py` is owned by another agent this sprint and
  is read, never edited, from here. Two docstring lines; the fix is smaller than this note.

- **Nothing compares a constant in code to the authority it must match.** Raised as
  `bean:0090`; the rate table is one instance of it.
- **`doc:60-cost-model#spend-record` §3.2 names four token kinds** — `inputTokens`,
  `outputTokens`, `cacheWriteTokens`, `cacheReadTokens` — and §3.2.1 says "all four token
  kinds". `tools/cost_lib.py` and every record already written to
  `domains/modus/cost/0001.ndjson` carry **five**, splitting `cacheWriteTokens` into
  `cacheWrite5mTokens` and `cacheWrite1hTokens` because the two are billed at different
  multipliers; the ndjson carries the folded `cacheWriteTokens` alongside them. The normative
  document is behind its own implementation. `documentation/` is owned by another agent this
  sprint, so this is reported rather than fixed.
- **`bean:0016`** will consume this vocabulary when it attributes spend, and
  **`bean:0021`** carries it over the wire. Neither is edited here.
