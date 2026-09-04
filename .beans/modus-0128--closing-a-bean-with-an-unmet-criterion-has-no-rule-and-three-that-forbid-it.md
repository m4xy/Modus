---
# modus-0128
title: Three beans have closed with an unmet criterion in three vocabularies, and every document that speaks to it says a work item may not
status: todo
type: fix
priority: normal
created_at: 2026-09-04T00:00:00Z
# No `order`. Absence is a defined position — `AGENTS.md` step 1 sorts an unordered bean
# after every ordered one and check 12 does not flag it — and this bean has no claim on
# being reached before the ordered `priority: normal` work already queued.
---

# Three beans have closed with an unmet criterion in three vocabularies, and every document that speaks to it says a work item may not

Three beans on `main` are `status: completed` with a success criterion their own record says
was not met. They use **three different phrasings** for the one verdict, which is the tell: an
undocumented practice has no vocabulary, so each author invented one.

| bean | which | the phrasing it used | the ruling |
|---|---|---|---|
| `bean:0049` | its second | `**NOT MET**` | the criterion was unmeetable as written; the owner ruled the criterion wrong rather than the work |
| `bean:0093` | its third | `**NOT MET AS WORDED**` | the adopted rule is a property and does not reject the container class by construction; the claim was corrected rather than the mechanism narrowed |
| `bean:0123` | its first | `**met in part**` | the covered half holds; the bypass assertion is a lexical enumeration and `bean:0126` owns the residual |

**That table is written "its second" rather than "criterion 2" on purpose, and the reason is
this repository's own defect.** A row of a table is a citation site, and check 14's matcher
reads a number after a `criteri(on|a)` token without reading the polarity of the claim around
it (`doc:05-authoring-for-agents#checks`). Written the natural way, a table *about* three
beans' unmet criteria would answer three of this bean's own criteria, from a row asserting
they were not met. `bean:0087` and `bean:0061` were each caught by that while documenting it.

## What documentation actually says, which is not nothing

This bean was dispatched on the claim that no rule permits or forbids the practice, from a
search of `doc:00-constitution`, `doc:80-agent-operating-procedure` and `documentation/adr/`.
**That is wrong, and the correction is the finding.** Widening the same question to the whole
package returns four statements, none of them in those three places, and every one of them
pointing the same way.

```
head:     3b02871, working tree clean
cmd:      /usr/bin/grep -rniE 'not met|unmet|met in part|partially met' documentation/ AGENTS.md \
            > [...]/searchA.txt
observed: documentation/80-agent-operating-procedure.md:329:4. A criterion with no evidence is **unmet**. There is no third state.
          documentation/20-ddd-practices.md:392:| Aggregate-internal | Aggregate method, before mutation | A work item may not close with an unmet success criterion |
          documentation/20-ddd-practices.md:401:  surface: `WorkItemNotClosableException(unmetCriteria)`. These extend a sealed
          documentation/30-code-style.md:334:| Test names | Full sentences describing behaviour: `"refuses to close a work item with an unmet success criterion"` |
          documentation/70-skills.md:135:a precondition is unmet; it never proceeds hopefully.
          documentation/70-skills.md:279:1. **Check preconditions**, and abort with a clear reason if unmet. Never proceed hopefully.
          documentation/70-skills.md:285:   that satisfies it. A criterion with no evidence counts as unmet.
          documentation/70-skills.md:287:   be fine". Partial completion is `failed` with a list of unmet criteria.
          documentation/70-skills.md:290:   unmet criteria, and its best hypothesis. It does not thrash, and it does not weaken
exit:     0
```

A claim of absence needs a second search by different words and a control
(`doc:50-memory-and-evidence#corpus-figures`). The second search asks for the act rather than
the verdict, and returns nothing the first did not, plus two lines about *when* a close happens
rather than *whether* it may.

