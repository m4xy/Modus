---
# modus-0094
title: The bean selection algorithm is prose, and an agent got it wrong reading it
status: todo
type: feature
priority: normal
order: CC
created_at: 2026-08-30T00:00:00Z
---

# The bean selection algorithm is prose, and an agent got it wrong reading it

`AGENTS.md` workflow step 1 states the selection rule exactly: skip `type: epic`; among
`status: todo` beans whose every `blocked_by` id is `completed`, the highest `priority` wins;
ties break on `order`, ascending; a bean with no `order` sorts after every bean that has one.

**Nothing computes it.** `docs-lint` check 12 comes within one line of doing so and stops.
It builds the ready set at `tools/docs-lint.sh`:

```sh
printf '%s\t%s\t%s\t%s\n' "$priority" "$order" "$id" "$file" >> "$TMP/bean-ready.tsv"
```

— priority, order, id, file, one row per selectable bean, in exactly the shape the rule
needs. That file is then used exactly twice: once sorted to find **duplicate** `(priority,
order)` pairs among selectable beans, and once to check it is **non-empty**. It is never
ranked. No tool in the repository answers "which bean is next".

So check 12 validates every *input* to the algorithm — edges resolve, no cycles, no ties,
the set is non-empty — while leaving the algorithm itself to be run in an agent's head.

## The evidence that this matters is an agent getting it wrong

Not hypothetical, and not someone else's mistake. While setting `order` on `bean:0065`,
`0066` and `0067` I reported to the orchestrator that the three would still rank near the
bottom of the backlog, because `bean:0027` carries `order: B` and `bean:0017` carries
`order: C`, and I read `B` and `C` as ranking ahead of `AK`, `AP`, `AQ`.

That is wrong. `order` is a lexicographic fractional index, and in ASCII `AK` < `AP` < `AQ` <
`B` < `C`. The three beans were second, third and fourth in the whole backlog, not last. I
corrected it only because I then reconstructed the algorithm in a throwaway script to check —
and the correction had to be sent as a follow-up to an orchestrator already acting on the
wrong figure.

The failure is ordinary and will recur: a single-letter `order` and a two-letter `order` look
comparable to a reader and are not, and `priority` is a word rather than a number so the
primary key does not sort either. This is precisely
`doc:00-constitution#mechanical-enforcement` — a rule that a human or an agent has to
*remember* will eventually be broken — applied to a rule nobody has yet given a tool.

## What to build, and the one judgement it turns on

The mechanism is easy; the question is what it should say.

| candidate | assessment |
|---|---|
| **A `nextBean` task** printing the ranked ready set | Recommended. It answers the question an agent actually asks at step 1, and it is a few lines over `bean-ready.tsv`, which already exists and already has the right columns. |
| Extend check 12 to **assert** a claimed next bean | Rejected. There is nothing to assert against: no file records "the next bean", and creating one would be a derived value stored as state, which `doc:00-constitution#flat-file-first` §2.5 says must be rebuildable rather than authoritative. |
| Print the ranked set on every `docsLint` run | Rejected as the default. `docs-lint` is a gate; a gate that prints a recommendation on every run trains readers to skim its output, which is how check 11's inert `- introduced` went unnoticed for four plants (`doc:00-constitution#observed-failing`). |

**The judgement:** whether the tool should print **one** bean or the **ranked set**. One bean
is the direct answer and is what step 1 asks for. The ranked set makes a wrong answer visible
— a reader who disagrees with the top row can see the second and third and say why — and it
degrades gracefully when two beans genuinely tie on everything. This bean does not decide it;
the implementer should, and should record which and why rather than defaulting.

## Scope

Owned: the task, wherever `doc:15-repository-layout#placement-table` §2.1 puts a
repository-wide check over files rather than over Kotlin — `tools/`, invoked from the root
`build.gradle.kts` — and this bean.

Not owned: `AGENTS.md`'s statement of the rule, which is correct and stays the single
normative source; the tool derives from it and does not restate it
(`doc:05-authoring-for-agents#one-fact-one-place`). Changing the rule itself.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | A command answers "which bean is next" and its answer matches `AGENTS.md` step 1 on the live backlog, including the `AK` < `B` case that a reader gets wrong | |
| 2 | Epics are excluded, and a bean with an unsatisfied `blocked_by` is excluded. Observed by flipping a blocker to `completed` and watching the answer change | |
| 3 | A bean with no `order` sorts **after** every bean that has one, at the same priority. Observed on a planted pair, since this is the rule's least intuitive clause | |
| 4 | `priority` ranks as `high` > `normal` > `low` and not alphabetically — the trap that `high` < `low` < `normal` as strings | |
| 5 | The tool reads the same `bean-ready.tsv` construction check 12 already performs, or shares it, so the two cannot disagree about what "selectable" means. Two implementations of one rule is the defect this bean is about, reproduced | |
| 6 | The one-bean-versus-ranked-set judgement is recorded with its reason | |
| 7 | `./gradlew qualityCheck` green | |

## Sequencing

Nothing blocks this and it blocks nothing. `priority: normal` rather than `high`: the rule is
correctly stated and an agent that runs it carefully gets the right answer, so this removes a
recurring error rather than unblocking work. It earns its place because the error has already
happened once, cost an orchestrator a wrong figure to act on, and was caught by luck rather
than by anything.
