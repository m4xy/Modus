---
# modus-0146
title: The read-only REST slice that lights the backoffice — session and work
status: todo
type: feature
priority: high
created_at: 2026-09-05T00:00:00Z
parent: modus-0018
blocked_by: [modus-0017, modus-0067, modus-0132]
---

# The read-only REST slice that lights the backoffice — session and work

Split out of `bean:0018`. The parent is `blocked_by: [modus-0017, modus-0031]`, and
`bean:0031` supplies step 4 of the §5.3 authorisation contract — module visibility. The two
endpoints that put a live server behind the backoffice need steps 1 to 3 and not step 4, so
they can land before module installation does. That is the whole reason for the split: it is
the only way anything reaches a browser from a real server before `bean:0031`.

Its own edges: `modus-0017`, the durable store, since a read has to read something;
`modus-0067`, the `.beans`-file-to-`WorkItem` mapper, which is where a work item comes from
at all and which carries `modus-0013` transitively; and `modus-0132`, which settles what the
two routes are called before this bean writes the first controller. `bean:0009` (actor,
grant, `domainIsVisible`) and `bean:0030` (the `Domain` aggregate) are `completed`, so they
impose no edge and supply the session's other two fields.

## The parent argues against this split, and the rebuttal has to be on the record

`bean:0018` states the position in its own text:

> narrowing this bean's own criteria to exclude step 4 would ship an authorisation contract
> with a hole in it and a second bean to patch the hole later. Adding the edge instead means
> the contract lands whole, once, the same shape `bean:0009` put the rest of
> `AccessDecision`'s mapping in.

That argument is right about `bean:0018` and does not reach this child, for a reason that is
checkable rather than asserted. §5.3 step 4 asks whether the resource is "served by a
**Module** installed in this domain". Neither route here is. The session route is not
domain-scoped at all, so it never reaches step 4; and work items are a **core** resource —
§5.1's own route list names `/domains/{domainId}/work-items` beside `/runs` and `/memories`,
while §5.4 defines a Modus Module as something *installed into* a domain through a
`ModuleInstallation` aggregate, which the `work` bounded context is not. A contract over two
routes that never reach step 4 has no hole where step 4 would be, so there is nothing for a
later bean to patch.

The parent keeps its `modus-0031` edge and its criteria are **not** narrowed: `bean:0018`
still lands the contract whole over every resource, and this child removes nothing from it.
`bean:0018`'s prose names `modus-0012` where its front-matter names `modus-0031` — the child
of `modus-0012` that supplies `ModuleInstallation`; that discrepancy is pre-existing and is
not this bean's to fix.

What would make the parent right and this child wrong: a work resource served by a Module.
If that is the intended design, this bean is wrong and should be closed unstarted, not
widened.

## The surface, established by driving the app

Two endpoints, both `GET`. The current frontend surface is entirely `GET`: `http.ts` types
the write verbs and no call site passes one.

```
cmd:      grep -rn 'method' backoffice/src
observed: backoffice/src/api/http.ts:25:  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
          backoffice/src/api/http.ts:31:  const method = options.method ?? 'GET';
          backoffice/src/api/http.ts:33:  const init: RequestInit = { method, headers: …
          backoffice/src/api/http.ts:43:    throw new ApiError(status, url, …method…
          (four hits, all inside the one module permitted to touch the network)
```

**The session endpoint is the hard gate.** `backoffice/src/app/DomainRoute.tsx:27` branches
on the session query and renders "Cannot reach the Modus API" when it fails — no shell, no
navigation, no domain, and no route below it. It returns `actor`, `domains` and
`permissions`.

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

**The work list endpoint lights the whole Work screen** — the table, the three stat tiles,
the status filters and the detail dialog, all derived from one list response.

```
cmd:      grep -n 'use[A-Z][a-zA-Z]*(' backoffice/src/routes/Work.tsx
observed: 43:  const { domain } = useDomain();
          44:  const query = useWorkItems(domain.id);
          45:  const [search, setSearch] = useState('');
          49:  const items = useMemo(() => {
```

**No per-item endpoint is needed for this slice.** The detail dialog is built from the row
already in the list, and `api.work.get` has no caller anywhere in the app (`bean:0144` owns
that finding and the choice it forces). A slice that serves the list serves the dialog.

