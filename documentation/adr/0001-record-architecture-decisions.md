# ADR 0001 — Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-08-28
- **Deciders:** Modus core
- **Supersedes:** —
- **Superseded by:** —

## Context

Modus is built primarily by agents. An agent starting a work item has no institutional
memory, no corridor conversation, and no recollection of why a boundary was drawn where it
was. It has only what is written down.

Undocumented decisions cost more here than in a human-only codebase:

- An agent re-derives the reasoning from scratch on every encounter. That is real money
  (`documentation/60-cost-model.md`) and it produces inconsistent conclusions across runs.
- An agent that cannot find the reason for a constraint will eventually treat it as
  accidental and refactor it away — confidently, and with a green build if the constraint
  was never mechanised.
- Reversing a decision safely requires knowing what was traded away. Without that, a
  reversal repeats a mistake that was already made and already understood once.

Git history is not a substitute. It records *what* changed and rarely *why*, the rationale
is scattered across many commits, and searching it is expensive in tokens.

We already have two adjacent mechanisms, and neither fits:

- **`documentation/`** states how the repository works *now*. It is deliberately
  present-tense and prescriptive; it carries no alternatives and no history.
- **Memories** (`documentation/50-memory-and-evidence.md`) are evidence-backed facts about
  a domain, scoped and invalidatable. A design decision is not a discovered fact — it is a
  choice among options, and it does not become false when the world changes; it becomes
  *superseded*.

There is a third thing — a **choice with alternatives and consequences** — and it needs its
own home.

## Decision

**We record architecturally significant decisions as Architecture Decision Records in
`documentation/adr/`, in the standard Status / Context / Decision / Consequences format.**

1. **Location and naming.** `documentation/adr/NNNN-kebab-case-title.md`, `NNNN`
   zero-padded and monotonically increasing. Numbers are never reused.

2. **Format.** The header block above (Status, Date, Deciders, Supersedes, Superseded by)
   followed by four sections: **Context**, **Decision**, **Consequences**, and
   **Alternatives considered**. The Decision section is written in the present imperative
   ("We store state as files"), not the past or the conditional.

3. **Immutability.** An accepted ADR is never edited except to change its `Status` and its
   `Superseded by` link. A changed mind produces a **new ADR** that supersedes the old one
   and explains what changed. The wrong decision, and the reasoning that produced it, stays
   readable — that record is often more instructive than the correction.

4. **Statuses.** `Proposed` → `Accepted` → (`Superseded` | `Deprecated`). `Rejected` is a
   valid terminal state and rejected ADRs are kept: "we considered X and did not do it" is
   exactly the question that recurs.

5. **What is architecturally significant.** An ADR is **required** for:
   - adding, removing, or renaming a Gradle module;
   - adding or removing a bounded context;
   - adding a non-domain-scoped API route;
   - changing the storage model, the durability guarantees, or the concurrency model;
   - adding a runtime dependency that becomes hard to remove (a database, a broker, a
     framework);
   - changing a cross-context domain-event contract;
   - removing or weakening a rule in `00-constitution.md`;
   - choosing between two viable approaches where the loser has real merit.

   An ADR is **not** required for: a new aggregate, a new port, a new adapter
   implementation of an existing port, a new endpoint under an existing resource, or any
   ordinary feature work. Those need a work item, not a record of a choice.

6. **Discovery.** ADRs are linked from the rules they justify. `00-constitution.md` §2
   links `0002`; every future rule with a contested rationale links its ADR. An agent
   encountering a constraint can reach its reasoning in one hop, without reading the ADR
   log.

7. **Process.** An ADR follows the ordinary workflow (`00-constitution.md` §7): work item,
   branch, pull request, review, merge. The ADR is reviewed as the primary artefact — the
   review is of the reasoning, not the prose.

8. **Length.** One to three pages. An ADR longer than that is a design document with an
   ADR hidden inside it; extract the decision and link the rest.

## Consequences

### Positive

- An agent can answer "why is it like this?" in one targeted read, rather than
  reconstructing it from git history at a cost of tens of thousands of tokens.
- Constraints stop looking accidental, so they stop being refactored away by accident.
- Reversals are informed: the consequences section states what was traded, so a reversal
  starts from the known cost rather than rediscovering it.
- Review has a natural home for design argument, separate from code review — which keeps
  code review focused on correctness (`00-constitution.md` §7.4).
- ADRs are ordinary Markdown files in the flat-file store, so they diff, review, version,
  and render through exactly the same machinery as everything else.
- When Modus self-hosts, its own decision log is already in the format its product
  consumes.

### Negative

- Writing an ADR costs time and tokens up front, and some of them will never be read.
- The "is this architecturally significant?" judgement will sometimes be wrong in both
  directions: trivia recorded as ADRs, and significant choices merged without one. Point 5
  narrows this but cannot eliminate it.
- An immutable log accumulates superseded entries. Mitigated by the `Superseded by` links
  and by loading only `Accepted` records by default.
- ADRs can drift from the code they describe. Mitigated by point 6 — a rule links its ADR,
  so changing the rule surfaces the ADR in the same diff.

### Neutral

- The `documentation/adr/` directory grows monotonically. At Modus's scale this is
  measured in kilobytes.

## Alternatives considered

| Alternative | Why not |
|---|---|
| **Rely on git history and PR descriptions** | Records what, not why. Rationale is scattered and expensive to search — an agent would burn its context budget reconstructing one decision. PR descriptions also disappear behind a network call, so they are unavailable to an offline agent. |
| **A single `DECISIONS.md`** | Grows without bound and must be loaded whole to find one entry, which is precisely the context-budget failure mode `00-constitution.md` §6 exists to prevent. It also invites in-place editing, which destroys the history of superseded reasoning. |
| **Put rationale in code comments** | Comments explain *why this line*, not *why this architecture*. Cross-cutting decisions have no single home, and comments are deleted with the code they annotate — losing the reasoning exactly when it becomes relevant. |
| **Store decisions as evidence-backed memories** | Category error. A memory is a discovered fact that can be shown false and invalidated (`50-memory-and-evidence.md` §6). A decision is a choice among options; it is not falsifiable, it is superseded. Conflating them would corrupt the invalidation semantics that make memories trustworthy. |
| **A wiki or an external tool** | Violates flat-file-first (`00-constitution.md` §2). Decisions would live outside the repository, outside review, outside git, and outside an agent's reach without a network call. |
| **Full RFC process with a template of a dozen sections** | Too heavy for this repository's pace. The four-section ADR captures the reusable part — the choice, the reason, and the cost — at roughly a tenth of the effort. |