```
head:     3b02871, working tree clean
cmd:      /usr/bin/grep -rniE 'close[sd]? (a |the )?(bean|work item|criterion)|closing (a |the )?(bean|criterion)' \
            documentation/ AGENTS.md > [...]/searchB.txt
observed: documentation/60-cost-model.md:418:| Epic view | Rolled-up spend, cost per closed work item, forecast to completion |
          documentation/80-agent-operating-procedure.md:330:5. Once the bean is `completed` it is final. A correction to a closed bean is an entry under
          documentation/50-memory-and-evidence.md:472:  fact, or used to close a criterion.
          documentation/00-constitution.md:126:be written down; they may not be stored as memories, and they may not close a work item.
          documentation/00-constitution.md:325:So closing a bean is always the *next* change, and is the first act of the session after a
          documentation/30-code-style.md:334:| Test names | Full sentences describing behaviour: `"refuses to close a work item with an unmet success criterion"` |
exit:     0

control:  the same apparatus, for a string known to be present
cmd:      /usr/bin/grep -rn 'There is no third state' documentation/
observed: documentation/80-agent-operating-procedure.md:329:4. A criterion with no evidence is **unmet**. There is no third state.
exit:     0

control:  documentation/adr/ is reached by -r, so its silence is measured and not assumed
cmd:      /usr/bin/grep -rln 'evidence' documentation/adr/
observed: documentation/adr/0001-record-architecture-decisions.md documentation/adr/0002-flat-file-over-database.md
          documentation/adr/0006-framework-boundary.md documentation/adr/0005-evidence-lives-in-the-work-item.md
exit:     0
```

Both searches ran under `/usr/bin/grep`, `grep (BSD grep, GNU compatible) 2.6.0-FreeBSD`, which
is what CI has; this harness's interactive `grep` is a shell function running `ugrep 7.8.4`.
The corpus is the nineteen files `docs-lint`'s own `OK` line counts as `19 documents`.

### The four statements, and why each one misses

- **`doc:80-agent-operating-procedure#self-validate` step 6 point 4** — *"A criterion with no
  evidence is unmet. There is no third state."* Its subject is the **absence of evidence**, not
  the close. But it settles the vocabulary question against the corpus regardless: `met in part`
  is precisely a third state, and it is on `main`.
- **`doc:20-ddd-practices` §7.1** — *"A work item may not close with an unmet success
  criterion"*, standing as the worked example of an aggregate-internal invariant.
- **`doc:20-ddd-practices` §7.2** — `WorkItemNotClosableException(unmetCriteria)`, standing as
  the worked example of a named domain exception.
- **`doc:30-code-style` §7** — `"refuses to close a work item with an unmet success criterion"`,
  standing as the worked example of a test name.
- **`doc:70-skills`** — *"Report a binary verdict: `passed` or `failed`. Never 'mostly done'.
  Partial completion is `failed` with a list of unmet criteria."* Its subject is a skill
  invocation's report, not a bean's close.

Three of those five are **illustrative examples of an aggregate that does not exist yet**, and
that is the whole reason nobody has noticed the contradiction: they are read as filler in a
table about where invariants live, how to express them and how to name a test.

```
head:     3b02871, working tree clean
cmd:      /usr/bin/grep -rn "WorkItemNotClosable" . --exclude-dir=.git
observed: ./documentation/20-ddd-practices.md:401:  surface: `WorkItemNotClosableException(unmetCriteria)`. These extend a sealed
exit:     1
```

One hit, in the document that names it as an example. No `WorkItem` aggregate, no exception, no
test. The invariant is unbuilt, so nothing has ever been observed rejecting a close — which is
`doc:00-constitution#observed-failing`'s definition of a claim rather than enforcement.

### Why an example in `doc:20` is not filler here

`doc:40-durability` §3.1 is explicit that these are not two concepts: *"There is **one**
work-item concept, stored in **one** shape"*, `.beans/` being that store for the `modus` domain,
and *"When Modus manages this repository (`00-constitution.md` §13), `.beans/` is what it
manages, with no migration and no import step."*

So the aggregate whose invariant `doc:20` §7.1 names is the aggregate that will one day load
`.beans/modus-0049`, `.beans/modus-0093` and `.beans/modus-0123` — three documents it would
refuse to have produced. Built as written, with the 100% `BRANCH` coverage `doc:20` §7.3
requires of every aggregate invariant, **Modus could not self-host its own work store.** That
is the cost of leaving this unsettled, and it is not paid by the beans already closed: it is
paid by whoever builds the aggregate, who has no way to know from `doc:20` that `main`
disagrees with it three times over.

## Is it mechanisable? Partly, and the part that matters is not

Check 14 has **no concept of `met`**. Its whole vocabulary, from the analyser's own header:

```
head:     3b02871, working tree clean
cmd:      sed -n '1,20p' tools/lib/docs-lint-c14.awk > [...]/c14-header.txt
observed: [...] the loading comment and the KINDS note, four lines
          #   UNTERMFENCE <line>          a fenced block opened there and never closed
          #   NOEV                        no evidence home
          #   EMPTYEV                     an evidence home with no entry
          #   EMPTYCELL <criterion>       an evidence cell that is blank
          #   HOLLOW <criterion> <cell>   an evidence cell that is only evidence-kind names
          #   NOEVCOL <heading>           a numbered table in an evidence section with no evidence column
          #   UNANSWERED <criterion>      a numbered criterion nothing answers
          #   STATS <criteria> <unnumbered>
exit:     0
```

