---
# modus-0052
title: Commit the orchestrator role to the constitution
status: completed
type: task
priority: high
order: A
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
---

# Commit the orchestrator role to the constitution

The session that produced this bean ran seven subagents and encoded their learnings, and the
orchestrator still implemented several work packages itself. That was the wrong division of
labour and it is now a rule rather than a preference.

## The measurement that makes it a rule

| | tokens |
|---|---|
| seven subagents | 71k–190k each, returning a few hundred lines apiece |
| the orchestrator | 85% of a 1M window |

The subagents' reading, false starts and reverted plants never entered the orchestrator's
window — that is the mechanism working. The 85% came from work the orchestrator did itself,
all of it delegable. A subagent's context is discarded on return; the orchestrator's is the
only one spanning the whole programme, which makes it the scarcest resource in the system
and the one least defensible to spend on implementation.

## Where it landed, and why split

`doc:00-constitution` §12 states the rule — the role, the context-scarcity reason, and the
one non-delegable duty. `doc:80-agent-operating-procedure` §0 carries the operating detail:
the five numbered rules, what a brief must contain, and the caveat that an agent's report is
evidence rather than a verdict.

Split because the constitution was at 484 of `adr:0003-document-line-budget`'s 500-line
ceiling and the whole section did not fit. That forced the right shape anyway — the rule is
constitutional, the briefing procedure is not. Two restatements were trimmed to make room
(§10, §11), both of which were paraphrasing documents they already cite.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | The constitution states the orchestrator's job as prioritisation and delegation, and names executing the work as a failure of the role | `doc:00-constitution#orchestrator` |
| 2 | It states that an orchestrator may spawn another orchestrator for a whole work package | §12 |
| 3 | It states the context rule, with the reason rather than as an assertion | §12 — a subagent's context is discarded on return; this one is not |
| 4 | Encoding returned learnings is named as non-delegable | §12, citing `doc:README#encoding-rule` |
| 5 | The brief's required contents are stated where an orchestrator will read them | `doc:80-agent-operating-procedure#orchestrating` |
| 6 | An agent's report is treated as evidence, not verdict — reproduce or say you did not | `doc:80` §0, last block; three findings this session carry that caveat explicitly |
| 7 | Both documents stay within their line budgets | `docs-lint` check 8: `doc:00` 500/500, `doc:80` 432/500 |
| 8 | `./gradlew qualityCheck` green | `test-run` |

## Not done

No enforcement. Nothing checks that an orchestrator delegated rather than implemented, and
nothing could from repository contents alone — the evidence is in a transcript, not a tree.
It is a rule enforced by review and by the context ceiling asserting itself, which is the
honest status (`doc:00-constitution#observed-failing`). Recorded rather than dressed up as a
gate.
