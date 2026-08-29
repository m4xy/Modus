---
# modus-0054
title: Take the cost baseline, and record spend at the harness edge
status: in-progress
type: feature
priority: high
order: B
created_at: 2026-08-29T00:00:00Z
---

# Take the cost baseline, and record spend at the harness edge

`doc:60-cost-model` §1.3 says measure before optimising, and nothing has been measured. Every
"we made this cheaper" claim this repository will ever make needs a number to be measured
against, and the data to produce it already exists in `~/.claude/projects/`. It is also the
only cost capability that pays back **before** Modus self-hosts, it needs no `core/` change,
and its output is the fixture corpus `bean:0014` and `bean:0016` would otherwise be built
against invented data.

Two deliverables, two stacked pull requests.

**A — the replay.** `tools/cost-replay.py` folds every transcript into per-run and
per-pull-request totals and writes `domains/modus/cost/replay/`. Both the tool and its output
are committed: the transcripts live outside the repository on one machine and are pruned, so
a baseline that must be regenerated to be read is not a baseline.

**B — the recorder.** A `Stop`/`SubagentStop` hook writes one `doc:60-cost-model#spend-record`
-shaped NDJSON record per agent run into `domains/modus/cost/`.

A **tool**, not a skill. `doc:70-skills#when-to-extract`'s escalation order stops at step 2:
the output is a file and the exit code is the verdict, so no judgement remains for a model to
supply. Tier 3 by `adr:0006-framework-boundary#the-test` — another domain adopting Modus would
want its own SDLC measured its own way — even though the record *shape* is tier 1.

## The headline

| | |
|---|---:|
| runs replayed | 62 (2 root sessions, 60 subagent runs) |
| tokens | 665,570,083 |
| cache-read ratio | **98.4%** |
| derived cost | **$407.33** |
| delegated share of cost | **56.8%** — included, not excluded |
| largest peak context | 865,375 tokens, 2.9x the 300k ceiling (`doc:00-constitution#context-budget`) |
| per-pull-request spread | 35 PRs, min $1.07, median $5.38, max $56.71 |

Fresh input plus output is 0.3% of all tokens. Cache read is the bill.

## What the transcripts do and do not carry

Four claims were handed to this work as prior findings. Each was re-derived rather than
trusted, and two were wrong.

| claim | verdict | what was observed |
|---|---|---|
| subagent spend is not attributable from transcripts | **false** | it is in `<sessionId>/subagents/agent-*.jsonl`, one file per subagent, absent from the parent transcript. Reading only `*.jsonl` at the top of the project directory hides 56.8% of the spend |
| `parentRunId` is unobtainable | **false** | the `agent-*.meta.json` sidecar carries `toolUseId`, the id of the `Task` tool_use block in the spawning run. 60 of 60 edges resolved, at depth 1 and depth 2 |
| assistant lines carry `durationMs` | **false** | the field does not exist. Wall clock is `max(timestamp) - min(timestamp)` and therefore includes idle time |
| `gitBranch` joins runs to branches | **false** | it is the literal string `HEAD` on 4,332 of 4,332 assistant messages, while the repository was on named branches throughout. Unusable as a join key |
| `message.usage` repeats per frame | **true** | 4,332 lines carry 4,332/1.833 distinct `message.id`s. Summing lines overcounts by 1.833x |
| `output_tokens` on a frame is a stale snapshot | **true, in subagent transcripts only** | root-session frames agree; subagent transcripts persist partial frames (`output_tokens: 3` superseded by `345`). Taking the first frame loses **898,426 output tokens, 45.1% of all output**. `input`/`cache_*` agree on every frame with zero exceptions, so the largest-output frame is the authoritative one |

The pull-request join therefore does not use `gitBranch`. A run that ran `gh pr create` and got
a `/pull/N` back owns PR N exactly, and so do its descendants; an orchestrator that opened
several is split on its own creation timestamps and that share is labelled `seg`, never folded
into the exact figure.

## Success criteria and evidence

| # | criterion | evidence | PR |
|---|---|---|---|
| 1 | The replay produces per-run and per-pull-request totals with all token kinds first class | `domains/modus/cost/replay/baseline.md`, `runs.ndjson` | A |
| 2 | Frame multiplicity is deduplicated and the dedupe rule is asserted, not assumed | `command`: `python3 tools/cost-replay.py`; observed `repeated frames … agree on input and cache tokens \| 0 disagreement(s)` | A |
| 3 | Delegated spend is included and its share stated | observed `Delegated spend is INCLUDED: 56.76% of the dollar total is subagent runs (60 of 62 runs)` | A |
| 4 | Dollars are integer micro-dollars, with cache read and both cache-write TTLs priced separately | `tools/cost_lib.py` `cost_micros`; rate table rendered into the baseline | A |
| 5 | Input hashes recorded, and re-checkable | `command`: `python3 tools/cost-replay.py --check`; observed `baseline inputs have moved on: 2 changed, 0 gone.` when a live session appended, exit 1 | A |
| 6 | One spend record per agent run is appended at the harness edge | `domains/modus/cost/0001.ndjson` | B |
| 7 | `parentRunId` is populated for a subagent run | `command`: recorder self-test | B |
| 8 | Fields the harness cannot supply are omitted, never invented | `tools/cost-record.py`; the table below | B |
| 9 | `./gradlew ktlintFormat && ./gradlew qualityCheck` green | `test-run` | A, B |

### Criterion 5, verbatim

```
cmd:      python3 tools/cost-replay.py --check
observed: baseline inputs have moved on: 2 changed, 0 gone. The committed
          figures describe the 122 files hashed at generation and are not
          reproducible from today's transcripts. Regenerate to re-baseline.
exit:     1
```

That is the check working, not failing: the two changed files are the transcripts of the
sessions that were live while the baseline was taken. `--check` verifies the recorded input
hashes **before** comparing output, so drift in the inputs is reported as drift in the inputs
rather than as a wrong number. Only a mismatch with the inputs unchanged means the replay is
non-deterministic, and that is reported as a bug in the tool.

## Not done

- **No gate.** Nothing in `./gradlew qualityCheck` runs either script. ktlint, Detekt and
  ESLint cover Kotlin and TypeScript; there is no Python gate, and adding one means editing
  `build.gradle.kts`, which this work does not own. `--check` exists and is runnable; nothing
  runs it.
- **`gitBranch` is a harness defect, not something this can fix.** The recorder works around it
  by resolving the branch from the hook's `cwd` at record time. The replay cannot: the
  transcripts are already written.
- **41% of the dollar total is attributed by timestamp, not exactly.** Stated in the baseline
  as its own column. An orchestrator that opens twenty pull requests from one session cannot be
  split any better from the record it leaves.
- **No price book.** `doc:60-cost-model` §2.1 requires `domains/<domainId>/cost/price-book.md`
  with `fetch` evidence per entry; it does not exist (`bean:0001`), so every dollar here is
  derived from a rate table inside the tool and is labelled derived wherever it is printed.
  `priceBookEntryId` is consequently unpopulated.