Eight findings, and the counter is `criteria` **numbered**, not criteria **met**. So "answered
but not met" is invisible to this check by construction, and `doc:05-authoring-for-agents#checks`
already says the boundary out loud: a heading *"answers the criterion it names whatever it says
about it"*, `### Criterion 2 cannot be met as written` being its own worked example.

**Two different things are being asked, and only one of them is decidable.**

| question | decidable from repository contents? |
|---|---|
| does every numbered criterion carry an explicit verdict token from a closed set? | **yes** — check 14 already locates each criterion's answering site and would only have to require a token on it |
| is the token true? | **no** — that is a judgement about the world, and `doc:00-constitution#independent-review` §7.4's reviewer is the mechanism for it |

**And the decidable half is the shape `doc:00-constitution#observed-failing` says fails open.**
A required token grades an author's typing: whoever would write a false `met` writes it under
the check too, and the check then certifies it. It raises the floor without gating, which is
exactly the conclusion `bean:0087` reached about evidence *cells* by a different route — three
successive allowlists on the defensive-copy gate were each walked past, and `bean:0123`'s own
bypass assertion is a fourth instance in flight.

What a token does buy, and it is not nothing, is **legibility**: three beans, three phrasings,
and no way to ask the corpus how many criteria have closed unmet without reading every closed
bean. A `STATS` field for verdicts recorded would make the practice countable for the first
time. `bean:0096` rejected an `OK`-line addition on the ground that a report nobody must act on
is a report nobody reads, and that objection applies here and is not obviously decisive. This
work item weighs it; it does not presume the answer.

**A verdict token is not a defence against the failure mode that actually occurred.** In all
three instances the author wrote the unmet verdict *voluntarily and prominently*. None of them
was hiding anything, and a token requirement would have changed none of the three records. The
risk this is really about is the close that records `met` where the evidence does not carry it,
and no token reaches that.

## The gate applies no pressure, measured on `bean:0123`'s own close

`bean:0123` closes with five `### Criterion N` sub-headings already present from PR #77, so a
**bare `status:` flip** — one changed line, no closing section at all — satisfies check 14:

```
head:     3b02871 + `bean:0123`'s `status:` line, nothing else
cmd:      /bin/bash tools/docs-lint.sh > [...]/bareflip.txt
observed: docs-lint: OK — 19 documents, 111 anchors, 1669 references, 110 beans, 42 graph edges, 50 selectable, 110 bean ids, 0 introduced, 110 on origin/main, 1 closing transitions, 5 criteria checked, 4 unnumbered.
exit:     0
```

Green, at exit 0, with one of those five criteria unmet. The same was true of `bean:0093`'s
close, whose 763 lines of evidence were author discipline and not gate pressure. **A green
check 14 has never been, and does not claim to be, a statement that the criteria were met** —
so nothing that exists today would notice a close that recorded no verdict at all.

## Where the rule would go, and what it costs

Every candidate home is at or near check 8's ceiling, which is why this is a work item rather
than a line somebody adds in passing.

