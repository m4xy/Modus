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
meta sidecars        89
```

**The hash that stood here has been removed, and the reason matters more than the figure.** It
read `file-set sha256[16] 73394a799c88dad4`, and it appeared in exactly one place in the
repository: this line, asserting itself. Nothing computed it that a reader could re-run, and —
decisively — **nothing a reader could run would ever reproduce it**, because the corpus it
hashes lives in `~/.claude` on one machine and grows continuously. A different reader has a
different file set by construction. A hash whose input no one else can hold is not a checksum;
it is a number that looks like one, which is worse than no number because it invites the trust
a checksum earns.

So what remains is stated as what it is: **an author's attestation**, not a check. The instant
and the line count say when the scan ran and how much it read, and a reader may believe or
disbelieve them. That is honest, and it is all this kind of figure can be.

The contrast worth drawing is `bean:0054`'s baseline, which *is* checkable and shows what the
difference costs: it records **each input file's path and hash individually**, in the
repository, and `tools/cost-replay.py --check` re-hashes those paths and reports drift. That
works because the paths are named, so the check can go and look. Reproducing that property here
would mean committing 91 paths and hashes for a corpus that is not the subject of this bean —
`bean:0054` already owns it, and duplicating it would create a second record to disagree with
the first.

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

**Every `cmd` below runs, and its `observed:` is that command's output.** That is an assertion,
not a label: run them and they reproduce, and if one does not, this table is wrong and should
be treated as wrong. It is checked mechanically rather than promised — see *The claim is
executable* below. No commit is named here on purpose: an earlier revision pinned the claim to
`fdfce90`, which every subsequent amend falsified while the claim stayed true.

The previous revision carried a *label* instead — "every row was re-derived by opening the
artefact it cites, not from memory of having fixed it" — and it was false in four of eight
rows. It survived a review round with the label attached. **A claimed-re-derived table and a
re-derived one are indistinguishable from outside**, so the label bought nothing and cost a
reviewer the work of running all eight commands to discover it. What is written above is
falsifiable by the reader in the time it takes to paste a line, which a claim about my
diligence is not.

Two conventions follow from what went wrong, and both are load-bearing:

- **Line numbers are not cited.** Four cells carried them and this pull request's own later
  edits moved every one. A line number is a claim about a file's current shape, and any edit
  above it falsifies the claim without touching what it describes.
- **Absence is not asserted where a positive claim will do.** An `observed: no output` block
  is falsified by its own record the moment the searched string appears in the searched
  corpus — which happened **twice** in this change, to `git log -S "outputPerMTok: 75"` in
  `bean:0090` and to `grep -rn "costUsdMicros"` here. A positive assertion cannot fail that
  way, so each cell below asserts what *is* there.

Elisions are marked `[...]` (`bean:0091`).

| # | criterion | evidence |
|---|---|---|
| 1 | `transport.ts` states per-request usage, `messageId` deduplication keeping max `outputTokens`, five token kinds, and a run terminal that is synthesised — citing the corpus, and citing **no other of the three artefacts** as its authority | `cmd`: `grep -n "bean:" backoffice/src/agent/transport.ts` -> five hits: `(bean:0059)`, `bean:0069 records the counts.`, `` `bean:0090` carries that gap.``, `` `bean:0002` records Opus 5``, `` `bean:0014`/`bean:0020` ``. So the file **does** name beans — an earlier cell claimed it named none, and a later one listed four of the five with two line numbers that no longer existed. What is true is narrower and is what the criterion says: none is cited as authority for the usage model. `cmd`: `grep -n "provenance, not" backoffice/src/agent/transport.ts` -> `Bean references below are provenance, not` (the sentence wraps; the grep is written to survive that) |
| 2 | `bean:0020`'s cumulative criterion is replaced, and its peak-context criterion attributes derivability to the cache kinds | `cmd`: `grep -n "cumulative" .beans/modus-0020--claude-code-runner.md` -> **two** hits, not none as an earlier cell claimed: `why the earlier "running cumulative total" criterion here` and `successive differences of a cumulative`. Both are the *replacement* describing what it replaced, which is the criterion being met, not violated — but the cell asserting no output was simply false. `cmd`: `grep -n "successive differences" .beans/modus-0020--claude-code-runner.md` -> `successive differences of a cumulative`. An earlier cell grepped `"cache kinds"` and reported output; that phrase is wrapped as `**cache` / `kinds**` across two lines and **cannot** match — output was pasted for a command that returns nothing |
| 3 | `bean:0014` is no longer silent: it states the published usage shape, and states the premise's enforcement status honestly | `cmd`: `grep -c "^[0-9]\. \*\*" .beans/modus-0014--execution-bounded-context.md` -> `6`, the six numbered clauses. An earlier cell used `grep -c ""`, a line count, which cannot show a section exists. `cmd`: `grep -n "observed to hold" .beans/modus-0014--execution-bounded-context.md` -> `It is *observed to hold* on every run of the replayed corpus, and it is **not` |
| 4 | No floating-point money survives in the seam, and the field carries the name `doc:60#spend-record` already uses | `cmd`: `grep -n "1_000_000" backoffice/src/agent/transport.ts backoffice/src/routes/AgentConsole.tsx` -> four hits: KDoc prose, a rate literal `input: 1_000_000`, `costMicros` dividing integer micros by integer micros, and `AgentConsole.tsx` dividing by 1,000,000. Two arithmetic sites; exactly one converts **to dollars**, and it is the render. `cmd`: `grep -n "costUsd: number" backoffice/src/agent/useAgentSession.ts` -> one hit, the state field declaration, integer micros. (Written without a pipe deliberately: a `|` inside a Markdown table cell must be escaped as `\|`, so any command containing one is corrupt the moment a reader pastes it. The escaped form here matched eleven lines instead of one, because `\|` is alternation in a basic regular expression.) Asserted positively: an earlier cell claimed `grep -rn "costUsdMicros"` gave no output, and by then it gave one — the KDoc explaining why that name is *not* used had created the match |
| 5 | The console implements the rule it publishes — the reducer folds and dedupes rather than assigning | `cmd`: `grep -n "keepLargerFrame(previous" backoffice/src/agent/useAgentSession.ts` -> the `usage` branch selecting the authoritative frame. `cmd`: `grep -n "foldUsage(usageByMessage)" backoffice/src/agent/useAgentSession.ts` -> the fold that replaces the old assignment. `cmd`: `grep -n "usageByMessage === state.usageByMessage" backoffice/src/agent/useAgentSession.ts` -> the early return that leaves state untouched when the frame is not larger. Three single-pattern commands rather than one alternation: an earlier cell used a `-A6` window that ended before two of the three, and the alternation that replaced it could not be pasted at all |
| 6 | The premise `keepLargerFrame` rests on is **detected**; the detector cannot be silenced by producer-chosen input; and every message reaches the fold whatever its id | `test-run`: `./gradlew e2eTest` -> `37 passed`. Firing: `a usage frame that disagrees on cache tokens is reported, not discarded` (`toHaveCount(1)`). Not silenceable: `a tool id colliding with the notice id does not suppress the detector` (`toHaveCount(1)`). Not firing spuriously: `an ordinary session reports no frame disagreement` (`toHaveCount(0)`) and `a message id naming an inherited property is counted, not silently dropped`, which also asserts the run costs the same as the identical clean run. Each was observed failing before its fix. **Scope, because an earlier version of this cell overclaimed:** "does not fire on a clean run" was evidenced only by the mock's own `msg_NN` ids, which establishes it for one id shape and not for wire input — the gap the inherited-property defect lived in |
| 7 | Sonnet 5 is priced at the rate in force, matching `cost_lib` for the same model id | `cmd`: `grep -n "claude-sonnet-5" backoffice/src/agent/transport.ts tools/cost_lib.py` -> `'claude-sonnet-5': { input: 2_000_000, output: 10_000_000 },` and `"claude-sonnet-5": (2_000_000, 10_000_000),`. Both halves of the seam agree on a live rate; the earlier `$3/$15` priced it 50% high on every day the introductory rate held, which is every day through 2026-08-31 |
| 8 | The gate is green | `command`: `./gradlew backofficeTypecheck backofficeLint backofficeFormatCheck` -> `BUILD SUCCESSFUL`; `./gradlew docsLint` -> `docs-lint: OK [...] 0 failure(s)`; `./gradlew e2eTest` -> `37 passed` |

