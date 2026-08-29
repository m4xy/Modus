---
# modus-0023
title: Implement ContextInternalsAreSealed and PublishedLanguageAllowlist
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0031]
---

# Implement ContextInternalsAreSealed and PublishedLanguageAllowlist

Why: `doc:10-architecture#bounded-contexts` §3.1 names four enforcing rules. Two are
implemented and non-vacuous; these two carry an `Enforcement gap:`, because both compare
one context against another and `identity` is the only modelled context — an
implementation today would be a rule that cannot fail, which
`doc:00-constitution#observed-failing` rates as worse than an admitted gap.

Success criteria:

- Both rules implemented in `architecture-tests`, derived from §3.1's two tables rather
  than carrying a copy of either.
- Each observed failing on a planted violation between two real contexts, then reverted —
  possible only once `bean:0012` has landed the second one.
- §3.1's `Enforcement gap:` paragraph replaced by an `Enforced by:` line.
