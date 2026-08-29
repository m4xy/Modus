---
# modus-0036
title: Enforce defensive copying of collection-typed properties in the domain
status: todo
type: feature
priority: high
order: AF
created_at: 2026-08-29T00:00:00Z
---

# Enforce defensive copying of collection-typed properties in the domain

The same defect has now shipped twice, in two bounded contexts, past two evidence passes.

| where | found by | what escaped |
|---|---|---|
| `PermissionGrant.capabilities` (`bean:0009`) | review | the live `Set` was returned; a caller could down-cast to `MutableSet` and grant itself a capability nobody issued |
| `ProcessDefinition` (`bean:0030`) | review | all four collections were `public val` constructor parameters, copied neither in nor out; a caller could add a transition, widen the terminal set, and mutate the payload of an **already-raised** `DomainCreated` |

`bean:0030`'s second occurrence got past ten planted mutations with a 10/10 kill rate, the
`doc:35-testing#fixture-variation` rule quoted in its own fixture KDoc, and an author who
had read the first occurrence's fix. That is the argument for a tool: the rule is known,
written down, and still not followed, which `doc:00-constitution#mechanical-enforcement`
says is a rule that will keep being broken.

`doc:20-ddd-practices#value-objects` §3.1 now states the rule and carries this bean as its
`Enforcement gap:`.

## Success criteria

- A type in `..domain.published..` or `..domain.aggregate..` with a collection-typed
  property fails the build unless the property is private and every accessor returns a copy.
  Observed rejecting the pre-fix `ProcessDefinition` and the pre-fix `PermissionGrant`
  before it is claimed (`doc:00-constitution#observed-failing`).
- The rule distinguishes a `private val` backing field from a `public val` — the former is
  the fix, not the violation.
- Choose the mechanism deliberately and record why: a Detekt rule sees the AST and the
  property's visibility directly, which is what this needs; ArchUnit sees bytecode, where a
  Kotlin getter returning a copy and one returning the field are hard to tell apart. The
  Detekt path is blocked on `bean:0026`, which has to establish that custom Detekt rules
  exist at all — none of the eleven `doc:30-code-style` §4 documents are implemented.
- A test asserting the copy property MUST use a collection of size two or more.
  `Collections.singleton` and `singletonList` throw on mutation, so the same test written
  against a one-element fixture passes while proving nothing — observed twice in
  `bean:0030`, once in the aggregate's `pendingEvents` and once in this bean's own subject.
