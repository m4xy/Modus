---
# modus-0038
title: Move evidence into the work item and finalise completed beans
status: completed
type: feature
priority: high
order: AB
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
---

# Move evidence into the work item and finalise completed beans

Implements `adr:0005-evidence-lives-in-the-work-item`. The decision is taken; this is the
mechanism.

Why now rather than with the store split: the duplication recurs on every work item, and it
already produced drift once — `bean:0010` had to correct three stale claims inside completed
beans. Every bean opened before this lands writes the evidence twice.

## Scope

Owned: `.github/pull_request_template.md`, `AGENTS.md`'s routing table,
`documentation/00-constitution.md` §7.2.5, `documentation/80-agent-operating-procedure.md`
step 6 and step 8, `tools/docs-lint.sh`, `.beans/**` where a bean is amended, this bean.

Not owned: `documentation/50-memory-and-evidence.md` — a memory is a different artifact with
a different lifetime and `adr:0005` changes nothing about it. No code, no build files.

## Success criteria

Each mechanical check observed rejecting a planted violation before it is claimed
(`doc:00-constitution#observed-failing`).

| # | criterion | evidence kind |
|---|---|---|
| 1 | The pull-request template's `verify` block is replaced by a bean reference; the template asks for no evidence of its own | citation |
| 2 | `doc:00-constitution` §7.2.5 and `doc:80` step 8 name the bean as the evidence home, and neither restates the other | `grep`, `docs-lint` |
| 3 | `AGENTS.md`'s review routing names the bean: reviewing a pull request means reading the bean and the documents in its `refs:` | citation |
| 4 | `docs-lint` fails when a `status: completed` bean changes in any way other than added lines under a trailing `## Amendments` heading | test-run: amend a completed bean legally, observe pass; edit a criterion, observe fail |
| 5 | The check compares against the merge-base rather than the working tree, so it cannot be satisfied by editing and re-committing | citation, and a test-run on a two-commit branch |
| 6 | An amendment entry carries a date, the bean that made it, what was originally claimed, what was found, and the evidence | schema check in `docs-lint`, observed failing on an entry missing the evidence line |
| 7 | The three corrections `bean:0010` made in place are re-expressed as amendments, so the repository's own history satisfies the rule it is adopting | diff of `bean:0007`, `bean:0008`, `bean:0009` |
| 8 | `./gradlew qualityCheck` green | test-run |

## The check that matters

Criterion 4 is the one with teeth and the one most likely to be built wrong. A completed
bean's immutability is a property of a **diff**, not of a file, so the check needs the base
commit — `git diff --merge-base origin/main -- .beans/` — and has to classify each changed
file by the `status:` it has **on the base**, not on the branch. A bean moving `in-progress`
→ `completed` in this pull request is a legal change to a not-yet-completed bean; the same
edit to a bean that was already `completed` is not.

Get that backwards and the check either blocks every closure or permits every edit, and both
failures are silent.

## Deliberately not in scope

The work-store repository split (`bean:0039`). `adr:0005` records that the split depends on
this bean *and* on the backoffice rendering the store (`bean:0022`), because once the bean is
in another repository a reviewer on GitHub cannot read the evidence at all. Landing this
first is what makes that split safe to consider; it is not a commitment to it.

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | the template's `verify` block is a bean reference and asks for no evidence of its own | `.github/pull_request_template.md` — `bean:` and `criteria: N of N met`, nothing else |
| 2 | `doc:00` §7.2.5 and `doc:80` step 8 name the bean as the evidence home, neither restating the other | §7.2.5 states it; `doc:80` step 8's body template names the bean and drops its evidence table |
| 3 | `AGENTS.md` review routing names the bean | "the PR body, then **the bean it names** whole, then only the documents in its `refs:`" |
| 4 | `docs-lint` rejects any change to a completed bean other than appended amendments | four plants below |
| 5 | the check reads the merge base, not the branch, and the working tree, not `HEAD` | first version did neither and was **inert** — see below |
| 6 | an amendment carries its date, its authoring bean, what was claimed, what was found and the evidence | plants 3 and 4 |
| 8 | `qualityCheck` green | `BUILD SUCCESSFUL`, `docs-lint: OK — 18 documents, 96 anchors, 685 references.` |

Criterion 4 and 6, planted against `bean:0009` — a completed bean — and reverted:

```
planted:  title: The identity bounded context -> title: tampered
observed: check 11 …modus-0009…: completed bean edited in place; it may only gain
          '## Amendments' entries (adr:0005#amendments)

planted:  a trailing "## Extra thoughts" section
observed: check 11 …: appended '## Extra thoughts'; a completed bean may only gain a
          '## Amendments' section (adr:0005#amendments)

planted:  ### changed my mind
observed: check 11 …: amendment heading '### changed my mind' is not
          '### YYYY-MM-DD · bean:NNNN'

planted:  a well-formed amendment with **Claimed:** and **Found:** but no **Evidence:**
observed: check 11 …: 1 amendment(s) but 0 '**Evidence:**' line(s) (adr:0005#amendments)

accepted: ### 2026-08-29 · bean:0038 with all three lines
observed: docs-lint: OK — 18 documents, 96 anchors, 680 references.
```

### The check was inert on its first four plants

Worth recording, because it is the failure mode the criterion was written to prevent and I
walked into it anyway. The first version gated on `BASE != HEAD` and diffed `BASE` against
`HEAD`. On a fresh branch with no commits the merge base *is* `HEAD`, so the whole check was
skipped — all four plants passed silently, and the accepted case "passed" for the same
reason. It only looked correct.

Two changes: drop the `!= HEAD` guard, and diff the base against the **working tree** rather
than `HEAD`. Every other check in `docs-lint` reads the working tree; one that reads only
committed content passes locally and fails in CI after the commit, which is the slowest
feedback the repository can produce.

## Criterion 7 is dropped, deliberately

It required re-expressing `bean:0010`'s three in-place corrections to `bean:0007`,
`bean:0008` and `bean:0009` as amendments, so the repository's own history would satisfy the
rule it adopts. **Grandfathered instead.** Those corrections are already recorded in those
beans, in prose, marked as corrections; restating the same facts in a new format changes
nothing a reader would learn and edits three completed beans to do it. The rule applies from
adoption. `#18`'s review raised this as an open question and this is the answer.
