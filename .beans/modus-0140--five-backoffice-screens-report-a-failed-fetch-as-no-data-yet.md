---
# modus-0140
title: Five backoffice screens report a failed fetch as "no data yet"
status: in-progress
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# Five backoffice screens report a failed fetch as "no data yet"

Why: `backoffice/src/routes/Cost.tsx` is the only route that branches on `query.isError`.
Work, Repositories, Memories and Skills branch on `query.isPending` and then fall through
to their empty state; the agent console's run history branches on neither and renders
`(runs.data ?? []).length === 0` straight into it, reaching the same screen one step
sooner. Either way a 500 from the server and an empty collection render identically. The
screens are correct today only because MSW never fails; the defect becomes reachable the
moment `bean:0022` points them at a live server, which is the first thing a real
`bean:0018` REST layer will do.

## Observed

Driven in headless Chromium against `vite --port 5174` started with `VITE_MOCK_API=false`,
so MSW's service worker was absent and Playwright's `page.route` reached the app's own
`fetch`. A stub session was served; `/api/domains/*/work` was fulfilled with `500`.

```
cmd:      grep -n 'isError' backoffice/src/routes/*.tsx
observed: Cost.tsx:26:  if (query.isError || !query.data) {
          (no other match in Work.tsx, Repositories.tsx, Memories.tsx,
           Skills.tsx, AgentConsole.tsx)

cmd:      grep -n 'runs.data' backoffice/src/routes/AgentConsole.tsx
observed: 303:            {(runs.data ?? []).length === 0 ? (
          336:                  {(runs.data ?? []).map((item) => (
          (the run history reads no status flag at all — neither isError nor
           isPending — so a rejected query renders "No runs recorded")

route:    **/api/domains/*/work -> 500
url:      http://localhost:5174/domains/d1/work
observed: OPEN ITEMS 0 / SPEND ATTRIBUTED $0.00 / IN REVIEW 0
          "No work items yet"
          "Beans are the unit of work in Modus. Create the first one and the
           harness has something to run against."

route:    **/api/domains/*/cost/summary -> 500
url:      http://localhost:5174/domains/d1/cost
observed: "Cost data is unavailable"
          "The cost summary request failed. Reload to try again."
```

The same `/work` route fulfilled with `[]` produces a byte-identical screen to the `500`
above. Cost, which has the branch, distinguishes the two correctly — so the fix is a shape
already present in the codebase, applied to the other five call sites.

Beyond the wording, the three stat tiles above the table report `0`, `$0.00` and `0` as
though measured. A reader is told the domain has no backlog and has spent nothing, when
what happened is that nothing was read.

Success criteria:

1. `Work`, `Repositories`, `Memories`, `Skills` and the agent console's run history each
   branch on `query.isError` before their empty state, and say the request failed rather
   than that the collection is empty.
2. No summary figure derived from a failed query is rendered as a measured value.
3. Each of the five is asserted under a forced non-2xx response, and each assertion is
   observed failing against the current code before the fix
   (`doc:00-constitution#observed-failing`).

Blocks `bean:0022` — replacing the mock with the live API is what makes this reachable.

## Restated criteria

| # | Restated, binary | Evidence kind planned |
|---|---|---|
| 1 | Each of Work, Repositories, Memories, Skills and the agent console's run history renders a distinct failure state under a non-2xx, and the empty-state sentence is absent from the DOM | test-run |
| 2 | Under a failed `/work`, no stat tile renders a figure — the three tiles are absent, not zeroed | test-run |
| 3 | Each assertion in 1 and 2 is observed failing against the pre-fix source, with the plant confirmed applied | test-run |

Out of scope: `bean:0022`'s live API, any change under `core/`, `adapters/` or
`modules/`, and any control that writes (that is `bean:0141`).

## Approach

- `backoffice/src/ui/ErrorState.tsx` — one shape for "the request failed", `role="alert"`,
  `data-testid="error-state"`. `EmptyState` keeps its own meaning.
- Six call sites branch on `query.isError` first: the five named plus `Cost`, which had
  the branch and now shares the shape.
- `Work` returns the whole screen as the failure — tiles and filters included, since a
  tile is a claim and a filter over an unfetched list filters nothing.
- The agent console's run history gains `isPending` as well as `isError`; the bean's own
  correction is that it read neither.
- `backoffice/src/mocks/handlers.ts` gains `?fail=<resource>`. Without it the branch is
  unreachable from a test: MSW's service worker answers before the network, so
  Playwright's `page.route` never sees the call.
