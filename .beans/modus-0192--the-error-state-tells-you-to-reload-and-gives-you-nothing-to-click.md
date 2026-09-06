---
# modus-0192
title: The error state tells you to reload and gives you nothing to click
status: todo
type: fix
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# The error state tells you to reload and gives you nothing to click

Why: `bean:0140` gave six screens an honest failure state, and every one of them ends its
description with "Reload to try again." There is no control to do it. The operator is told what
to do and handed no way to do it, on a screen whose whole purpose is not to mislead.

`ErrorState` already has the slot. `ErrorStateProps.action` is declared, threaded through to
`EmptyState`'s `action`, and passed by nobody:

```
cmd:      grep -rn 'action' backoffice/src/ui/ErrorState.tsx
observed: 8:  action?: ReactNode;
          22:export function ErrorState({ title, description, action }: ErrorStateProps) {
          25:      <EmptyState title={title} description={description} mark="!" action={action} />

cmd:      grep -rn '<ErrorState' -A 4 backoffice/src/routes | grep -c 'action='
observed: 0
```

A prop with no call sites is an untested prop. It renders in `EmptyState`'s `action` div, which
no test has ever driven.

This is deliberate scope discipline in `bean:0140`, not an oversight there — a retry button
wants `query.refetch()` and a pending state on the button, which is a behaviour rather than a
branch, and the branch was that bean's subject. It becomes worth doing the moment `bean:0022`
points these screens at a live server, because a real transient failure is the case where
reloading the whole document to retry one query is visibly the wrong affordance.

## Success criteria

| # | Criterion | Evidence kind |
|---|---|---|
| 1 | Each of the six error states carries a control that refetches its own query, and the copy names that control instead of "Reload" | test-run |
| 2 | The control reports its own outcome: pending while refetching, and a second failure leaves the error state standing rather than silently doing nothing — the `bean:0141` rule applied to a retry | test-run |
| 3 | Asserted under a fault that clears: the first read 500s, the retry succeeds, the screen shows data without a document reload | test-run |
| 4 | Asserted under a fault that persists: the retry fails and nothing reports success | test-run |
| 5 | axe is clean on an error screen with the control present, both themes, `wcag2a`/`wcag2aa`/`wcag21a`/`wcag21aa` | test-run |

Criterion 3 needs the mocked API's `?fail=` switch to be clearable per request rather than read
from the URL on every call (`backoffice/src/mocks/handlers.ts`) — a fail-once mode, or a
counter. That is part of this bean.

Raised from independent review of the `bean:0140`/`bean:0141` pull request.
