---
# modus-0146
title: The session endpoint — the read-only slice that lights the app shell
status: todo
type: feature
priority: high
created_at: 2026-09-05T00:00:00Z
parent: modus-0018
blocked_by: [modus-0147, modus-0148, modus-0149, modus-0150, modus-0132]
---

# The session endpoint — the read-only slice that lights the app shell

Split out of `bean:0018`. One endpoint: the session route, returning `actor`, `domains` and
`permissions`. It is not domain-scoped, so it never enters the §5.3 authorisation contract
and therefore never reaches step 4 — module visibility, which is what `bean:0031` supplies
and what keeps the parent blocked. That is the entire reason this can land first.

**This bean is much thinner than its first draft advertised, and the thinning was a
correction rather than a trim.** It lights the app shell, the domain switcher and the
capability-driven navigation. It does **not** light the Work screen. An earlier revision
claimed the work-list route as well, on the argument that work items are a core resource and
not a Module resource. That argument is wrong, and the section below records why, because a
reader who reaches the same conclusion independently needs to find it already answered.

## What the earlier scope got wrong, and why the parent was right

`bean:0018` argues against splitting:

> narrowing this bean's own criteria to exclude step 4 would ship an authorisation contract
> with a hole in it and a second bean to patch the hole later. Adding the edge instead means
> the contract lands whole, once, the same shape `bean:0009` put the rest of
> `AccessDecision`'s mapping in.

On the work route the parent is right. Work tracking is served by a **Module**, and a Module
is per-domain installable, so §5.3 step 4 applies to it:

```
cmd:      grep -n 'module-beans' documentation/10-architecture.md documentation/15-repository-layout.md
observed: 10-architecture.md:41:A Gradle module named `modules/module-beans` implements a Modus Module. A bounded
          10-architecture.md:294:`module-beans` (work tracking) and `module-cost` (spend tracking) are the two first-party
          15-repository-layout.md:47:  module-beans/                         work tracking (per-domain installable)

cmd:      grep -n 'module-beans' settings.gradle.kts
observed: 43:module("module-beans", "modules/module-beans")
```

§7 states it without qualification: `module-beans` and `module-cost` "are structurally
ordinary modules — they get no privileges a third-party-authored module would not get. This
is deliberate: it keeps the module boundary honest." Two further reads close the escape
routes the earlier draft used:

- **There is no core-route category to appeal to.** §7 clause 4 says a Module "Contributes
  routes only under `/domains/{domainId}/…`" — the same namespace as everything else. So
  §5.1's uniform route list cannot be read as core routes distinct from module routes, and
  the earlier argument depended on exactly that distinction existing.
- **§5.4's other half was quoted selectively.** Beside the per-actor `/openapi.json` it
  says: "The backoffice navigation is likewise generated from the installed-and-visible
  module set. There is no hardcoded menu." Work is a nav item.

The last move the earlier draft made was a conflation: step 4 does not apply because the
`work` bounded context is not installed through `ModuleInstallation`. True and irrelevant —
a bounded context is not installable; the Module that exposes it is, and that is
`module-beans`. `doc:10-architecture#module-system` and §2's table row exist precisely to
keep those two names apart.

**So the Work screen needs step 4, and therefore `bean:0031`, and therefore stays with
`bean:0018`.** Making work tracking stop being a Module would be an architecture change to
§7; it must not ride on a slice, and anyone who wants it needs their own bean and its own
ADR (`doc:15-repository-layout#extending` §9).

The session route survives all of this because §5.3 opens "Every **domain-scoped** request
passes through exactly one authorisation step". The session route is not one, so there is no
step 4 to hole and nothing for a later bean to patch.

## What one endpoint actually buys

**It is the hard gate for everything else.** `backoffice/src/app/DomainRoute.tsx:27` branches
on the session query and renders "Cannot reach the Modus API" when it fails — no shell, no
navigation, no domain, and no route below it. Until this endpoint answers, no other endpoint
can be exercised through the app at all.

`permissions` is load-bearing and is not optional. `DomainContext` derives `can()` from the
grant whose `domainId` matches the current domain, and `AppShell`'s `PrimaryNav` reads it per
item: an item with `whenDenied: 'hide'` disappears, and every other item renders locked, with
a lock glyph and a toast, and does not navigate. With an empty grant the shell is present and
nothing in it works.

```
cmd:      grep -n 'whenDenied' backoffice/src/app/navigation.ts backoffice/src/app/AppShell.tsx
observed: navigation.ts:15:  whenDenied: 'hide' | 'lock';
          navigation.ts:24,31,38,45,59:    whenDenied: 'lock',      (work, repositories,
                                            agents, memories, skills)
          navigation.ts:52,66:    whenDenied: 'hide',              (cost, settings)
          AppShell.tsx:152:        if (!allowed && item.whenDenied === 'hide') return null;
```

So the slice proves the seam that carries the most risk, against a live server rather than
MSW: the dev-server wiring below, the session contract, and capability gating. Every screen
behind it renders its empty or locked state, which is the honest picture of a backoffice with
one endpoint.

