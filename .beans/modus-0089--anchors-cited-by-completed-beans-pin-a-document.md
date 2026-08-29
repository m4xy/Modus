---
# modus-0089
title: An anchor cited by a completed bean cannot move, so the line ceiling has no remedy but deletion
status: todo
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
---

# An anchor cited by a completed bean cannot move, so the line ceiling has no remedy but deletion

`doc:README#changing-this-package` states the remedy for a document that reaches the
`adr:0003-document-line-budget#line-budget-decision` ceiling: "A file that outgrows the
ceiling is two files, or it contains material that belongs in an ADR or in a skill."

For `doc:00-constitution` and `doc:35-testing`, both at exactly 500 of 500 today, that
remedy is largely unavailable, and the reason is mechanical rather than stylistic.

## The constraint

1. A reference is `doc:<NN>-<slug>#<anchor>`, and it resolves through the **file** the
   `doc:` id names (`doc:05-authoring-for-agents#reference-syntax`). Moving a section to a
   second document or to an ADR therefore changes every reference to it — the `doc:` half,
   not only the `#anchor` half.
2. `docs-lint` check 6 fails a reference that resolves to other than exactly one target, and
   it reads **every** file in the tree, beans included.
3. A `completed` bean is final: it may gain `## Amendments` entries and may change in no
   other way (`adr:0005-evidence-lives-in-the-work-item#finalisation`, check 11).

So an anchor cited by a completed bean is pinned to its file. Moving it breaks check 6
against a record nobody may lawfully edit, and the build then fails with no legal fix
available in the repository.

`documentation/README.md` already states this for section **numbers** — "a reallocated
number silently misdirects a record nobody may edit". It is equally true of anchors, and of
the `doc:` id itself, and that half is not written down.

The set is monotonic: it only grows, because beans only become `completed`.

## The measurement

Exact, and taken twice. `git ls-tree --name-only 8181726 .beans/ | wc -l` → `64`, of which
23 were `completed`; the second run below is on this branch's head, where the tree holds 69
beans and 27 are `completed`. The first version of this paragraph said "23 of the 65", which
was neither run: 65 was this branch's bean count, borrowed into a sentence whose sha names
the tree without it. An unqualified count beside a sourced one is the worse arrangement,
because the sourced neighbour lends it credit.

```
cmd:      python3 - <<'EOF'
            import re, glob, os
            def provides(p):
                s = open(p).read().split('---')[1]; out = []; inp = False
                for l in s.splitlines():
                    if l.startswith('provides:'): inp = True; continue
                    if inp:
                        if l.startswith('  - '): out.append(l[4:].strip())
                        else: break
                return out
            beans = [(os.path.basename(b), open(b).read()) for b in sorted(glob.glob('.beans/*.md'))]
            done  = [(n, t) for n, t in beans if re.search(r'^status:\s*completed\s*$', t, re.M)]
            for doc in ('documentation/00-constitution.md', 'documentation/35-testing.md'):
                A = provides(doc); P = [a for a in A if any(a in t for _, t in done)]
                print(doc, len(A), 'anchors,', len(P), 'pinned')
                print('  pinned:', ', '.join(a.split('#')[1] for a in P))
                print('  free  :', ', '.join(a.split('#')[1] for a in A if a not in P))
            EOF
observed: documentation/00-constitution.md 11 anchors, 6 pinned
            pinned: layering, evidence-rule, domain-scoping, mechanical-enforcement,
                    observed-failing, orchestrator
            free  : flat-file-first, context-budget, workflow, independent-review,
                    bean-lifecycle
          documentation/35-testing.md 10 anchors, 7 pinned
            pinned: purity-rules, assertions, load-bearing-evidence, fixture-variation,
                    mutation-testing, coverage, gaps
            free  : definitions, source-sets, unit-classpath
exit:     0
```

**13 of 21 anchors across the two documents were pinned at `8181726`, and 15 of 21 are
pinned at this branch's head** — `context-budget` and `bean-lifecycle` joined the set when
four more beans completed.

The claim that survives is about the **numerator**, not the ratio. The pinned count is
monotonic: an anchor enters the set when a bean citing it completes, and completion is
one-way, so it never leaves. The denominator is not — 21 is the `provides:` count of two
documents, and any edit adding or removing an anchor moves it; `docs-lint` reported 107, 108
and 109 anchors at three points in this one stack. So "15 of 21" is two figures with
different behaviour written as one, and only the left-hand one is evidence of the trend. The
right-hand one is context.

Re-run on this head:

```
cmd:      python3 - <<'EOF'   (the script above, unchanged)
observed: beans total 69, completed 27
          documentation/00-constitution.md 11 anchors, 8 pinned
            pinned: layering, evidence-rule, context-budget, bean-lifecycle,
                    domain-scoping, mechanical-enforcement, observed-failing, orchestrator
            free  : flat-file-first, workflow, independent-review
          documentation/35-testing.md 10 anchors, 7 pinned
            pinned: purity-rules, assertions, load-bearing-evidence, fixture-variation,
                    mutation-testing, coverage, gaps
            free  : definitions, source-sets, unit-classpath
          TOTAL 15 of 21
exit:     0
```

At `8181726` the same script reported 6 pinned in `doc:00-constitution` and 7 in
`doc:35-testing`, and the six anchors still free at that head are each cited by a bean not
yet `completed`, so on current behaviour the free set goes to zero as those beans close.

