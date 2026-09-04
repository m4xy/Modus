---
# modus-0128
title: Three beans have closed a criterion unmet, in three phrasings, against a step-6 MUST that says there is no third state
status: todo
type: fix
priority: low
created_at: 2026-09-04T00:00:00Z
# `low`, lowered from `normal` in review. An earlier revision of this bean argued that a
# built `WorkItem` aggregate would refuse to load three files already on `main`, and
# priced the finding at that. The argument was wrong — see "The rule is in `doc:80`, and
# the rest are examples" — and with it withdrawn the counter-argument stated at the end of
# this bean wins on the remaining framing. What survives is real and is documentation
# hygiene: a numbered MUST, three closes that record otherwise, and no name for the
# honest verdict.
# No `order`. Absence is a defined position — `AGENTS.md` step 1 sorts an unordered bean
# after every ordered one and check 12 does not flag it — and this bean has no claim on
# being reached before the ordered work already queued.
---

# Three beans have closed a criterion unmet, in three phrasings, against a step-6 MUST that says there is no third state

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
**That is wrong, and the correction is the finding.** The statement that decides it was inside
that search's own scope all along — it is in `doc:80` itself — and asking the whole package
returns it beside statements in `doc:20`, `doc:30` and `doc:70` that are not rules at all.

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

### What each statement is, named rather than counted

Named rather than counted deliberately: an earlier revision of this section wrote a total above
the list and a different total below it, which is the failure this bean's own evidence standard
exists to prevent. Each is identified below by where it sits, so the list and the prose around
it cannot disagree about how many there are.

- **`doc:80-agent-operating-procedure#self-validate`, step 6 point 4** — *"A criterion with no
  evidence is **unmet**. There is no third state."* Item 4 of a numbered MUST list in the step
  an agent runs when it closes a bean, between "attach it to the criterion on the work item"
  and "once the bean is `completed` it is final". **This is the finding, and it is the only one
  of them that is normative.** Its first clause is scoped to the *absence of evidence*, and
  none of the three instances is that case — each carries evidence. Its second clause is not so
  scoped: *"there is no third state"* is a statement about the verdict space itself, and the
  step it closes is the step that closes beans. `met in part` is precisely a third state, it is
  on `main`, and it is there in three spellings.
- **`doc:20-ddd-practices` §7.1** — *"A work item may not close with an unmet success
  criterion"*, sitting in the **"Example"** column of a table whose subject is which layer an
  invariant belongs in. The normative content of that row is "Aggregate method, before
  mutation"; the sentence is what such an invariant looks like.
- **`doc:20-ddd-practices` §7.2** — `WorkItemNotClosableException(unmetCriteria)`, an
  illustrative name inside a bullet whose subject is choosing between `require`, `check` and a
  named domain exception.
- **`doc:30-code-style` §7** — `"refuses to close a work item with an unmet success criterion"`,
  in the **"Detail"** cell of a "Test names" row whose rule is *"Full sentences describing
  behaviour"*. The rule is the sentence shape; the example is arbitrary.
- **`doc:70-skills` §5.1** — *"Report a binary verdict: `passed` or `failed`. Never 'mostly
  done'. Partial completion is `failed` with a list of unmet criteria."* Its stated subject is
  the first line of that section: *"A skill invocation, once started, MUST"*. It governs what a
  skill run reports about **itself**, not what verdict a bean's record may carry — see below.

The `doc:20` row, the `doc:20` exception name and the `doc:30` test name are examples of an
aggregate that does not exist:

```
head:     3b02871, working tree clean
cmd:      /usr/bin/grep -rn "WorkItemNotClosable" . --exclude-dir=.git
observed: ./documentation/20-ddd-practices.md:401:  surface: `WorkItemNotClosableException(unmetCriteria)`. These extend a sealed
exit:     1
```

One hit, in the document that names it as an example. No `WorkItem` aggregate, no exception, no
test. The invariant is unbuilt, so nothing has ever been observed rejecting a close — which is
`doc:00-constitution#observed-failing`'s definition of a claim rather than enforcement.

### `doc:70-skills` §5.1 does not govern a bean's close

Worth stating rather than leaving on the list, because a skill in this repository does close
beans: `modus-work-package`'s procedure step 11 is *"After merge, close the bean"*. Even so,
§5.1's verdict is the **invocation's** verdict against the **skill's** success criteria — the
five checkboxes in that skill's own `## Success criteria` — and one of those reads "every
criterion in the bean has evidence beside it". All three instances satisfy it: each carries
evidence, and the evidence is what establishes the criterion unmet. The skill would report
`passed` on every one of them. §5.1 is not the rule being broken, and listing it as though it
were inflates the finding.

