---
# modus-0141
title: Settings confirms a budget save that never leaves the browser
status: in-progress
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# Settings confirms a budget save that never leaves the browser

Why: `backoffice/src/routes/Settings.tsx`'s **Save budget** button's entire `onClick` is a
call to `notify`. It issues no request, mutates no cache and persists nothing, then tells
the operator in a success toast that the domain "will refuse new runs above" the number
they typed. A spend cap is the one control in this product whose failure costs money, and
the screen reports success for a cap that was never set.

This is worse than an unwired control, because an unwired control is visibly unwired. A
false confirmation is indistinguishable from a real one, so the operator has no reason to
check.

## Observed

Driven in headless Chromium against the MSW-backed dev server, recording every request
whose URL contains `/api/`.

```
url:      http://localhost:5173/domains/modus/settings
action:   click "Save budget"
observed: toast — "Budget saved / Modus Core will refuse new runs above $750."
          API requests issued by the click: []

action:   set the budget field to 1234, click "Save budget" again
observed: API requests issued: []
          sidebar still reads "$428.60 of $750.00 budget"
```

```
cmd:      grep -n 'onClick' backoffice/src/routes/Settings.tsx
observed: 70:                  onClick={() =>
          (the file's only onClick; `sed -n '70,76p'` gives the whole handler)
          70:                  onClick={() =>
          71:                    notify({
          72:                      tone: 'success',
          73:                      title: 'Budget saved',
          74:                      body: `${domain.name} will refuse new runs above $${budget}.`,
          75:                    })
          76:                  }
```

Two further controls on the same screen are editable and have no save path at all: the
**Display name** `Input` and the **Environment** `Select` are rendered with `defaultValue`
and enabled whenever the actor holds `settings.write`, but nothing reads them back and no
button submits them. Typing in either changes nothing and says nothing.

Success criteria:

1. No control in the backoffice reports a write as succeeded without a 2xx from the
   server. Either the control persists, or it is disabled and says why.
2. **Display name** and **Environment** either submit or are not editable.
3. The false-success path is asserted — a save against a failing endpoint must not produce
   a success toast — and the assertion is observed failing against the current code
   (`doc:00-constitution#observed-failing`).

Blocked on nothing to make the controls honest; the persisting half needs `bean:0018`.

## Restated criteria

| # | Restated, binary | Evidence kind planned |
|---|---|---|
| 1 | **Save budget** is disabled, carries no `onClick`, and no interaction with the Settings screen produces a success toast or a non-GET request to `/api/` | test-run |
| 2 | **Display name** and **Environment** are disabled, and still display the domain's real values | test-run |
| 3 | Both assertions are observed failing against the pre-fix source, with the plant confirmed applied | test-run |

Out of scope: persisting anything. There is no endpoint — `src/api/client.ts` declares
reads only and the aggregate is `bean:0018`.

## The ruling: disable, not wire

Wiring the control needs an endpoint that does not exist. Inventing one would move the
untruth one layer down — the browser would report a 2xx from a mock nobody will ship —
so the honest half of criterion 1 ("or it is disabled and says why") is the one taken.
Every control on the screen is now read-only, with one sentence on the screen saying
why and a second beside the button saying why that one in particular.

## Evidence

### The plant, confirmed applied

Shared with `bean:0140` — one revert of `backoffice/src/routes` to `99212fc` over a clean
tree, with the mocked API and the specs left standing.

```
cmd:      git status --porcelain | wc -l
observed: 0

cmd:      git checkout 99212fc -- backoffice/src/routes
cmd:      grep -n "title: 'Budget saved'" backoffice/src/routes/Settings.tsx
expected: the false-success handler is back in the source the run will build
observed: 73:                      title: 'Budget saved',
```

### Criterion 1 — no success toast, and no request, for a save that did not happen

```
cmd:      npx playwright test tests/settings.spec.ts        (against the plant)
expected: fails — the click raises a success toast naming the limit
observed: ✘ clicking Save budget confirms nothing, because nothing is saved
            Error: expect(received).toBe(expected) // Object.is equality
            Expected: 0
            Received: 1
            > 49 |   expect(await page.getByText('Budget saved').count()).toBe(0);

cmd:      npx playwright test tests/settings.spec.ts        (with the fix)
expected: 2 passed
observed: ✓ clicking Save budget confirms nothing, because nothing is saved (802ms)
          ✓ display name and environment are not editable (495ms)
```

The passing test clicks the control with `force: true` — past Playwright's actionability
check, because the question is what the app says, not whether a user could reach it — and
then reads, without retrying, that no toast text exists, that the notification region's
text is empty, and that the click issued no non-`GET` request to `/api/`.

### Criterion 2 — Display name and Environment are not editable

```
cmd:      npx playwright test tests/settings.spec.ts        (against the plant)
expected: fails — both are enabled whenever the actor holds settings.write
observed: ✘ display name and environment are not editable
            Locator:  getByLabel('Display name')
            Expected: disabled
            Received: enabled
            18 × locator resolved to
              <input id="_r_2_" value="Modus Core" class="Field-module__control__oFHjY"/>
```

With the fix all three fields are `disabled`, and still display the domain's real values —
`Modus Core` and `750` — rather than blanks, which the same test asserts.

### Criterion 3 — the assertion was observed failing, and the first version did not

The first form of the toast assertion was `await expect(page.getByText('Budget
saved')).toHaveCount(0)`, and it **passed against the code that raises the toast**. A
web-first assertion retries until it holds; the toast dismisses itself after
`DISMISS_AFTER_MS = 6000` (`backoffice/src/ui/Toast.tsx`) and the suite's `expect` timeout
is `7_000`. So it sat through the false confirmation, watched it go, and reported a pass.

Probed in the same build, rather than asserted:

```
cmd:      a throwaway spec that clicks and reads the count at four moments
observed: PROBE disabled = false
          PROBE count immediately = 1
          PROBE count after 50ms = 1
          PROBE count after 550ms = 1
          PROBE region = "Budget saved\n\nModus Core will refuse new runs above $750."
```

The element was there within 50ms and stayed for six seconds. The retrying assertion had
every chance to see it. The non-retrying form quoted under criterion 1 is what fails.

Raised as `bean:0190`, because the rule belongs in `documentation/` and this change does
not own that tree.

### The gate

```
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 1m 31s

cmd:      npx playwright test
observed: 55 passed (13.5s)
          ✓ Settings renders with no accessibility violations
```
