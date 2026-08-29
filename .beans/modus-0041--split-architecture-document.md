---
# modus-0041
title: Split doc:10-architecture along its tier seam
status: in-progress
type: task
priority: high
order: AGM
created_at: 2026-08-29T00:00:00Z
parent: modus-0040
---

# Split `doc:10-architecture` along its tier seam

`adr:0006-framework-boundary#classification` classifies this document at **section**
granularity because it contains both tiers: §3.1, §4.1, §5 and §7 are the extension contract
a third-party Module author must obey; §2, §4.2, §4.3, §6 and §8 are this repository's own
layout and internal rules. Until it is split, the tier is a property of a passage rather than
a file, which no check can read.

It is also at 482 of `adr:0003-document-line-budget`'s 500-line ceiling, so the split is due
on length regardless.

Success criteria:

- Two documents, each wholly one tier, with `read_when` predicates that select the right one:
  a Module author's task must not select this repository's package rules, and a `core/` change
  must not select the extension contract.
- Every `provides` anchor keeps resolving. 96 anchors and 614 references exist repo-wide; a
  split that breaks one is `docs-lint` check 4 or 6 failing, so this is mechanically verified
  rather than reviewed.
- The tier-1 half states the Module contract completely enough to write a Module against
  without reading the tier-2 half. That is the criterion that decides whether the seam was cut
  in the right place, and it is checked by reading it as that author, not by counting sections.
- `adr:0006`'s classification table is updated to name files rather than sections, and
  the tier table — `doc:15-repository-layout#tiers` §2.2, which this criterion addressed
  under its pre-split owner — is corrected with it.

## Restated criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Two documents, each wholly one tier, and the `read_when` predicates select disjointly: a Module author's task selects the extension contract and not this repository's package rules; a `core/` change selects the package rules and not the extension contract | test-run of the `doc:05-authoring-for-agents#read-when` selection algorithm over the real front-matter |
| 2 | Every `provides` anchor still resolves, and no reference anywhere in the repository is left dangling | `docs-lint` green, plus checks 4 and 6 observed rejecting a planted bad split |
| 3 | The tier-1 half states the Module contract completely enough to write a Module against without opening the tier-2 half | citation: every reference the tier-1 half makes into the tier-2 half, classified as rule or rationale |
| 4 | No rule is stated in both halves, and none is lost | test-run: every non-trivial line of the pre-split body appears exactly once across the pair |
| 5 | `adr:0006`'s classification names files; the tier table agrees with it | citation |
| 6 | `./gradlew qualityCheck` green | test-run |

## Which half keeps `doc:10-architecture` — decided by the check, not by taste

The seam is `adr:0006-framework-boundary#classification`; the *direction* of the cut is not
free. Four `status: completed` beans reference anchors of this document, and a completed bean
may not be edited (`docs-lint` check 11, `adr:0005-evidence-lives-in-the-work-item#amendments`).
An anchor that leaves `documentation/10-architecture.md` breaks check 6 in a file that cannot
be fixed:

```
cmd:      grep -l 'doc:10-architecture#' .beans/*.md | xargs grep -H '^status:' | grep completed
observed: .beans/modus-0009--identity-bounded-context.md:status: completed
          .beans/modus-0010--lifecycle-handoff.md:status: completed
          .beans/modus-0030--domainmgmt-domain-aggregate.md:status: completed
          .beans/modus-0032--domain-id-shared-kernel.md:status: completed
          — between them: #bounded-contexts, #domain-root-convention, #module-system
```

All three of those anchors are tier-1 content, so `doc:10-architecture` is the **tier-1**
half and the new `doc:15-repository-layout` is the tier-2 half. The alternative — tier 2
keeping the id — is unreachable, not merely unattractive.

Section numbers are not reallocated for the same reason, one level down: beans and ADRs cite
sections by number, and `bean:0001`, `bean:0009` and `bean:0030` are completed beans citing
§3.1, §4.1, §4.2 and §5.5. `doc:10-architecture` therefore keeps §1, §3, §4.1, §5 and §7 and
`doc:15-repository-layout` keeps §2, §4.2, §4.3, §6, §8 and §9, gaps and all. The rule is
stated once, in `doc:README#changing-this-package`.

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | the predicates select disjointly | selection run below — Module author selects `doc:10-architecture` only; `core/` and `adapters/` changes select `doc:15-repository-layout` only |
| 2 | every anchor and reference resolves | `docs-lint: OK — 19 documents, 100 anchors, 741 references.` (was 18 / 96 / 685) |
| 2 | a broken split is caught, not reviewed | two plants below, checks 6 and 4 |
| 3 | the tier-1 half is self-sufficient | five references point from `doc:10-architecture`'s body into `doc:15-repository-layout`; none is a rule a Module author must obey — enumerated below |
| 4 | nothing restated, nothing lost | conservation run below: 21 changed lines, all of them the deliberate edits; 0 rules duplicated |
| 5 | the classification names files | `adr:0006-framework-boundary#classification` rows read `doc:10-architecture` → 1 and `doc:15-repository-layout` → 2; `doc:15-repository-layout#tiers` §2.2 carries the same two names and marks itself the rendering |
| 6 | the gate | `BUILD SUCCESSFUL`, `> Task :docsLint  docs-lint: OK — 19 documents, 100 anchors, 741 references.` — full run: `167 actionable tasks: 53 executed, 114 from cache` |