Its edges: the durable store, because `bean:0009` declared `ActorRepository` and
`PermissionGrantRepository` and implemented neither; and `modus-0132`, which settles what
this route is called before the first controller is written. The store edge named
`modus-0017` when this bean was written and now names that bean's four children,
`bean:0147` to `bean:0150`: `docs-lint` check 12 refuses an edge onto a `type: epic` bean,
and all four rather than a subset because the single edge cleared only when all four bullets
were done, so naming all four clears at exactly the same moment (`bean:0017`). `bean:0009` and `bean:0030` are
`completed` and supply the model. The work-item chain — `modus-0067`, and `modus-0013`
behind it — is **no longer an edge here**: it belonged to the work route, and the work route
went back to the parent with it.

## The prerequisite that is not an endpoint

`backoffice/vite.config.ts` declares no `server.proxy`. With `VITE_MOCK_API=false` the app's
own `fetch` asks **Vite** for the session, Vite has no such route, and the browser shows
"Cannot reach the Modus API" against a server that is answering correctly.

```
cmd:      grep -n 'server\|proxy' backoffice/vite.config.ts
observed: 18:  server: { port: 5173, strictPort: true },
          (no proxy key)

cmd:      grep -rn 'VITE_MOCK_API' backoffice/src
observed: backoffice/src/main.tsx:13:  if (import.meta.env['VITE_MOCK_API'] !== 'false') {
```

Either the dev server proxies to the Spring server, or the SPA is served from Spring so the
two share an origin. Naming it is the point: it is a build-config gap, not a missing route,
so an agent working endpoint by endpoint never reaches it and concludes the endpoint is
broken.

## Decisions this slice does NOT settle

1. **How a request is bound to an actor.** §5.3 step 1 resolves the actor "from the session
   or token", and `bean:0019` owns authentication and grant administration. §5.5's bootstrap
   — the first actor created by `app/modus-server` at first start from configuration — is a
   documented path to *an* actor and is not a login. This slice must not invent an
   authentication mechanism to fill the gap, and must not let whatever it uses to reach a
   bootstrap actor harden into one.
2. **`AgentRun`'s shape.** `backoffice/src/api/types.ts` marks the interface pre-correction
   in its own KDoc: `costUsd` is floating-point dollars where `doc:60-cost-model#spend-record`
   and every record already written under `domains/<domainId>/cost/` are integer
   micro-dollars, and `tokensIn`/`tokensOut` are two token kinds where the stream seam next
   door reports five (`bean:0069`). `bean:0014` defines the aggregate and `bean:0044` would
   generate types from it. This slice serves no run.
3. **The agent stream.** §5.1 lists `/runs/{runId}/stream` (SSE) *and* `/runs/{runId}/socket`
   (WebSocket) and chooses neither; `backoffice/src/agent/transport.ts:5` says "the real one
   will be an SSE (or WebSocket) client" and the only implementation is
   `MockStreamTransport`. So no URL, verb or protocol is settled on either side — unspecified
   surface, not deferred surface, with an owner (`bean:0021`) and no shape.

## What remains with `bean:0018`

- Every domain-scoped route, **including the work list**, and with it §5.3 steps 1 to 4 and
  the `modus-0031` edge that step 4 needs.
- Every write verb, and therefore every control `bean:0141` finds confirming a save that
  never happens.
- The per-actor `/openapi.json` of §5.4, and with it `bean:0044`'s generated types.
- `DomainTypesDoNotEscape`, `NoDtoInCore` and `ControllersAreDomainScoped`.
  `doc:15-repository-layout#adapter-rules` §4.3's gap line assigns all three to `bean:0018`
  by name, and this bean does not write them — see criterion 5.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The session route is served by `adapters/adapter-rest` at the path `bean:0132` settles. This bean writes the first controller and adopts that outcome: it chooses no path, moves no document and raises no ADR | citation |
| 2 | The response carries `actor`, `domains` and `permissions`. The permissions half is asserted — a response without it leaves the shell rendered and unnavigable, which passes any check that only asserts 200 | test-run |
| 3 | `domains` lists only domains the actor holds a grant on, decided by `bean:0009`'s `AccessDecision` with no conditional of the transport's own. Asserted on a planted extra domain, and observed silent on unmodified source (`doc:00-constitution#observed-failing`) | test-run |
| 4 | Actor and grants are read through `bean:0017`'s repositories from the store, not from a fixture; a grant added on disk changes the response without rebuilding anything but the read | test-run |
| 5 | The controller returns a DTO and no `core-domain` type appears in its signature (`doc:15-repository-layout#adapter-rules` §4.3). The ArchUnit rule that would decide this mechanically is `DomainTypesDoNotEscape`, which does not exist and which §4.3's gap line assigns to `bean:0018`. This bean asserts the behaviour and records the gap; it does not write the rule and does not claim coverage it lacks | diff |
| 6 | The backoffice reaches the live server with `VITE_MOCK_API=false` — proxy or shared origin, whichever is chosen — and renders the shell, the domain switcher and capability-gated navigation. Every screen behind it shows its empty or locked state, asserted as the expected result rather than treated as a failure | test-run |
| 7 | No authentication mechanism is introduced (decision 1 above), and the three decisions are settled nowhere | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The work list route and every other domain-scoped route. They need §5.3 step 4 and
  therefore `bean:0031`; `bean:0018` keeps them.
- The route names themselves (`bean:0132`), which this bean is blocked on.
- Changing §7 so that work tracking stops being a Module. That is an architecture change
  needing its own bean and its own ADR, and it must not ride on a slice.
- `bean:0140`'s failed-fetch handling and `bean:0144`'s per-item address, both
  backoffice-side and neither blocking this.
- Deleting the mock (`bean:0022`), which needs far more than one endpoint.
