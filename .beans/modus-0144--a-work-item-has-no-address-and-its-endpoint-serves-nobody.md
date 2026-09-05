---
# modus-0144
title: A work item has no address, and the endpoint that would serve one is called by nothing
status: todo
type: fix
priority: normal
created_at: 2026-09-05T00:00:00Z
---

# A work item has no address, and the endpoint that would serve one is called by nothing

Why: `GET /domains/{domainId}/work/{key}` is declared in `backoffice/src/api/client.ts`
and answered by `backoffice/src/mocks/handlers.ts`, and no component calls either. The work
item detail dialog is built from the row already in the list response, opening it does not
change the URL, and the path the endpoint implies is not a route.

Two consequences, one of them for the backend. A human cannot link a colleague to a work
item — the unit this whole product is organised around. And `bean:0018` would build a
per-item REST route, and `bean:0044` would generate types for it, to serve a client that
never calls it. The contract is being carried forward on the strength of the mock alone.

## Observed

Driven in headless Chromium against the MSW-backed dev server, recording every request
whose URL contains `/api/`.

```
url:      http://localhost:5173/domains/modus/work
observed: API requests on load — GET /api/session
                                 GET /api/domains/modus/work
action:   click the work item "Backoffice foundation"
observed: dialog opens, renders title, status, kind, spend and the markdown body
          API requests added by the click: []
          url unchanged: http://localhost:5173/domains/modus/work

url:      http://localhost:5173/domains/modus/work/0002
observed: "That page does not exist / Every screen in Modus lives under a domain."
```

```
cmd:      grep -rn 'api.work.get\|work.get(' backoffice/src
observed: (no matches outside backoffice/src/api/client.ts's own declaration)
```

`backoffice/src/App.tsx`'s route table has a `work` route under `/domains/:domainId` and no
child below it, so the 404 above is the router behaving correctly against a route that was
never added.

Success criteria:

1. A work item is addressable — a URL below the work route resolves to that item, is
   produced when the detail opens, and survives a reload and a browser back.
2. The detail view is served by `api.work.get`, so the per-item endpoint has exactly one
   caller rather than none.
3. An unknown key under a visible domain renders a not-found state for the item, not the
   application's 404 page.

This settles a shape `bean:0018` would otherwise guess at and `bean:0044` would generate
types for; either resolve it here or delete the unused endpoint so the mock stops asserting
a contract nothing holds.