### Criterion 1 — the selection algorithm run over the real front-matter

```
task:  Write a Modus Module that serves incident records in a domain, with its own routes
       and permissions.
files: modules/module-acme/**
observed: SELECTS doc:10-architecture   path: modules/**
          SELECTS doc:30-code-style     path: "**/*.kt"
          SELECTS doc:35-testing        task: write|add|move|fix .*(test|…)
          doc:15-repository-layout NOT selected

task:  Model the work bounded context: aggregates, value objects, domain events and ports.
files: core/core-domain/**
observed: SELECTS doc:15-repository-layout  path: core/**
          SELECTS doc:20-ddd-practices      path: core/**
          SELECTS doc:30-code-style         path: "**/*.kt"
          doc:10-architecture NOT selected
```

The cost of the second row, stated rather than glossed: `doc:10-architecture#bounded-contexts`
§3.1 governs `core/` packages and is no longer selected by a `core/` change. It is reached in
one hop from the two places in `doc:15-repository-layout` that cite it — §4 and §4.2's
`PublishedLanguageIsLeaf` row — which is the mechanism `doc:05-authoring-for-agents#read-when`
step 5 specifies. The same holds for an `adapter-rest` change and §5.1's route allowlist.

### Criterion 2 — the plants

```
planted:  doc:10-architecture#module-system renamed to #modules in provides and in the heading
observed: FAIL check 6  .beans/modus-0009--identity-bounded-context.md: 'doc:10-architecture#module-system'
                        — documentation/10-architecture.md does not provide '#module-system'
          FAIL check 6  .beans/modus-0039…, .beans/modus-0043…, documentation/adr/0006-framework-boundary.md
          docs-lint: 4 failure(s).
```

The first line is the constraint above, firing: a completed bean is the file that breaks.

```
planted:  doc:15-repository-layout also provides #module-system, with a heading to match
observed: FAIL check 4  anchor '#module-system' is provided by more than one document:
                        doc:10-architecture#module-system doc:15-repository-layout#module-system
          docs-lint: 1 failure(s).
```

Both reverted; `docs-lint: OK` after each.

### Criterion 3 — every reference from the tier-1 half into the tier-2 half

| where | points at | rule or rationale |
|---|---|---|
| lead paragraph | the whole document | routing |
| §3.1, "`cost` imports `execution`'s events because it consumes `AgentRunCompleted`" | §6.1 | rationale for a row of a table stated in full above it |
| §3.1 `Enforced by:` | §4.2 `PublishedLanguageIsLeaf` | enforcement of a rule §3.1 states in full |
| §5.1 | §4.3 `ControllersAreDomainScoped` | enforcement of the allowlist §5.1 states in full |
| §5.1 | §9 | the ADR requirement for *changing* the allowlist — this repository's change control, not a Module author's obligation |

No obligation on a Module author lives in `doc:15-repository-layout`. Read as that author,
the tier-1 half carries the descriptor and its dependency rule (§7.1–§7.2, §4.1), the
non-pollution rule (§7.3), the route convention and the authorisation contract (§5.1–§5.4),
per-domain visibility (§5.4), and what may be imported from `core` (§3, §3.1). The one
addition made for this criterion is the pointer from §7.2 to `doc:40-durability` — a module's
durable state goes through a port, and the format those ports write was reachable from
nowhere in the tier-1 half.

### Criterion 4 — conservation

```
cmd:      every non-trivial line of the pre-split body, counted across the two halves
observed: lines in pre-split body: 459
          MISSING (21):     the 8 edited passages — the title and lead, the "directory tree
                            below" cell, §2.2's per-section classification sentence and two
                            tier rows, four cross-references that became typed references,
                            two headings that gained an <a id>, and §7.2's durability pointer
          DUPLICATED (0 rules): only the "## 4. Dependency rules — machine-readable" heading,
                            which both halves carry by design, and the "| Rule | Detail |"
                            table header
```

Line counts: `documentation/10-architecture.md` 287, `documentation/15-repository-layout.md`
230, against the 500-line ceiling and 482 before the split.

## Encoded, and deliberately not done

- `doc:README#changing-this-package` gains the section-numbering rule.
- `bean:0050` raised: `adr:0001-record-architecture-decisions` §3 forbids editing an accepted
  ADR, and this bean's own criteria required editing `adr:0006`'s classification table. The
  edit was made and the conflict is the new bean's to rule on.
- `bean:0042` will forbid a tier-1 document referencing a tier-3 one. `doc:10-architecture`
  is now tier 1 and cites `doc:00-constitution` §1.2 and §8, which `adr:0006` classifies
  tier 3. The split does not create that violation and cannot fix it: the constitution
  carries tier-1 rules (layering, domain scoping) inside a tier-3 document. `bean:0042` is
  blocked on that, not on this.
- Not done: renumbering sections, moving `doc:50-memory-and-evidence`'s tier seam, and
  editing the §-numbers of `todo` beans that cite sections which stayed exactly where they
  were.