### The claim is executable, which is why it is now made

The header above asserts that every `cmd` was run. That assertion is worth something only
because it was **mechanised**: a 28-line script extracts every `` `cmd`: `...` `` from this
table, runs each in the repository root, and reports any that produce no output or that cannot
be pasted at all.

It caught two defects **reading would not have**, and both are invisible by construction
because the fault is in how the cell *renders* the command rather than in the command's logic:

- `grep -n "keepLargerFrame\|foldUsage\|return state;" ...` — a `|` inside a Markdown table
  cell must be escaped as `\|`, so what a reader copies is not what was written. In a basic
  regular expression `\|` is **alternation**, and the escaped form matched eleven lines where
  one was intended. Replaced with three single-pattern commands: **a command containing a pipe
  cannot be cited verbatim in a table cell**, and the escape changes its meaning silently.
- `grep -n "provenance, not authority" ...` and `grep -n "cache kinds" ...` — both phrases wrap
  across two lines in their source files, so neither grep can ever match. Both had output
  pasted beside them.

That is the executable form of the claim this table carried falsely for a round: **"every row
was re-derived by opening the artefact" is unfalsifiable from outside, and "every command in
this table runs" is checked by running them.** A table whose commands are extracted and
executed cannot carry the claim falsely — the check does not verify that a cell's *conclusion*
is right, but it does verify that the evidence offered for it is real and reachable, which is
the failure that actually occurred here four times.

