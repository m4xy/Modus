---
# modus-0041
title: Split doc:10-architecture along its tier seam
status: todo
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
  `doc:10-architecture#tiers` is corrected with it.