An unknown domain answers `404` with a null body in the mock, which is already §5.3 step 2's
404-not-403 rule; the slice reproduces it rather than inventing a shape.

Both routes are named here by role and not by path. What they are called is `bean:0132`,
which this bean is blocked on: the typed client's `/api` prefix, its non-domain-scoped
session route, and `/work` against the document's `/work-items` are three disagreements with
`doc:10-architecture#domain-root-convention` §5.1 that exist today and are decidable today.
This slice writes the first controller, so it inherits whatever `bean:0132` settles and
settles none of it itself.

## The third prerequisite is not an endpoint

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

Recorded here so nobody later reads a green slice as having settled them.

1. **`AgentRun`'s shape.** `backoffice/src/api/types.ts` marks the interface pre-correction
   in its own KDoc: `costUsd` is floating-point dollars where `doc:60-cost-model#spend-record`
   and every record already written under `domains/<domainId>/cost/` are integer
   micro-dollars, and `tokensIn`/`tokensOut` are two token kinds where the stream seam next
   door reports five (`bean:0069`). `bean:0014` defines the aggregate; anything generating
   types from the frontend propagates the pre-correction shape (`bean:0044`). This slice
   serves no run and must not be read as blessing the type.
2. **The agent stream.** §5.1 lists `/runs/{runId}/stream` (SSE) *and* `/runs/{runId}/socket`
   (WebSocket) and chooses neither; `backoffice/src/agent/transport.ts:5` says "the real one
   will be an SSE (or WebSocket) client" and the only implementation is
   `MockStreamTransport`. So no URL, verb or protocol is settled on either side. That is
   **unspecified** surface, not deferred surface — deferred surface has an agreed shape and
   an owner, and this has an owner (`bean:0021`) and no shape.

## What remains with `bean:0018`

- Step 4 of §5.3, module visibility, which is what `bean:0031` supplies and what the parent
  keeps its edge to.
- Every write verb, and therefore every control `bean:0141` finds confirming a save that
  never happens.
- The per-actor `/openapi.json` of §5.4, and with it `bean:0044`'s generated types.
- Every resource beyond session and work — repositories, agent runs, memories, skills,
  `cost/summary`.
- 404-not-403 asserted over **every** cross-domain access path, and the
  `ControllersAreDomainScoped` ArchUnit rule: §5.4's own gap line assigns both to
  `bean:0018`. This slice asserts 404-not-403 for the two routes it serves; the parent's
  criterion is over all of them and is not weakened.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The session route and the domain-scoped work-list route are served by `adapters/adapter-rest` at the paths `bean:0132` settles. This bean writes the first controller and adopts that outcome: it chooses no path, moves no document and raises no ADR | citation |
| 2 | The session response carries `actor`, `domains` and `permissions`, and the permissions half is asserted — a response without it leaves the shell rendered and unnavigable, which passes any check that only asserts 200 | test-run |
| 3 | Work items are read through `bean:0067`'s mapper from the store, not from a fixture; a work item added to `.beans/` appears in the response without rebuilding anything but the read | test-run |
| 4 | §5.3 steps 1 to 3 hold on both routes: no actor → 401, no grant on the domain → **404, never 403**. Each assertion is observed failing on a planted leak and silent on unmodified source (`doc:00-constitution#observed-failing`) | test-run |
| 5 | Controllers return DTOs; no domain type crosses the boundary (`doc:15-repository-layout#adapter-rules` §4.3) | test-run |
| 6 | The backoffice reaches the live server with `VITE_MOCK_API=false` — proxy or shared origin, whichever is chosen — and the Work screen renders rows read from `.beans/`. Asserted, not screenshotted | test-run |
| 7 | The two decisions above are settled nowhere: a reviewer can see that the slice serves no run and generates no type | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The route names themselves (`bean:0132`), which this bean is blocked on.
- `bean:0140`'s failed-fetch handling and `bean:0144`'s per-item address. Both are
  backoffice-side and neither blocks this; this slice is what makes the first of them
  reachable.
- Deleting the mock. `bean:0022` owns that, and it needs more than these two routes.