The script is small enough to commit and general to any bean using the `cmd:`/`observed:`
convention (`adr:0005-evidence-lives-in-the-work-item`, `bean:0091`). Whether it belongs in
`tools/` is not this bean's call — `tools/` is owned elsewhere this sprint, and wiring it into
`qualityCheck` would mean editing `build.gradle.kts`, which this bean does not own either. It
is offered, not installed.

**Its scope is the table, and round three found that the scope is the coverage.** The extractor
reads every `cmd` from *this table*, and every defect this section describes was found inside
it. Review re-ran the table's commands and reproduced all of them; it re-ran the `cmd:` blocks
in prose — which the extractor never sees — and found several defective, including two in this
file and one in `bean:0090` whose `sed` ranges no longer emitted the row quoted beneath them.
Same file, same author, same sprint, differing in one variable. `bean:0106` owns extending the
extractor to every `cmd:`/`observed:` block in a bean file and records that comparison as its
evidence.

### What a green check 14 does and does not say about this table

Worth stating beside the table, because the two are easily confused. Check 14 verifies that
each numbered criterion has a **non-empty** evidence cell that is not merely a list of
evidence-kind names. A reviewer confirmed that `-`, `n/a`, `TODO` and `BUILD SUCCESSFUL` all
satisfy it in these cells, and only a bare `test-run` fails.

So **check 14 cannot see whether a cell is true**, and that is exactly how four false cells
passed it through a full review round. The check is a floor against empty evidence, not a
statement about content. Nothing mechanical in this repository reads an evidence cell against
the artefact it describes; a reader running the commands is the only thing that does, which is
what happened here and is why the table is now correct.

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
fabricated — the only commit that ever touched the line carrying the Opus 5 rate introduced it
at $5/$25, and `git log --all -S "outputPerMTok: 75" -- backoffice/` returns nothing. On that
basis a correction was written saying the incident **never happened**, and it was one edit from
landing in both artefacts.

