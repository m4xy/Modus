---
# modus-0132
title: The backoffice's typed client disagrees with the normative route convention in three ways
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# The backoffice's typed client disagrees with the normative route convention in three ways

Why: `doc:10-architecture#domain-root-convention` §5.1 is the normative route list and the
single normative copy of the non-domain-scoped allowlist. `backoffice/src/api/client.ts`
disagrees with it in three ways. All three exist today, are decidable today, and are blocked
by nothing.

Split out of `bean:0146`, which is blocked on `modus-0017` and cannot start. §5.1 is cited
by name from three places outside that slice — `doc:15-repository-layout#adapter-rules`
§4.3, `doc:30-code-style` §4 and `doc:00-constitution#domain-scoping` §8 — so the blast
radius is not slice-local, and a slice whose purpose is to be the narrowest thing that
reaches a browser is the wrong place to settle it.

The three rows are not settled by one bean either. `bean:0146` serves the session route
(row 2) and `bean:0018` serves every domain-scoped route, the work list (rows 1 and 3)
among them. Settling all three here is what keeps the answer in one place instead of two.

## The three, and which one §9 gates

| # | typed client, observed | §5.1 | ADR under `doc:15-repository-layout#extending` §9 |
|---|---|---|---|
| 1 | `API_BASE = '/api'` — `backoffice/src/api/http.ts:9` | the route list carries no prefix | no |
| 2 | `GET /api/session`, non-domain-scoped — `client.ts:20` | the allowlist has no `/session`. `/auth/**` (prefix) covers "login, token exchange, session"; `/domains` (exact) covers "list domains this actor can see" | **yes** |
| 3 | `/work` — `client.ts:24` | `/domains/{domainId}/work-items` | no |

§9 requires an ADR for "a Gradle module, a bounded context, or a **non-domain-scoped
route**" and states that everything else "is ordinary work and needs only a work item". Row
2 is a non-domain-scoped route. Rows 1 and 3 are not, and presenting all three under one ADR
gate makes the cheap two look expensive while making the ADR that would legitimise row 2
look like the price of the set.

```
cmd:      grep -n "API_BASE = \|'/work'\|'/session'" backoffice/src/api/http.ts backoffice/src/api/client.ts
observed: backoffice/src/api/http.ts:9:export const API_BASE = '/api';
          backoffice/src/api/client.ts:20:  session: (signal?: AbortSignal) => request<Session>('/session', signal ? { signal } : {}),
          backoffice/src/api/client.ts:24:      request<WorkItem[]>(domainPath(domainId, '/work'), signal ? { signal } : {}),
```

## Nothing decides this mechanically, so the first controller decides it

```
cmd:      grep -rn 'ControllersAreDomainScoped\|DomainScopedRoute' architecture-tests/ config/
observed: (no match)

cmd:      grep -rn 'ControllersAreDomainScoped\|DomainScopedRoute' documentation/
observed: 00-constitution.md:382:**Enforcement gap:** neither `ControllersAreDomainScoped` (ArchUnit) nor `DomainScopedRoute`
          10-architecture.md:243:**Enforcement gap:** none of the three exist — `ControllersAreDomainScoped`, the
          (and the specifying rows: 15-repository-layout.md:129, 30-code-style.md:189)
```

Both rules are specified and neither has a source. They are named here in backticks and not
as typed `rule:` references for that reason: check 6 resolves `rule:` against the tree, and a
rule a document specifies but `config/detekt/detekt.yml` does not declare is not a target
(`doc:05-authoring-for-agents#reference-syntax`).

That is what makes this `type: fix` rather than a preference. Both rules are specified to
**read** §5.1's list rather than carry a copy, so whatever the first controller writes
becomes the de-facto input to two rules nobody has written yet — and the first controller
to be written is `bean:0146`'s session route.

## Recommendation, offered as a recommendation

The frontend moves in all three cases and no ADR is needed.

| # | recommended | cost |
|---|---|---|
| 1 | the `/api` prefix goes in the Vite proxy, stripping it, or in a servlet context-path — not in the mapping annotation. A literal `@GetMapping("/api/domains/{domainId}/work")` is exactly what `DomainScopedRoute` is specified to reject, since `doc:30-code-style` §4 tests a path that **starts with** `/domains/{domainId}` | build config, which `bean:0146` needs anyway |
| 2 | `/auth/session`, already covered by the existing `/auth/**` prefix. The allowlist is untouched, §9 is not reached, and no ADR is written | one line, `client.ts:20` |
| 3 | `/work` → `/work-items`, following the document | `client.ts:24` and the mock handler beside it |

Whoever takes this bean may instead argue the document should move; §9 says what that costs
and this bean does not pre-empt it. What it rules out is settling row 2 by writing the ADR
without first pricing the one-line alternative.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Each of the three is settled on its own merits — frontend moves, document moves, or ADR — with row 2 the only one for which an ADR is even in question | diff |
| 2 | Every path the typed client can produce is checked against §5.1 path by path, not by eye, and each either appears in the route list or matches an allowlist entry | command |
| 3 | If any allowlist member is added, `documentation/adr/` carries the ADR §9 requires and §5.1 — the single normative copy — gains the member; no second copy of the list appears anywhere | citation |
| 4 | `backoffice/src/mocks/handlers.ts` and `client.ts` agree after the change, so the mock stops asserting a contract the normative document denies | command |
| 5 | `./gradlew qualityCheck` green, backoffice checks included | test-run |

## Not in scope

- Writing `ControllersAreDomainScoped` or `DomainScopedRoute`. `doc:00-constitution#domain-scoping`
  §8's gap line names `bean:0018` as the carrier of both, and this bean settles which paths
  they will read, not the rules that read them.
- Serving any of these routes. `bean:0146` serves the session route and is blocked on this;
  `bean:0018` serves the domain-scoped ones.
- The per-item work route, which the document lists and no component calls (`bean:0144`).
