---
# modus-0048
title: Reconcile ADR immutability with a decision that defers a detail
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Reconcile ADR immutability with a decision that defers a detail

`adr:0001-record-architecture-decisions` §3: "An accepted ADR is never edited except to
change its `Status` and its `Superseded by` link. A changed mind produces a **new ADR** that
supersedes the old one."

`adr:0006-framework-boundary#classification` classified `doc:10-architecture` at section
granularity and stated in the same section that the split was **deferred to the work item
that writes the tier-1 surface**. `bean:0041` then performed the split, and its success
criteria required that classification to be updated to name files. Two ratified rules, one
change, and no procedure that satisfies both:

| option | why it is wrong |
|---|---|
| edit the two classification rows | violates `adr:0001` §3 as written |
| write a superseding ADR | the decision did not change; only the address of the classified content did. It would also point four live `adr:0006-…#…` references at a superseded document |
| leave the rows naming sections | the ADR would carry a normative table whose targets do not exist — a dangling rule, which is the failure `doc:05-authoring-for-agents#one-fact-one-place` is about |

`bean:0041` took the first option and recorded it. This bean decides the rule.

Success criteria:

- `adr:0001` §3 states, or explicitly refuses, an exception for the case where a decision
  names a work item that will make one of its own statements concrete. The exception, if
  granted, is bounded to that statement and decidable without judgement about intent.
- The recursion is addressed: `adr:0001` governs its own editing, so whatever mechanism
  changes it is the mechanism the rule then permits.
- Whether `docs-lint` can check it at all is answered either way. A rule about which parts
  of an ADR may change is a diff-shaped rule and `docs-lint` check 11 already implements one
  for beans (`adr:0005-evidence-lives-in-the-work-item#finalisation`), so the machinery
  exists; whether an ADR should carry it is the open question.
