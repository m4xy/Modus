---
id: adr:0005-evidence-lives-in-the-work-item
title: Evidence lives in the work item; the pull request carries a reference
status: active
superseded_by: null
read_when:
  - path: .beans/**
  - path: .github/pull_request_template.md
  - task: evidence|work item|bean|pr body|amend|finalis|immutab|record a (conclusion|finding)
provides:
  - adr:0005-evidence-lives-in-the-work-item#evidence-home
  - adr:0005-evidence-lives-in-the-work-item#finalisation
  - adr:0005-evidence-lives-in-the-work-item#amendments
depends_on: [doc:00-constitution, doc:05-authoring-for-agents, doc:50-memory-and-evidence]
---

# ADR 0005 — Evidence lives in the work item; the pull request carries a reference

- **Date:** 2026-08-29
- **Deciders:** Modus core
- **Supersedes:** —

## Context

`doc:00-constitution#evidence-rule` requires every claim to carry its evidence, and
`.github/pull_request_template.md`'s `verify` block requires one evidence block per success
criterion in the pull-request body. `doc:80-agent-operating-procedure` step 6 separately
requires the evidence to be attached to the criterion **on the work item**. Both are
followed, so the evidence is written twice.

Measured on `bean:0030`, whose pull request and work item were authored in the same session:
five distinct observed-output strings appear verbatim in both — `Unexpected elements from
index 1`, `expected:<2> but was:<0>`, `branches covered ratio is 0.6`,
`Collections.singleton`, `getProbe-OlfN_Ag`. That is `doc:05-authoring-for-agents#one-fact-one-place`
violated in the two artifacts whose purpose is being the single source of truth, and it is
the shape of drift `bean:0010` had to correct once already: three claims in `bean:0007`,
`bean:0008` and `bean:0009` had gone stale against what merged.

The duplication is not an authoring slip. It follows from two documents each naming a
different home for the same fact, so it recurs on every work item until one of them yields.

## Decision <a id="evidence-home"></a>

**The work item is the evidence record. The pull-request body carries a reference to it and
no evidence of its own.**

1. Every success criterion's evidence — command, expectation, verbatim observed output —
   lives in the bean, beside the criterion it satisfies.
2. The pull-request body's `verify` block is replaced by the bean reference. A reviewer
   reads the bean, which `doc:80-agent-operating-procedure` step 1 already names as the one
   thing read whole.
3. `AGENTS.md`'s review routing gains the bean: reviewing a pull request means reading the
   bean and the documents in the pull request's `refs:`.

**Why this direction rather than the other.** Putting the evidence in the pull-request body
and reducing the bean to workflow state would also remove the duplication, and it is the
easier change. It was rejected: the body of a pull request lives in GitHub, which Modus
neither controls nor can replay, and `adr:0002-flat-file-over-database` makes the filesystem
the source of truth for exactly this class of record. Evidence that exists only in a hosted
review tool is evidence Modus cannot re-read, migrate, or serve.

It also puts the record where the product model already says it belongs. `.beans/` **is** the
`modus` domain's work store (`doc:00-constitution` §7.2.1), so evidence about work on the
product living in the product's domain store — citing the code by immutable commit — is what
self-hosting means rather than a filing convention.

## Finalisation <a id="finalisation"></a>

**A bean with `status: completed` is immutable.** Its criteria, its evidence and its summary
are the durable record of what was required and what was observed, and a record that can be
edited after the fact is not evidence (`doc:00-constitution#evidence-rule`).

## Amendments <a id="amendments"></a>

Finalisation conflicts with practice already used in this repository, and the conflict is
resolved here rather than left to be discovered. `bean:0010` re-read `bean:0007`,
`bean:0008` and `bean:0009` against `main`, found three claims that had drifted during their
review cycles, and **corrected them in place** inside beans that were already `completed`.
That was the right thing to do and it would be forbidden by the rule above.

So the rule is append-only rather than absolute:

- A completed bean may gain entries under a trailing `## Amendments` section, and may change
  in no other way. Every entry is dated and names the bean that made it.
- An amendment states what the original claimed, what was found to be true, and the evidence
  for the correction. It never edits the original text — a reader must be able to see both
  what was believed and what was found.
- Nothing else about a completed bean changes: not `status`, not a criterion, not an
  evidence block, not a word of the summary.

This keeps the whole record of one work item in one file, which is what the next agent reads,
rather than fragmenting it across superseding files the way `adr:0001-record-architecture-decisions`
does for decisions. A decision is superseded; an observation is amended.

## Consequences

### Positive

- One home for one fact, so the two artifacts can no longer disagree.
- The evidence is a flat file under version control, per `adr:0002`, rather than a row in a
  hosted database.
- A completed bean becomes citable: `bean:0009`'s evidence can be referenced by a later work
  item and will still say the same thing.
- The pull-request body shrinks to what is genuinely about the change — contract, decisions,
  review focus — which is what a reviewer's attention is for.

### Negative

- **A reviewer on GitHub can no longer read the evidence in the pull request.** Today the
  bean is in the diff, so this costs nothing. Once the work store is a separate repository it
  costs a great deal, and the split therefore depends on the backoffice being able to render
  the store for a human (`bean:0022`). Splitting before that is splitting the evidence away
  from the only place anyone reads it.
- Append-only amendment is weaker than immutability, and the weakness is load-bearing: a
  wrong original claim stays visible forever beside its correction. That is the intent, and
  it makes a bean longer over time.
- `docs-lint`'s line budget does not apply to beans today. If it ever does, amendments push
  against it.

### Neutral

- Nothing changes about `doc:50-memory-and-evidence`. A memory is domain-scoped and carries
  its own evidence records; this ADR is about the work item, which is a different artifact
  with a different lifetime.

## Alternatives considered

| alternative | rejected because |
|---|---|
| Evidence in the pull-request body; the bean holds workflow state only | the durable record would live in a hosted tool Modus cannot replay or serve, contradicting `adr:0002`. It is the easier change and it trades the constitution's evidence rule for GitHub's uptime |
| Evidence in both, with a mechanical check that they match | two copies with a diff check is still two copies; the check would have to compare free text, and the failure mode it prevents — drift — is the one `doc:05#one-fact-one-place` already forbids by construction |
| Completed beans strictly immutable, corrections in a new superseding bean | fragments one work item's record across files. `doc:80` step 1 has the next agent read the work item whole; a record assembled from three files is not read whole, it is skimmed |
| Completed beans freely editable, as today | an evidence record that can be silently edited after the fact is not evidence. `bean:0010`'s corrections were legitimate precisely because they were marked as corrections |
