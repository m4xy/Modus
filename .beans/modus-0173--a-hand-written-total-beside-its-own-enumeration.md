---
# modus-0173
title: Nothing checks a stated total against the enumeration it summarises
status: todo
type: fix
priority: normal
order: CN
created_at: 2026-09-05T00:00:00Z
---

# Nothing checks a stated total against the enumeration it summarises

Two defects in one review round of `bean:0147`, both the same shape, on top of seven earlier
instances this sprint:

| where | stated | enumerated |
|---|---|---|
| `bean:0147`, mutation tally | "sixteen mutations, **fifteen killed** and one demoted" | 16 `planted:` blocks — 13 killed, 2 surviving, 1 killed. **14 killed, 2 surviving.** The stripes plant was counted in both columns, called a survivor in one sentence and killed in the next |
| `bean:0033`, restoration tally | "**Eight** restorations by hand" | seven named, and `bean:0066`'s two omitted entirely — that bean merged after the line was written |
| `bean:0033` line 87 (pre-existing, inherited) | "every one of the **six** restorations" | five named |
| `config/coverage/baseline.tsv` note | carried the same drift as `bean:0033` independently | — |

**The failure is not arithmetic.** In every instance the total was correct when written and
was made wrong by a later commit that extended the enumeration beside it — usually a
different agent's, sometimes a merge. `doc:00-constitution` §9 is explicit that a rule anyone
has to remember is eventually broken, and "update the total when you add a row" is exactly
such a rule. It is also invisible in review: the number and the list are in the same
paragraph, and a reviewer who reads the list does not re-count it.

## What to build

A `docs-lint` check that counts the machine-countable enumerations a bean makes and compares
them against the totals the bean states. The one that is unambiguous today is `planted:`
blocks in an evidence section, which `bean:0147` has 16 of; the restoration tallies are prose
and are the harder half.

**It must fail, not report.** A reported number nobody asserts on is a diagnostic, and this
repository has the case history: `docs-lint`'s own counts line was added as a vacuity
assertion and `bean:0127` records that most checks contribute no figure to it. A count that
prints and never blocks is the shape `bean:0051` found inert in CI.

## Success criteria

| # | criterion |
|---|---|
| 1 | A bean stating a total for a countable enumeration is checked against it, and a mismatch **fails** `docs-lint` with both figures named |
| 2 | `planted:` blocks are the first countable kind. Observed rejecting a planted violation: state a wrong total in a bean and watch the check refuse it, against a negative control where the total is right (`doc:00-constitution#observed-failing`) |
| 3 | The check is not vacuous on the corpus: it reports how many beans stated a countable total and how many were compared, so a run that recognised no totals is distinguishable from a run that found them all correct — the `-` versus `0` signal `bean:0051` records |
| 4 | Decide and record how a total is **declared**, rather than inferring it from prose. Guessing which English number in a paragraph is a total is not decidable; a marked form the author opts into is. The cost of an opt-in is that an unmarked total stays unchecked, and that cost is stated rather than hidden |
| 5 | Whether the prose tallies — `bean:0033`'s restorations, `config/coverage/baseline.tsv`'s note — can be brought in at all is answered rather than left open. If they cannot, the honest outcome is criterion 4's opt-in plus a note saying so |
| 6 | `./gradlew qualityCheck` green |

## Not in scope

Recounting the corpus. `bean:0147` and `bean:0033` are corrected by hand in the pull request
that raised this bean; every other bean is `completed` and frozen (`adr:0005#finalisation`),
so a wrong total in one is an amendment and not an edit.
