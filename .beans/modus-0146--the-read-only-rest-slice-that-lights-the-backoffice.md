---
# modus-0146
title: The read-only REST slice that lights the backoffice — session and work
status: todo
type: feature
priority: high
created_at: 2026-09-05T00:00:00Z
parent: modus-0018
blocked_by: [modus-0017, modus-0067]
---

# The read-only REST slice that lights the backoffice — session and work

Split out of `bean:0018`. The parent is `blocked_by: [modus-0017, modus-0031]`, and
`bean:0031` supplies step 4 of the §5.3 authorisation contract — module visibility. The two
endpoints that put a live server behind the backoffice need steps 1 to 3 and not step 4, so
they can land before module installation does. That is the whole reason for the split: it is
the only way anything reaches a browser from a real server before `bean:0031`.

Its own edges are `modus-0017` — the durable store, since a read has to read something — and
`modus-0067`, the `.beans`-file-to-`WorkItem` mapper, which is where a work item comes from
at all and which carries `modus-0013` transitively. `bean:0009` (actor, grant,
`domainIsVisible`) and `bean:0030` (the `Domain` aggregate) are `completed`, so they impose
no edge and supply the session's other two fields.

## The surface, established by driving the app

Two endpoints, both `GET`. The current frontend surface is entirely `GET`: `http.ts` types
the write verbs and no call site passes one.

```
cmd:      grep -rn 'method' backoffice/src
observed: backoffice/src/api/http.ts:25:  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE';
          backoffice/src/api/http.ts:31:  const method = options.method ?? 'GET';
          backoffice/src/api/http.ts:33:  const init: RequestInit = { method, headers: …
          backoffice/src/api/http.ts:43:    throw new ApiError(status, url, `${method} …
          (four hits, all inside the one module permitted to touch the network)
```

**`GET /api/session` is the hard gate.** `backoffice/src/app/DomainRoute.tsx:27` branches on
the session query and renders "Cannot reach the Modus API" when it fails — no shell, no
navigation, no domain, and no route below it. It returns `{ actor, domains[], permissions[] }`.

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

**`GET /api/domains/{domainId}/work` lights the whole Work screen** — the table, the three
stat tiles, the status filters and the detail dialog, all derived from one list response.

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
404-not-403 rule; the slice must reproduce it rather than invent a shape.

## The third prerequisite is not an endpoint

`backoffice/vite.config.ts` declares no `server.proxy`. With `VITE_MOCK_API=false` the app's
own `fetch` asks **Vite** for `/api/session`, Vite has no such route, and the browser shows
"Cannot reach the Modus API" against a server that is answering correctly.

```
cmd:      grep -n 'server\|proxy' backoffice/vite.config.ts
observed: 18:  server: { port: 5173, strictPort: true },
          (no proxy key)

cmd:      grep -rn 'VITE_MOCK_API' backoffice/src
observed: backoffice/src/main.tsx:13:  if (import.meta.env['VITE_MOCK_API'] !== 'false') {
```

Either the dev server proxies `/api` to the Spring server, or the SPA is served from Spring
so the two share an origin. Naming it is the point: it is a build-config gap, not a missing
route, so an agent working endpoint by endpoint never reaches it and concludes the endpoint
is broken.

## Three route conflicts this slice must settle, not inherit

`doc:10-architecture#domain-root-convention` §5.1 is the normative route convention and says
its non-domain-scoped allowlist is the single normative copy. The frontend disagrees with it
in three ways, and every one of them is a decision the slice makes the moment it serves a
byte:

| conflict | frontend, as observed | §5.1 |
|---|---|---|
| base path | `API_BASE = '/api'` in `backoffice/src/api/http.ts` | the route list carries no prefix |
| session route | `GET /api/session`, non-domain-scoped | the allowlist has no `/session`; `/auth/**` covers "login, token exchange, session" and `/domains` (exact) covers "list domains this actor can see" |
| resource name | `/work` | `/domains/{domainId}/work-items` |

§5.1 requires an ADR to add an allowlist member (`doc:15-repository-layout#extending` §9), so
the slice either serves the shapes the document already allows or raises the ADR. It may not
add `/session` quietly, and it may not leave the frontend and the document disagreeing while
claiming the slice is done.

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
- 404-not-403 asserted over **every** cross-domain access path. This slice asserts it for the
  two routes it serves; the parent's criterion is over all of them and is not weakened.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `GET` session and `GET` work are served by `adapters/adapter-rest` at routes that agree with `doc:10-architecture#domain-root-convention` §5.1, or the ADR §9 requires is raised in the same change | citation |
| 2 | The session response carries `actor`, `domains` and `permissions`, and the permissions half is asserted — a response without it leaves the shell rendered and unnavigable, which is a pass to any check that only asserts 200 | integration test |
| 3 | Work items are read through `bean:0067`'s mapper from the store, not from a fixture; a work item added to `.beans/` appears in the response without a rebuild of anything but the read | integration test |
| 4 | §5.3 steps 1 to 3 hold on both routes: no actor → 401, no grant on the domain → **404, never 403**, and each observed failing on a planted leak (`doc:00-constitution#observed-failing`) | planted violation, reverted |
| 5 | Controllers return DTOs; no domain type crosses the boundary (`doc:15-repository-layout#adapter-rules` §4.3) | archunit |
| 6 | The backoffice reaches the live server with `VITE_MOCK_API=false` — proxy or shared origin, whichever is chosen — and the Work screen renders real rows. Asserted, not screenshotted | e2e |
| 7 | The two decisions above are restated nowhere and settled nowhere; a reviewer can see that the slice serves no run and generates no type | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- `bean:0140`'s failed-fetch handling and `bean:0144`'s per-item address. Both are
  backoffice-side and neither blocks this; this slice is what makes the first of them
  reachable.
- Deleting the mock. `bean:0022` owns that, and it needs more than these two routes.
