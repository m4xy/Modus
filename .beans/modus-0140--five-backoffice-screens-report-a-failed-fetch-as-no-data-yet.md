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

## Evidence

Two runs of the same assertions against the same rig: one with the fix in the tree, one
with `backoffice/src/routes` reverted to `99212fc` and everything else — the fault switch
included — left standing.

### The plant, confirmed applied

```
cmd:      git status --porcelain | wc -l
expected: 0 — never plant over uncommitted work (AGENTS.md)
observed: 0

cmd:      git checkout 99212fc -- backoffice/src/routes
cmd:      git diff --stat HEAD -- backoffice/src/routes
observed: 7 files changed, 46 insertions(+), 150 deletions(-)

cmd:      grep -c ErrorState backoffice/src/routes/Work.tsx backoffice/src/routes/AgentConsole.tsx
expected: 0 in each — the fix is gone from the source the run will build
observed: backoffice/src/routes/Work.tsx:0
          backoffice/src/routes/AgentConsole.tsx:0

cmd:      grep -n "title: 'Budget saved'" backoffice/src/routes/Settings.tsx
observed: 73:                      title: 'Budget saved',

cmd:      grep -c 'failing(' backoffice/src/mocks/handlers.ts
expected: 9 — the plant must not disarm the rig it is measured with
observed: 9

cmd:      lsof -ti tcp:4173
expected: nothing — reuseExistingServer would otherwise serve the previous build
observed: (no output)
```

### Criterion 1 — each of the five branches on `isError` and says the request failed

`e2e/tests/failure-states.spec.ts`, one test per screen, driving headless Chromium
against the production build with `?fail=<resource>` forcing that resource to `500`.

Against the plant — each names the sentence the screen must not have said:

```
cmd:      npx playwright test tests/failure-states.spec.ts
expected: each of the five fails on the empty-state sentence
observed: ✘ Work reports a failed read as a failure, not as no data
            Locator:  getByText('No work items yet')
            Expected: 0
            Received: 1
          ✘ Repositories reports a failed read as a failure, not as no data
            Locator:  getByText('No repositories connected')
            Expected: 0
            Received: 1
          ✘ Memories reports a failed read as a failure, not as no data
            Locator:  getByText('Nothing remembered yet')
            Expected: 0
            Received: 1
          ✘ Skills reports a failed read as a failure, not as no data
            Locator:  getByText('No skills installed')
            Expected: 0
            Received: 1
          ✘ Agent console run history reports a failed read as a failure, not as no data
            Locator:  getByText('No runs recorded')
            Expected: 0
            Received: 1
          ✘ Cost reports a failed read as a failure, not as no data
            Locator: getByRole('alert')
            Expected substring: "Cost data could not be loaded"
            Error: element(s) not found
```

Cost is the control: it already distinguished the two, so it does not fail on the
empty-state sentence — it fails one line later, on the shared shape and the `role="alert"`
the other five now carry too.

With the fix in the tree:

```
cmd:      npx playwright test tests/failure-states.spec.ts tests/settings.spec.ts
expected: 18 passed
observed: 18 passed (22.9s)
```

Each screen is asserted twice. The second half is what makes the first mean anything —
a branch that fires on every input has replaced one wrong answer with another:

```
observed: ✓ Work still renders its data when the read succeeds
          ✓ Repositories still renders its data when the read succeeds
          ✓ Memories still renders its data when the read succeeds
          ✓ Skills still renders its data when the read succeeds
          ✓ Agent console run history still renders its data when the read succeeds
          ✓ Cost still renders its data when the read succeeds
```

Each of those six asserts a value only the real fixture can produce — `run_301`,
`git@github.com:m4xy/Modus.git`, `Backoffice foundation` — and that no `alert` is present.

### Criterion 2 — no figure derived from a failed query is rendered as measured

Against the plant:

```
cmd:      npx playwright test tests/failure-states.spec.ts
expected: fails — the tiles report 0, $0.00 and 0 above the "no work items" sentence
observed: ✘ Work reports no figures it did not measure
            Locator:  getByText('$0.00')
            Expected: 0
            Received: 1
```

With the fix, the same test passes: a failed `/work` returns the whole screen as the
failure, so `Open items`, `Spend attributed`, `In review`, `$0.00` and the search field
are all absent from the DOM rather than zeroed. While the query is merely *pending* the
tiles read `—`, for the same reason one step earlier.

### Criterion 3 — the assertions were observed failing, and were not vacuous

The first draft of these assertions was vacuous, and the plant is what said so. Run
straight after `goto`, `toHaveCount(0)` resolves on its first poll — while the screen is
still a skeleton and the empty-state sentence has not been rendered yet. All six passed
against the unfixed code:

```
observed (first plant run): ✓ ... reports a failed read as a failure  (the absence line)
                            ✘ ... — failed one line later, on the alert not existing
```

The absence assertions now run behind `settled()`, which waits for the route's `h1` and
then for `[aria-busy="true"]` — `SkeletonList`'s own marker — to reach zero. The failures
quoted under criteria 1 and 2 above are from the corrected assertions.

### The gate and accessibility

```
cmd:      ./gradlew qualityCheck
expected: BUILD SUCCESSFUL
observed: BUILD SUCCESSFUL in 1m 31s
          191 actionable tasks: 62 executed, 129 from cache

cmd:      npx playwright test          (the whole suite, fix in tree)
expected: no regression on the 7-route axe scan, and the new error UI clean too
observed: 55 passed (13.5s)
          ✓ Work renders with no accessibility violations
          ✓ Repositories renders with no accessibility violations
          ✓ Agent console renders with no accessibility violations
          ✓ Memories renders with no accessibility violations
          ✓ Cost renders with no accessibility violations
          ✓ Skills renders with no accessibility violations
          ✓ Settings renders with no accessibility violations
          ✓ no accessibility violations in dark theme
          ✓ a failed screen has no accessibility violations, light and dark
```

The last of those is new: `wcag2a`/`wcag2aa`/`wcag21a`/`wcag21aa` over a screen showing
the new `ErrorState`, in both themes, with zero violations.
