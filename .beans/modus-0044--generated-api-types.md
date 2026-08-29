---
# modus-0044
title: Generate the backoffice API types from the OpenAPI document
status: todo
type: feature
priority: high
order: AH
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0018]
---

# Generate the backoffice API types from the OpenAPI document

`doc:30-code-style` §6 states it as a rule already:

> | API types | Generated from the OpenAPI document | Hand-written API types are forbidden — they drift |

`backoffice/src/api/types.ts` carries **22 hand-written declarations**, including
`export type DomainId = string`, and nothing generates or checks them. The rule has been
false since the backoffice was written, and it carries no `Enforcement gap:` — this bean is
the one it should have named.

## Why this is load-bearing, not tidy-up

It is the gate on splitting the frontend into its own repository (`bean:0039`). Today the
contract between `core-domain` and the backoffice is checked by a person noticing:
`bean:0009` verified `Capability`'s vocabulary against `types.ts` by hand, once, and
recorded it as evidence. That works while both sides sit in one diff. Across a repository
boundary nothing notices at all, and the failure is silent — a renamed field ships green on
both sides and breaks at runtime in a tenant's browser.

A generated client turns the boundary into a **versioned artifact**, which is what makes a
split safe rather than merely faster. Do it before the split, not after.

## The toolchain, already chosen

Code-first, not spec-first. `gradle/libs.versions.toml:7` already declares
`springdoc = "3.1.0"` and `springdoc-openapi-starter-webmvc-ui`, and
`rule:archunit/domainIsFrameworkFree` already forbids `org.springdoc..` inside
`core-domain` — so the repository committed to generating the OpenAPI document *from* the
Spring controllers before this bean existed. It is unwired only because `adapter-rest` is an
empty placeholder.

That leaves one open half: what turns the document into TypeScript.

| tool | what it emits | verdict |
|---|---|---|
| **`openapi-typescript`** | types only, no runtime | **chosen.** It replaces exactly the artifact that is wrong — `types.ts` — and touches nothing else. `client.ts`, `http.ts` and `queries.ts` stay hand-written, which keeps the diff to the one file that drifts |
| `orval` | types **plus** TanStack Query hooks | rejected for now. The backoffice already has hand-written `queries.ts` over TanStack Query, so this would regenerate working code to solve a problem nobody has. Revisit if the hand-written query layer starts drifting too |
| `openapi-generator` (`typescript-fetch`) | a whole client | rejected. It replaces the transport as well as the types, and the transport is not the thing that drifts |
| Kotlin Multiplatform — share the domain types directly | one definition, compiled to JS | **ruled out by the constitution**, not by taste. `doc:00-constitution#layering` §1.1 says `backoffice/` may depend on the REST API contract and MUST NOT depend on any Kotlin source. A shared KMP module is Kotlin source |

Spec-first — hand-maintain an OpenAPI YAML and generate both sides from it — was also
rejected: it makes the spec a third artifact that can drift from the Kotlin, which is the
same class of problem one level up.

## Success criteria

- `backoffice/src/api/types.ts` is generated from the OpenAPI document `adapter-rest`
  serves, and is checked in so the build stays hermetic.
- Regenerating produces no diff when the API has not changed, and `qualityCheck` fails when
  the checked-in types differ from what the current document generates. Observed failing:
  rename a field server-side, regenerate, watch the check reject it
  (`doc:00-constitution#observed-failing`).
- `doc:30-code-style` §6's row becomes an `Enforced by:` naming that check, or is struck.
  It has been an unmarked falsehood since the backoffice was written.
- `identity`'s `Capability` vocabulary is covered by the generated contract rather than by
  the hand check `bean:0009` recorded.

Blocked on `bean:0018`: there is no OpenAPI document to generate from until the REST layer
exists. `adapter-rest` is an empty placeholder today.