What §5.1 does carry is point 8's *"Never modify the validation command or the success criteria
to make itself pass"*, which `doc:80` step 6 imports by reference for every agent, skill or not.
All three authors obeyed it. That is the point of the counter-argument at the end of this bean.

## The rule is in `doc:80`, and the rest are examples

An earlier revision of this bean argued that `doc:20` §7.1's row is binding on `.beans/`
through `doc:40-durability` §3.1, and that a `WorkItem` aggregate built as written would throw
`WorkItemNotClosableException` at three files already on `main`, so **Modus could not
self-host its own work store**. **That argument was wrong on three independent grounds and is
withdrawn.** It is recorded here rather than deleted because it is what made this bean look
architectural, and because a reader coming from the commit that raised it needs to find the
retraction.

```
head:     documentation/ and origin/main both at 3b02871; this change touches neither
cmd:      /usr/bin/grep -n 'no idea it is stored in Markdown\|on-disk shape is allowed to differ\|adapter owns the translation' \
            documentation/20-ddd-practices.md > [...]/cit-doc20-9.txt
observed: 474:The domain has no idea it is stored in Markdown. This is not incidental — it is the point.
          484:- Conversely, the on-disk shape is allowed to differ from the aggregate shape. The
          485:  adapter owns the translation and owns its tests.
exit:     0

cmd:      /usr/bin/grep -n 'are the same thing\|is identical in both places\|no migration and no import step\|directory move, not a design change' \
            documentation/40-durability.md > [...]/cit-doc40-31.txt
observed: 150:### 3.1 `.beans/` and `domains/<domainId>/work/` are the same thing
          157:- The **schema** is identical in both places (the upstream `hmans/beans` convention; see
          163:  manages, with no migration and no import step. That is the point of picking one shape:
          167:a directory move, not a design change.
exit:     0

cmd:      git grep -n -e '^title:' -e '^status:' -e '^priority:' origin/main -- '.beans/modus-0067--*.md'
observed: origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:3:title: The .beans file to WorkItem mapper
          origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:4:status: todo
          origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:6:priority: high
exit:     0

cmd:      git grep -n -A1 'Front-matter keys present across the corpus today' origin/main \
            -- '.beans/modus-0067--*.md'
observed: origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:50:Front-matter keys present across the corpus today: `title`, `status`, `type`, `priority`,
          origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md-51-`order`, `parent`, `blocked_by`, `created_at`, `updated_at`. `blocked_by` is a YAML flow
exit:     0

cmd:      git grep -n -E '^\| (6|10) \|' origin/main -- '.beans/modus-0067--*.md'
observed: origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:118:| 6 | Reading every bean file on disk succeeds, and the count of files parsed equals the count of files present — a mapper that parses a subset and reports success has examined less than it claims (`doc:00-constitution#observed-failing`) | |
          origin/main:.beans/modus-0067--beans-file-to-workitem-mapper.md:122:| 10 | Fixtures vary rather than repeat: at least one bean with no `order`, one with none of the optional keys, one with a multi-element `blocked_by`, one `completed` and one `todo` (`doc:35-testing#fixture-variation`) | |
exit:     0

head:     this change's tree
control:  bean:0067's key list is a claim about the corpus, so the corpus is asked directly —
          every top-level front-matter key of every bean file present
