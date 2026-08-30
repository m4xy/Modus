---
# modus-0098
title: Pull-request bodies restate evidence the template forbids them to restate
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# Pull-request bodies restate evidence the template forbids them to restate

`.github/pull_request_template.md`'s `## verify` block says it outright: *"The evidence lives
in the bean, beside the criterion it satisfies. Name it here; do not restate it."*
`adr:0005-evidence-lives-in-the-work-item#evidence-home` is the decision behind it, taken
because evidence written twice is evidence that can disagree with itself — and it did.

Bodies restate it anyway. This is not a proposal for a new rule. **It is a rule that exists,
is written in the template the author is filling in, and is not followed.**

## The instances, counted rather than asserted

Every pull request from #36 to #56, scanned for gate output, test counts and `docs-lint`'s own
`OK` line:

```
cmd:      for n in 36..56: gh pr view $n --json body --jq .body \
            | grep -nE 'BUILD SUCCESSFUL|BUILD FAILED|actionable tasks|docs-lint: OK|^observed:|[0-9]+ passed'
observed: PR 36  2 lines   `BUILD SUCCESSFUL`, and a full `docs-lint: OK` line
          PR 42  1 line    `BUILD SUCCESSFUL`, `docs-lint: OK`      (no figures)
          PR 43  1 line    `BUILD SUCCESSFUL`, `docs-lint: OK — …`
          PR 44  3 lines   `167 actionable tasks`, a full `docs-lint: OK` line, and a
                           reviewer's `BUILD SUCCESSFUL in 7s` quoted in a response table
          PR 45  4 lines   a full `docs-lint: OK` line, `BUILD SUCCESSFUL in 18s`,
                           `35 passed (8.3s)`, `BUILD SUCCESSFUL in 9s`
          PR 47  2 lines   `BUILD SUCCESSFUL in 15s`, `158 actionable tasks: …`
          PR 50  1 line    prose ABOUT the string `BUILD SUCCESSFUL` — not an instance
          PR 55  1 line    `BUILD SUCCESSFUL`, `158 actionable tasks`
exit:     0
```

**Seven bodies restate evidence; six restate a figure that can drift; one hit is a false
positive** (PR 50 discusses the string rather than quoting a run). The brief that raised this
bean estimated four. The measurement is what goes on the record.

### The sharpest form: three branches, three different totals

`docs-lint`'s `OK` line describes **the whole tree**, not the diff. So each branch quotes a
number that is true on itself and false everywhere else:

```
PR 44:  docs-lint: OK — 19 documents, 106 anchors, 969 references, 67 beans, 32 graph edges,
        20 selectable, 67 bean ids, 3 introduced, 64 on origin/main, …
PR 45:  docs-lint: OK — 19 documents, 106 anchors, 944 references, 66 beans, 28 graph edges,
        20 selectable, 66 bean ids, 2 introduced, 64 on origin/main, …
PR 47's branch, run locally rather than quoted in its body — which restates the
        gate a different way, as `BUILD SUCCESSFUL in 15s`, `158 actionable tasks: 4
        executed, 1 from cache, 153 up-to-date`:
        docs-lint: OK — 19 documents, 106 anchors, 927 references, 65 beans, 28 graph edges,
        22 selectable, 65 bean ids, 1 introduced, 64 on origin/main, …
```

Two of those are quotations from pull-request bodies; the third is a local run on a third
branch, labelled as such because **PR #47's body does not quote that line at all** — an
earlier draft of this bean presented it as though it did. Catching that is the fourth
self-inflicted instance in this sequence and it is left on the record rather than smoothed
away: composing a comparison table is itself an occasion to invent a figure that no run
produced.

Three branches, one repository, three answers. Every one of them was accurate when
it was taken. `64 on origin/main` is the only figure all three agree on — and it is precisely
the figure **the first merge invalidates for the other two**. A number that all parties agree
on today and none can hold tomorrow is the clearest possible statement of why this belongs in
one place that moves with the code.

### The first instance is the pull request raising this bean

PR #47 restated the gate line under `## verify`, and its `size: files=4 lines=+496/-76` was
already wrong by the next commit. That author had spent an hour cataloguing exactly this
class of defect in four other beans at the time.

That is the argument, and it is not "authors are careless". **A defect that survives the
attention of the person documenting it is a defect that needs a mechanism**, or a rule so
cheap to follow that there is nothing to get wrong.

## What to do about it

The fix is **enforcing a rule that already exists**, and the cheapest form is to stop
producing the thing that drifts:

| | approach | cost |
|---|---|---|
| 1 | **Name the run, do not quote the line.** `gate` job of run `<id>` on head `<sha>`, conclusion `success`. Immutable, checkable by anyone, and already what `## verify` asks for | none — it is less typing than the quotation |
| 2 | Derive `size:` from `git diff --numstat` at push time rather than typing it | none, and it is the only numeric field the body legitimately owns |
| 3 | A check comparing a body's quoted line against a fresh run | real, and see below |

Approach 3 was the original proposal for this bean and it is **not** recommended. `docs-lint`
cannot own it: `doc:05-authoring-for-agents#checks` requires every check to be decidable from
repository contents alone, and a pull-request body is not in the tree — locally there is often
no pull request at all. Putting it in CI instead breaks the other invariant:
`doc:00-constitution` §7.2 step 4 promises a green local `qualityCheck` implies a green CI run,
and says plainly that the moment CI can run something local cannot, the promise is gone.
So a body check costs one of two documented properties, to police a figure that approach 1
removes for free.

### Three techniques, for the figures that must be quoted somewhere

All three came out of fixing this in PR #47 and are the practical half of this bean:

- **Quote what is stable, elide what moves — with a marked elision — and pin the rest to an
  immutable commit.** In `bean:0055` the nine corpus counts are elided as
  `[... nine corpus counts, elided …]` while the four counts check 14 owns are quoted
  verbatim, because those describe the closures rather than the tree. The one full line that
  is quoted is pinned to the CI run of a named commit, where it is reproducible forever.
- **A fixed point usually exists, and it is worth reaching for before eliding.** The count
  converges if the paste adds no counted token: finalise the prose, run the gate, paste
  figures only, re-run. Two runs and the second prints the quoted line back identically. That
  fails when the paste itself carries a typed reference — which is what happened in
  `bean:0055`, three times, each fix moving the number it was fixing — and eliding is the
  answer only then.
- **Scope a diff figure to exclude the file it is written in.** A `numstat` row that counts
  its own bean is stale the moment it is pasted, so quote the diff for everything *but* the
  file carrying the quote, and say that is what the figure is.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The template's existing rule is restated nowhere and enforced somewhere: either every open pull-request body stops quoting gate output, or the refusal is recorded with its reason | citation over the open pull requests |
| 2 | `size:` is derived rather than typed, or the field is removed | diff |
| 3 | The decision on a mechanical check is recorded, with the two documented properties it would cost named explicitly | citation |
| 4 | The three quoting techniques are stated where an author filling in the template will read them | citation |
| 5 | `./gradlew qualityCheck` green | test-run |

Criteria 1 and 3 are satisfiable by acting **or** by refusing on the record. A refusal closes
them; silence does not.

## Not in scope

- The four beans closed in PR #47, which are `completed` and frozen by `docs-lint` check 11.
- Transcript fidelity **inside** a bean — marked elisions, runnable commands, a command that
  answers its criterion as worded. That is `bean:0091`, and this bean is its counterpart one
  artifact along.
- The generated-block staleness `bean:0059` owns, which is the same disease in a third place.