The sharpest single instance is `doc:00-constitution#observed-failing`, cited by eleven
completed beans: `bean:0010`, `bean:0028`, `bean:0029`, `bean:0034`, `bean:0035`,
`bean:0038`, `bean:0045`, `bean:0046`, `bean:0048`, `bean:0051`, `bean:0052`.

The instance that blocks the documented remedy outright is `doc:35-testing#mutation-testing`,
cited by `bean:0010`. §7 is the one section of `doc:35-testing` that is unambiguously ADR
material — a decision, with alternatives considered, and a stated reopening condition — and
`doc:README#ownership` gives ADRs exactly that. It cannot be moved.

## The casualty that was not one, withdrawn

This section claimed a concrete casualty and the claim was false. It said
`doc:00-constitution#observed-failing`'s superseded clause — "recorded verbatim (§3), in the
work item **and in the pull-request body**", which
`adr:0005-evidence-lives-in-the-work-item#evidence-home` overtook — had "no line to correct
it in: the file is at 500 of 500".

**Correcting it cost zero lines.** The clause sat inside one line, and deleting the offending
phrase left that line one line long. A within-line deletion cannot exceed a line ceiling, and
no arithmetic beyond that was needed to see it. It is fixed in the branch that raised this
bean; `doc:00-constitution` is 500 before and after.

The supporting claim was wrong in the same way. It said the matching defect in
`doc:35-testing` §6 "was only fixable because that document had two removable recaps in it".
That fix was two lines to two lines, net zero. The recaps were spent on a different
transaction — §6's new bullet — and were never needed for the correction.

**Why this mattered more than an ordinary error.** It was the one instance a reader could
check in ten seconds, offered as the concrete proof of a thesis the rest of the bean argues
from measurement. A false ten-second example does more damage than no example: it invites a
reader to test the cheapest claim, find it wrong, and stop.

**The thesis is unaffected and rests where it always did**, on the pinned-anchor measurement
below, which reproduces. What this section now records is the opposite lesson: **before
asserting that a document has no room for a correction, price the correction.** Ceilings bind
line *counts*, and a great many corrections — a struck phrase, a substituted word, a shortened
citation — do not change one.

## The casualty that survives checking

`doc:00-constitution#observed-failing` states that a mechanism must be observed rejecting a
planted violation. That is necessary and not sufficient: **a mechanism that fires on every
input has also been observed firing.** The claim a reader needs is three observations — it
fires on the plant, it fires the expected number of times, and it is silent on the
unmodified source.

The rule was learned from a live instance (`bean:0068` carries the provenance) and it
belongs at `#observed-failing`, beside the half already there. It could not go there. The
file is at 500 of 500 and its anchors are pinned, so the choice was between deleting
something true and putting the rule somewhere else. It went to
`doc:50-memory-and-evidence` §2.2's table, which does point back; `doc:00-constitution`
carries no pointer forward, because a pointer is also a line.

**This is the wall causing the defect the package exists to prevent.** One rule now lives in
two documents, its halves cannot reference each other, and
`doc:05-authoring-for-agents#one-fact-one-place` is what that section is called.

This is the casualty that survives checking, and it is the only one this bean now claims. The
withdrawn one was a superseded rule said to be uncorrectable; it was correctable within its
own line. This one is a new rule that genuinely cannot be put where it belongs, because
putting it there needs a line the file does not have and no phrase in `#observed-failing` is
spare. The distinction between the two is the whole of what this bean learned: **the wall
binds what must be added, not what can be struck.**

## Not proposed here

No remedy is proposed. Naming the constraint is the whole of this bean, because every
candidate — an alias mechanism in check 6, a redirect stub document, permitting an
`## Amendments`-only reference fix on a completed bean, raising the ceiling — changes either
`adr:0003` or `adr:0005`, and choosing between two ratified ADRs is a decision with more than
one defensible answer (`doc:00-constitution` §4).

What is decidable and is recorded above: which anchors are movable today, and that the set
shrinks.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The constraint is stated where the remedy is stated — `doc:README#changing-this-package` covers section numbers and must cover anchors and the `doc:` id | citation |
| 2 | The count is re-measured at the time of the fix, not carried from this bean; the set is monotonic and this bean's numbers are a snapshot | command |
| 3 | A decision is recorded, as an ADR, on which of `adr:0003` and `adr:0005` yields, or that neither does and the remedy is something else | citation |
| 4 | ~~`doc:00-constitution#observed-failing`'s superseded pull-request-body clause is corrected~~ — **met early**: corrected in this bean's own branch at zero line cost, which is why the section above is withdrawn rather than restated | citation |
| 5 | `doc:00-constitution#observed-failing` carries the negative half of its own rule, or cites where it lives | citation |

## Also at the wall

`doc:20-ddd-practices` is the next document to which all of the above applies, and its
anchors carry the same exposure.

**No line count is stated here, deliberately.** The first version of this section said "498
of 500", which was true for one commit of one branch and was falsified by the author's own
next edit; at `8181726` the file was 487 and at `97f13b0` it is 500. A line count in prose is
invalidated by any edit to the file, including edits that change no rule, so it has a shorter
half-life than anything else this bean could assert. `wc -l documentation/20-ddd-practices.md`
answers it at any commit, and `bean:0068` records the figure with its command at the commits
where it was load-bearing.

The pinned-anchor count above is the number this bean rests on precisely because it does not
behave that way: it moves only when a bean completes, which is a reviewable event, not a
side effect of editing prose.
