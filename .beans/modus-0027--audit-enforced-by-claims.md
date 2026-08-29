---
# modus-0027
title: Audit every Enforced by line against the observed-failing rule
status: todo
type: task
priority: high
order: B
created_at: 2026-08-29T00:00:00Z
---

# Audit every Enforced by line against the observed-failing rule

Why: `doc:00-constitution#observed-failing` makes an `Enforced by:` line admissible only
once its mechanism has been observed rejecting a violation. Every such line in
`documentation/` predates that rule. Two were found false by review and one by writing the
rule, so the base rate is not low.

Success criteria:

- Every `Enforced by:` line in `documentation/**` resolved to a named mechanism, and that
  mechanism observed rejecting a planted violation of the rule it claims. The output is a
  table of claim, mechanism, planted violation, observed rejection; a row with no observed
  rejection is a failed row.
- A line whose mechanism does not exist, or cannot be made to fail, is demoted to an
  `Enforcement gap:` naming the bean that closes it — never quietly deleted, never left
  standing.
- Start from `doc:00-constitution`: §2's Gradle dependency-verification rule for database
  drivers, §3's memory schema validation and `work` transition guard, §7.1's branch
  protection and `pre-push` hook, §7.3's commit-message check (`bean:0024`), §8's
  `ControllersAreDomainScoped` and `DomainScopedRoute`. Several name contexts that do not
  exist yet and belong to the beans that build them; that is a legitimate outcome, stated
  rather than assumed.
- Also start from `doc:40-durability` and `doc:60-cost-model`: together roughly nine more
  `Enforced by:` lines (a `SIGKILL` kill test, a recovery test suite, a round-trip property
  test, `module-cost` validation rules and more) name mechanisms for
  `adapter-persistence-flatfile` and `module-cost` — both empty placeholder descriptor
  classes with zero test files anywhere. Every one of those lines is a claim about a
  mechanism that does not exist; scoping the audit to `doc:00-constitution` alone would
  miss the largest concentration of false claims in the package.

Splitting is expected (`doc:00-constitution#context-budget` §6.2): the survey is small and
a document's worth of demotions may not be.