cmd:      /usr/bin/awk 'FNR==1 { st = ($0 == "---") ? 1 : 0; next } st==1 && $0=="---" { st=2; next } \
            st==1 && /^[a-z_]+:/ { k=$0; sub(/:.*/,"",k); print k }' .beans/*.md | sort | uniq -c | sort -rn
observed:  111 type
           111 title
           111 status
           111 created_at
           109 priority
            36 order
            30 blocked_by
            28 updated_at
            17 parent
exit:     0
```

The keys `bean:0067` enumerates, and no other. Nothing on disk records a criterion, let alone a
verdict against one: the success criteria live in the Markdown **body**, which `bean:0067`
requires to survive a read byte for byte and does not ask the mapper to parse.

**`doc:40-durability` §3.1's subject is two directories, not a file and an aggregate.** Its
heading is *"`.beans/` and `domains/<domainId>/work/` are the same thing"*; what it says is
identical in both places is the **schema**; its *"no migration and no import step"* is about
self-hosting not requiring a conversion when the store root moves, and it closes with *"a
directory move, not a design change."* Nothing in it says how a file becomes an aggregate, and
nothing in it is addressed to `doc:20` at all.

**`doc:20-ddd-practices` §9 says the opposite, four sections below the row, in the same
document.** *"The domain has no idea it is stored in Markdown. This is not incidental — it is
the point."* and *"the on-disk shape is allowed to differ from the aggregate shape. The adapter
owns the translation and owns its tests."* No aggregate loads `.beans/`. An adapter does, and
what it hands inward is whatever it chose to translate.

**`bean:0067` is the import step §3.1 was read as denying, and it is on `main` at
`priority: high`.** *"The `.beans` file to WorkItem mapper"* — the adapter §9 describes, placed
in `adapters/adapter-persistence-flatfile`. Its contract enumerates the keys it maps: `title`,
`status`, `type`, `priority`, `order`, `parent`, `blocked_by`, `created_at`, `updated_at`. A
criterion's verdict is not among them and is not a front-matter key anywhere in the corpus, so
`unmetCriteria` has no source in the on-disk record and §7.1's invariant cannot be evaluated
against `.beans/` at all. `bean:0067` also requires that reading **every** bean file on disk
succeeds, and that its fixtures include a `completed` bean — it is specified to load exactly
the records the withdrawn argument said an aggregate would refuse.

**And the invariant fires on a transition, not on a state.** The row's own middle column is
*"Aggregate method, before mutation"*: it guards a `close()` call. Reading a bean that is
already `completed` is reconstitution, which calls no aggregate method and evaluates no
invariant. Three closed files on disk are not three violations waiting to be thrown at.

What follows from that is demotion, in both directions. `doc:20` §7.3's 100% `BRANCH` floor is a
package-scoped Jacoco ratio over `..domain.aggregate` — a testing floor on the branches a test
suite must reach, saying nothing about which production inputs reach an invariant — so it adds
nothing here either. And the cost of leaving this unsettled is not architectural. It is that
`doc:80` states a MUST, three records on `main` state otherwise, and the next author who
reaches the same honest verdict has no name for it and will invent a fourth phrasing.

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

Green, at exit 0, with one of those five criteria unmet. That `50 selectable` is the bare-flip
arm and not this change's tree: the tree this bean is raised in reads `51 selectable`, because
this bean is itself an unblocked `todo` with no `blocked_by`. Both figures and the difference
between them are recorded in `bean:0123`'s Block E and beside its `qualityCheck` run; neither
is re-derived here. The same was true of `bean:0093`'s close,
whose 763 lines of evidence were author discipline and not gate pressure. **A green check 14
has never been, and does not claim to be, a statement that the criteria were met** — so nothing
that exists today would notice a close that recorded no verdict at all.

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

- **`doc:80-agent-operating-procedure` step 6** is where the rule already is, at **498 of 500**.
  If the settlement affirms "there is no third state", nothing is added here and the vocabulary
  goes elsewhere. If it permits a partial verdict, that sentence has to change, and two lines is
  the room there is to change it in.
- **`doc:00-constitution` §7.2.1** is where a lifecycle rule would go — the section that already
  says when a bean closes would say what it may close with. It is at **500 of 500**, zero
  headroom, so the line costs an eviction. `bean:0120` is already queued needing a line in the
  same section for the same reason, and the two evictions should be decided once rather than
  twice.
- **`doc:20-ddd-practices` §7.1** holds an example, not the rule, and this bean no longer asks
  for it to be rewritten on its own account. It is at **499**. If the settlement makes the
  example misleading, changing it is a copy-edit to a worked example — but if it is judged to
  state a rule rather than illustrate one, removing it needs an **ADR**
  (`documentation/README.md`), and that judgement belongs to the settlement, not to this
  section.
- **`doc:05-authoring-for-agents`** has room at 365 and is the wrong document anyway — it owns
  what a check decides, and this is a lifecycle question, not a check-14 question. It is also
  `bean:0121`'s file while that work item is open.

## The case against doing anything, stated as well as it can be, and it is now the stronger case

**Three beans in the hundred and ten `main` carried at `3b02871` is not a practice, it is three
exceptions**, and every one of them was argued at length in the open by an author who could have
written `met` and did not. The rule that actually governed all three is already written and
already worked: `doc:80-agent-operating-procedure#self-validate` forbids weakening a criterion
to reach green, and all three authors obeyed it — recording the failure *is* the compliant
behaviour, and inventing a phrasing for it is what an author does when the honest option has no
name. A repository with seventy `todo` beans at that head does not need a seventy-first to bless
what its authors are already doing correctly. On that reading the whole finding is a naming
problem, and naming problems are cheap to leave open.

```
head:     3b02871
cmd:      git grep -l "^status: todo" 3b02871 -- .beans | wc -l
observed:       70
exit:     0
cmd:      git ls-tree -r --name-only 3b02871 .beans | wc -l
observed:      110
exit:     0
```

**That case is no longer answered by an architectural cost, because there is not one.** What
answers it, and it is weaker, is that `doc:80` step 6 point 4 is a numbered MUST rather than an
example, and a MUST with three recorded departures and no amendment is a document that has
stopped describing the repository. Left alone it is not a naming problem that stays contained:
the fourth author reaches the same fork, finds the same silence, and writes a fourth phrasing,
and by then the question of *which* of the four the corpus meant is unanswerable from the
corpus. That is the whole of the case for acting, stated at its real weight. It is why this bean
is `priority: low` and carries no `order`, and it is why it declines to settle anything itself.

## Success criteria

1. Each of the three instances is confirmed still `completed` on `main` with the verdict its
   record carries, at the head this work item runs on, and no closed bean is edited.
2. The question is **settled** in one direction, with the reason: either a bean may close with
   an unmet success criterion, or it may not. A statement that both are acceptable is not a
   settlement.
3. `doc:80-agent-operating-procedure#self-validate`'s "There is no third state" is either
   affirmed as written or corrected in place. If the settlement removes or weakens it, an ADR
   records that (`documentation/README.md`) — it is a numbered MUST in a step every agent runs,
   and this work item rests on nothing else.
4. If the practice is permitted, exactly one verdict vocabulary is stated and the three existing
   phrasings are named as the reason it is needed.
5. `doc:20-ddd-practices` §7.1's row and §7.2's exception name are reconciled with whatever is
   settled **as the worked examples they are**, and not as a constraint `.beans/` is under. If
   either is instead judged to state a rule and is then removed or weakened, an ADR records that
   as well (`documentation/README.md`).
6. Whatever `doc:00-constitution` line this needs is placed with `bean:0120`'s in one eviction
   decision, or the decision to defer is recorded with its reason.
7. A mechanical requirement is either adopted, with an observed rejection of a planted close
   that records no verdict and a control that stays silent on a close that records one
   (`doc:00-constitution#observed-failing`), or declined in writing on the ground that it grades
   typing rather than truth. Declining is a legitimate outcome and must not be reached by
   omission.
8. `./gradlew qualityCheck` green.

## Not in scope

- **Whether an evidence cell contains evidence.** That is `bean:0087`, and it is a different
  axis: a criterion can carry a perfect verbatim `test-run` and still not be met, which is what
  all three instances above are. `bean:0087`'s candidate fixes — a backtick-quoted span echoed
  elsewhere in the bean, a minimum information content — are orthogonal to a verdict token and
  neither subsumes the other. This does not fold into it.
- **Who closes a bean, and when.** That is `bean:0120`. It shares only the `doc:00-constitution`
  §7.2.1 eviction, which is why success criterion six ties the two together rather than merging
  them.
- **Re-opening or amending any of the three closed beans.** `docs-lint` check 11 forbids it in
  place, and none of the three was wrong to close as it did.
- **Building the `WorkItem` aggregate or its mapper.** `bean:0013` owns the aggregate and
  `bean:0067` owns the flat-file mapping. This settles a vocabulary; it asks for neither, and
  after the retraction above it makes no claim about what either will do.

## References

`bean:0049`, `bean:0093`, `bean:0123` — the three instances.
`bean:0067` — the `.beans` file to `WorkItem` mapper: the import step, on `main` at
`priority: high`, whose enumerated keys carry no criterion verdict.
`bean:0087` — check 14 verifies an evidence record's shape and not its content; the adjacent
axis, and the source of the argument that a required token raises a floor without gating.
`bean:0120` — nothing closes a bean after its work merges; shares the eviction and nothing else.
`bean:0121` — owns `doc:05-authoring-for-agents`'s citation-site residuals, including the
evidence-cell laundering route that makes the table at the top of this bean write "its second".
`doc:20-ddd-practices` §9 — the domain has no idea it is stored in Markdown, and the on-disk
shape may differ from the aggregate shape; the reason §7.1's row is not binding on `.beans/`.
`doc:80-agent-operating-procedure#self-validate` — step 6 point 4, the one normative statement
this bean rests on.
