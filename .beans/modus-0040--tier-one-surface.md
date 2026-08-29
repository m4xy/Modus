---
# modus-0040
title: Write the tier-1 surface and extract the modus domain's SDLC from it
status: todo
type: epic
priority: high
order: AG
created_at: 2026-08-29T00:00:00Z
---

# Write the tier-1 surface and extract the `modus` domain's SDLC from it

Implements `adr:0006-framework-boundary`. The tiers and the test are decided; this is the
work of making the tree obey them.

The finding that motivates it: **Modus has no documented extension surface.** Every one of
the twelve documents in `documentation/` addresses someone developing Modus in this
repository, and none addresses someone using it. A third-party Module author has no document
to read.

## Children

| bean | ships |
|---|---|
| `bean:0041` | split `doc:10-architecture` along the tier seam its §2.2 already records |
| `bean:0042` | mark tier on every document's front-matter, and check it mechanically |
| `bean:0043` | move `tools/docs-lint.sh` behind the Module boundary |

## Done when

A reader can tell, from the front-matter of any document and the tier table of any directory,
which of the three tiers it belongs to; `doc:10-architecture` no longer mixes two tiers in one
file; and the `modus` domain's document policy is that domain's rather than the framework's.

## Deliberately not in scope

Writing tenant-facing *guides* — a getting-started, an extension tutorial. Those are a
product-documentation effort with a different audience and a different review standard, and
naming the surface is a prerequisite for them rather than part of them.