(Two defects in that sentence, both fixed above and both generalised in a separate bean
raised for the purpose. It said
"all three commits" — six within a day, since this branch touches the file; a count over a
growing set is stale on arrival where a quantifier is not. And the search was originally
unscoped and doubled, `-S "75_000_000" -S "outputPerMTok: 75"`, which `git log` resolves
**last-wins** rather than as "either" — so half of it never ran — while the unscoped form also
matched this bean's own prose once committed, making the transcript falsify itself.)

It did happen. `.beans/modus-0002--backoffice-foundation.md:130` records it with a before/after
table and pre-computes this very test's response to it. The defect was fixed **before the
merge**, so it appears in no commit — which is what a pre-merge fix looks like, and under
squash-merge is the normal case rather than an edge one.

```
cmd:      grep -n '15/\$75' .beans/modus-0002--backoffice-foundation.md
observed: 132:`PRICING` claimed $15/$75 per MTok for `claude-opus-5`. The list price is
```

The search names the file that holds the record rather than sweeping
`backoffice/src e2e/tests .beans`. The sweeping form is what found `bean:0002` — see *How it was
caught* — and it is still the wrong command to *record*, for two reasons. It returns many lines
across several files and only one of them was quoted, with nothing saying the rest were dropped:
`[...]` marks elision **within** a line, not omitted lines (`bean:0091`). And two of the lines it
returns are the `cmd:` and `observed:` lines of this block, so the assertion sits inside its own
searched corpus and its output changes every time this bean is edited. The narrow form
establishes the same fact, is one line long, and cannot rot that way.

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

## Three defects this bean committed, found by running the gate

Recorded rather than quietly fixed, because each is an instance of the class this bean is
about, and two of them are classes nobody here had named.

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

  **And it is normative nowhere.** §9.1's MUST bullets still require only the planted
  violation, on `main` and on this branch alike — the document is byte-identical across the
  two, so nothing in this change moved it. The requirement that a mechanism also be observed
  *not* firing on a clean input currently lives in this bullet and in `bean:0090`'s success
  criteria; `bean:0105` records the sweep behind that, and why the vacuity assertion §9.1 does
  carry is a different question. Two unmerged beans agreeing on a rule makes it a convention
  between them: a third bean written next week has nothing to read. `bean:0105` owns getting
  the negative half into §9.1, and is named here rather than in a commit message so the gap is
  visible from the artefact that depends on it.

**A producer-controlled string used as an object key, in a seam that already knew better.**
`usageByMessage` is a plain object and `messageId` is wire data the producer chooses, so an id
naming an inherited property — `constructor`, `toString`, `valueOf` — was a lookup *hit* on a
map that had never seen that message. Two silent failures followed: `framesDisagree` compared
`undefined !== 0` and reported a disagreement on a stream containing none, and
`keepLargerFrame` evaluated `n > undefined` as `false`, kept the inherited value, left the map
unwritten, and **dropped the message from the fold entirely** — halving the run's cost and
understating peak context with nothing on screen to say so.

`Object.create(null)` does not fix this, and it was the first fix considered. It corrects the
initial map and not the invariant: the reducer rebuilds by spreading, and a spread produces a
fresh object with `Object.prototype` back on the chain. **The guard has to be at the lookup,
which is the only place that can be made unconditionally safe** — hence `usageOf`.

**The interesting part is not the bug; it is the unit of analysis.** The obvious reading is a
transfer failure — a lesson learned in one place and not carried to another. It is not, and the
file refutes it: `isPricedModel`, twenty lines away, already reads
`Object.prototype.hasOwnProperty.call(BASE_RATES_UPM, model)`, written by this bean, for a
producer-controlled string, for exactly this reason. The same principle, in the same file, for
the same class of input, applied once and not twice.

So the failure is that **each lookup was treated as a local question instead of asking which
namespaces in this seam are producer-controlled**. That question has a short answer —
`messageId`, `callId`, `model` — and it should have been asked once rather than three times.

That distinction matters because it changes the remedy. "Carry the lesson forward" is an
exhortation, and it scales with vigilance, which is to say it does not scale. "List this seam's
producer-controlled namespaces" is a question with a finite answer that can be written down and
checked against. **A checklist beats an exhortation**, and this one is three items long:

| namespace | reaches | guarded by |
|---|---|---|
| `messageId` | the `usageByMessage` key | `usageOf` |
| `callId` | the transcript block id | `block.kind === 'notice'` in the suppression check |
| `model` | the `BASE_RATES_UPM` key | `isPricedModel` |

All three were found one at a time, by three separate review findings, over two rounds. Asked
as one question, the list takes a minute to produce. Any implementation of `bean:0014` inherits
the same three namespaces across the wire and should produce its own list before writing
lookups, not after review finds them individually.

**"Exhaustive for this seam" is narrowed to the transcript seam, because review found two more
sites outside it.** `backoffice/src/mocks/handlers.ts` indexes `table[domainId]` in `scoped`
and `costByDomain[domainId]` in the cost-summary handler. Both are the same shape — a
caller-supplied key reaching a plain object with no own-property check — with the key taken
from the URL rather than from a transcript frame. Neither ships: MSW handlers run in
development and under test only, so nothing here is a live defect and nothing is fixed for it.
What the two sites cost is the claim: a checklist that says *exhaustive* and stops at the
namespaces one review round happened to visit is the exhortation it replaced, wearing a table.
The scope now stated is the one the list was actually derived over — `transport.ts` and
`useAgentSession.ts` — and the general question, *which keys in this file does someone else
choose*, is the part worth carrying to `handlers.ts` and to `bean:0014`.

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

## Three behaviours that are correct and uncovered

None is a defect. All are recorded because "correct" and "covered" are different claims, and
only one of them has evidence.

**The losing-frame path has no shipped test.** `?fault=usage-disagreement` corrupts frame 2 of a
message — a *winning* frame, larger `outputTokens`, so `kept !== previous` and the map updates.
The other branch is the one where a disagreeing frame *loses*: `kept === previous`, the map is
not rewritten, and the early return is what stops the reducer discarding the notice along with
the frame. That branch was checked by review and behaves correctly; nothing in the suite
exercises it. It is the branch where the detector's suppression and its retention interact, so
it is the one most likely to break under a later edit.

**Usage arriving after `session-end` still moves the cost.** The reducer does not gate on
`status`, so a late `usage` event folds normally and a run's total can change after the UI reads
"Complete". This is arguably right — a late frame is still real spend, and dropping it would
understate the bill, which is the failure this whole bean exists to prevent. But it is
*undecided* rather than decided: nothing states which reading is intended, and a consumer
reading a settled total may see it move. `bean:0014` should settle it when it defines the
terminal event, since the question is really "what does `AgentRunCompleted` promise about the
totals that precede it".

**The unpriced-model path cannot be reached from the console at all.** `totalCostUsd` returns
`null` when `isPricedModel` rejects the id, and that is the behaviour `bean:0090`'s instance 3
is closed against. No e2e test covers it and none can: `AgentConsole.tsx` renders a fixed list
of three model options and all three are in `BASE_RATES_UPM`, so no interaction with the
shipped UI produces an unpriced id. The branch is reachable only from a producer sending a
model the table does not carry, which is exactly the case `bean:0014` will introduce and this
console cannot. Covering it needs either a fault injection like the existing `?fault=` ones or
a unit test on the reducer, and neither is added here — what is recorded is that the
uncoveredness is structural rather than an omission.

There is a second, smaller thing in the same neighbourhood, and it is a citation rather than a
behaviour: `costMicros` in `transport.ts` is exported and called by nothing. It is the checked
front door described in the KDoc, `costMicrosOf` is what the reducer imports, and `bean:0090`'s
instance 3 named `costMicros` as its closure — so the closure was cited at dead code while the
implemented outcome ran through `totalCostUsd`. The citation is corrected in `bean:0090`; the
export is left alone, since deleting a public function is a change this bean's contract does
not cover.

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

**Every occurrence computes the figure, carries it through the summary structure, or renders it
into a table row — and none of them raises.** That is stated over the set rather than counted,
and this paragraph has now been wrong twice for wanting to count it: it said "all six uses render
a table row", which was wrong in both halves, and the revision that corrected it gave a total and
named the rendering lines. Both are figures about `tools/cost-replay.py`, a file this change does
not own and another branch is actively rewriting. The grep above reproduces against that branch
too, so the count was exposure rather than a defect — but the quantifier is what the argument
needed either way, and it is the form that survives the file being edited by its owner.

`cost-replay.py` does exit non-zero: on a bean file missing its `BEGIN`/`END` markers, and on
finding no transcripts to replay. Neither path is reachable from a frame disagreement, which is
the claim that matters here. `--check` returns on input drift before reaching any comparison, and
compares file text when it does; and `./gradlew qualityCheck` runs **no Python at all**
(`cost_lib.py`'s own module docstring records that gap). So the premise is observed to hold on
this corpus and enforced nowhere. An implementation of `bean:0014` must not treat it as
guaranteed by tooling.

## Findings for others, not acted on here

- **`tools/cost_lib.py` asserts, in prose, the opposite of the specification this bean
  publishes — in the file a reader would open to check it.** Two docstrings claim the
  enforcement that the section above establishes does not exist:

  ```
  cmd:      grep -n 'byte-identical\|replay asserts\|never assumed' tools/cost_lib.py
  observed: 170:    `cache_creation_input_tokens` are byte-identical across every frame of a message; the
            171:    replay asserts that on every run and reports the count, so no figure is quoted here — a
            254:    undercounts. Asserted on every run, never assumed.
  ```

  The `cmd:` is `grep -n` because the `observed:` is `grep -n` output: an earlier revision
  carried these three numbered lines under a `sed -n '168,171p;249,255p'`, and `sed -n …p`
  emits no line numbers and no gaps. The transcript was right and the command beside it was
  not, which is the shape this bean's criteria table is mechanised against and which nothing
  checks outside that table (`bean:0106`).

  Neither is true: nothing raises, and no Python runs in `qualityCheck`. This is the most
  load-bearing instance of the wrong claim left in the tree, because `cost_lib.py` is the
  reference implementation `bean:0014`, `bean:0020` and `transport.ts` all point a reader at —
  so a reader who follows the citation to check the premise finds it *reassured* by the very
  file whose silence is the gap. `tools/cost_lib.py` is owned by another agent this sprint and
  is read, never edited, from here. Two docstring lines; the fix is smaller than this note.

- **Nothing compares a constant in code to the authority it must match.** Raised as
  `bean:0090`; the rate table is one instance of it.
- **`bean:0103`'s instance block states a count where its own next paragraph argues for a
  quantifier**, and the `-S` command cited beside it cannot see the commits that falsify it.
  Found from here, and raised as **`bean:0107`**, which owns the sentence, the evidence and the
  one-sentence fix; it is not restated here, and `bean:0103` is merged on `main` and not edited
  from here. Recorded as its own work item rather than left as this bullet, because a bullet in
  a bean that is about to close has no owner.
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