```
head:     3b02871, working tree clean
cmd:      /usr/bin/awk 'FNR==1 { if (NR>1) print f": "n; f=FILENAME; n=0 } { n++ } END { print f": "n }' \
            documentation/00-constitution.md documentation/05-authoring-for-agents.md \
            documentation/20-ddd-practices.md documentation/30-code-style.md \
            documentation/80-agent-operating-procedure.md > [...]/ceilings.txt
observed: documentation/00-constitution.md: 500
          documentation/05-authoring-for-agents.md: 365
          documentation/20-ddd-practices.md: 499
          documentation/30-code-style.md: 365
          documentation/80-agent-operating-procedure.md: 498
cmd:      /usr/bin/grep -n 'max_lines' documentation/README.md
observed: 112:- Line budget for `documentation/*.md`: `max_lines: 500`, `min_lines: none` (`adr:0003`).
exit:     0
```

- **`doc:00-constitution` §7.2.1** is the natural home — the lifecycle rule that already says
  when a bean closes would say what it may close with. It is at **500 of 500**, zero headroom,
  so the line costs an eviction. `bean:0120` is already queued needing a line in the same
  section for the same reason, and the two evictions should be decided once rather than twice.
- **`doc:20-ddd-practices` §7.1** is where the contradicting sentence stands. It is at **499**,
  one line of room. Changing that row is not a copy-edit: if the invariant is right the corpus
  is wrong, and if the corpus is right the row removes a rule, which `documentation/README.md`
  requires an **ADR** for.
- **`doc:05-authoring-for-agents`** has room at 365 and is the wrong document anyway — it owns
  what a check decides, and this is a lifecycle question, not a check-14 question. It is also
  `bean:0121`'s file while that work item is open.
- **`doc:80-agent-operating-procedure`** is at 498 and already carries the binary in step 6.
  Restating the lifecycle rule there would be the duplication
  `doc:05-authoring-for-agents#one-fact-one-place` forbids.

## The case against doing anything, stated as well as it can be

**Three beans in one hundred and ten is not a practice, it is three exceptions**, and every one
of them was argued at length in the open by an author who could have written `met` and did not.
The rule that actually governed all three is already written and already worked:
`doc:80-agent-operating-procedure#self-validate` forbids weakening a criterion to reach green,
and all three authors obeyed it — recording the failure *is* the compliant behaviour, and
inventing a phrasing for it is what an author does when the honest option has no name. A
repository with seventy `todo` beans does not need a seventy-first to bless what its authors are
already doing correctly. On that reading the whole finding is a naming problem, and naming
problems are cheap to leave open.

That case is answered by exactly one thing, and it is not the three beans. It is
`doc:20-ddd-practices` §7.1: a sentence that will be compiled, tested to 100% branch coverage,
and pointed at `.beans/`. Left alone it does not stay a naming problem — it becomes a
`WorkItemNotClosableException` thrown at three files that are already on `main` and that
`adr:0005-evidence-lives-in-the-work-item#finalisation` forbids editing.

## Success criteria

1. Each of the three instances is confirmed still `completed` on `main` with the verdict its
   record carries, at the head this work item runs on, and no closed bean is edited.
2. The question is **settled** in one direction, with the reason: either a bean may close with
   an unmet success criterion, or it may not. A statement that both are acceptable is not a
   settlement.
3. `doc:20-ddd-practices` §7.1's row and `doc:20-ddd-practices` §7.2's exception name are
   reconciled with whatever is settled, and if the settlement removes or weakens what that row
   states, an ADR records it (`documentation/README.md`).
4. If the practice is permitted, exactly one verdict vocabulary is stated and the three existing
   phrasings are named as the reason it is needed. A vocabulary that permits a partial verdict
   is reconciled with `doc:80-agent-operating-procedure#self-validate`'s "there is no third
   state", or that sentence is corrected.
5. Whatever `doc:00-constitution` line this needs is placed with `bean:0120`'s in one eviction
   decision, or the decision to defer is recorded with its reason.
6. A mechanical requirement is either adopted, with an observed rejection of a planted close
   that records no verdict and a control that stays silent on a close that records one
   (`doc:00-constitution#observed-failing`), or declined in writing on the ground that it grades
   typing rather than truth. Declining is a legitimate outcome and must not be reached by
   omission.
7. `./gradlew qualityCheck` green.

## Not in scope

- **Whether an evidence cell contains evidence.** That is `bean:0087`, and it is a different
  axis: a criterion can carry a perfect verbatim `test-run` and still not be met, which is what
  all three instances above are. `bean:0087`'s candidate fixes — a backtick-quoted span echoed
  elsewhere in the bean, a minimum information content — are orthogonal to a verdict token and
  neither subsumes the other. This does not fold into it.
- **Who closes a bean, and when.** That is `bean:0120`. It shares only the `doc:00-constitution`
  §7.2.1 eviction, which is why success criterion five ties the two together rather than merging
  them.
- **Re-opening or amending any of the three closed beans.** `docs-lint` check 11 forbids it in
  place, and none of the three was wrong to close as it did.
- **Building the `WorkItem` aggregate.** This settles what its invariant must say. It does not
  ask for the aggregate.

## References

`bean:0049`, `bean:0093`, `bean:0123` — the three instances.
`bean:0087` — check 14 verifies an evidence record's shape and not its content; the adjacent
axis, and the source of the argument that a required token raises a floor without gating.
`bean:0120` — nothing closes a bean after its work merges; shares the eviction and nothing else.
`bean:0121` — owns `doc:05-authoring-for-agents`'s citation-site residuals, including the
evidence-cell laundering route that makes the table at the top of this bean write "its second".
`doc:40-durability` §3.1 — `.beans/` and `domains/<domainId>/work/` are the same thing, which is
what makes `doc:20-ddd-practices` §7.1's example binding rather than illustrative.
