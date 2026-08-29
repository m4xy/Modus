---
# modus-0025
title: docs-lint must resolve a rule:archunit reference to a real rule
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# docs-lint must resolve a rule:archunit reference to a real rule

Why: check 6 of `doc:05-authoring-for-agents#checks` resolves an `archunit` reference with
`grep -rhoE "(val|fun) <ident>\b" architecture-tests`, which matches any declaration of
that name — a helper, a local, a `val` holding a string. It establishes neither that the
name is an `ArchRule` nor that any test evaluates it. Two rules `doc:10-architecture`
documented as enforcing survived a full review cycle without existing, and a reviewer
found them, not the linter.

Success criteria:

- Check 6 fails when an `archunit` reference names something that is not an `ArchRule` (or
  an `ArchCondition`-backed equivalent) evaluated by a test method, observed failing on a
  planted reference to a real-but-non-rule declaration and on a rule declared but never
  evaluated, then reverted (`doc:00-constitution#observed-failing`).
- The `detekt` arm assessed in the same pass: it greps the config for a rule id and cannot
  see whether the rule can fire, which is exactly the gap
  `doc:30-code-style#detekt-configuration` now records. Fix it or state it.
- Row 6 of `doc:05-authoring-for-agents#checks` says what the check now decides.
