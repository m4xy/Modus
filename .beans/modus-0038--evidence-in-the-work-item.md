---
# modus-0038
title: Move evidence into the work item and finalise completed beans
status: todo
type: feature
priority: high
order: AB
created_at: 2026-08-29T00:00:00Z
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
