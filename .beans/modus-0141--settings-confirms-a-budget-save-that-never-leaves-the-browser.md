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
