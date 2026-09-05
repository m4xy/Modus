---
# modus-0143
title: The global margin reset pins every modal to the viewport's top-left corner
status: todo
type: fix
priority: normal
created_at: 2026-09-05T00:00:00Z
---

# The global margin reset pins every modal to the viewport's top-left corner

Why: `backoffice/src/ui/Dialog.module.css` sets no margin, and the UA stylesheet centres a
modal `<dialog>` with `margin: auto`. `backoffice/src/styles/global.css:11` resets
`margin: 0` on `*`, which wins over the UA rule, so every dialog opens flush against the
top-left corner of the viewport instead of centred. The work item detail panel — the only
dialog currently rendered — is affected, and so is anything else built on the primitive.

The component is otherwise correct: it uses the native element, gets the top layer, the
focus trap and Escape from the platform, and axe reports no violation. The defect is purely
positional, which is why no automated check catches it and why it survived to here.

## Observed

Driven in headless Chromium at a 1440×1000 viewport against the MSW-backed dev server.

```
url:      http://localhost:5173/domains/modus/work
action:   click the work item "Backoffice foundation"
observed: dialog getBoundingClientRect = { x: 0, y: 0, width: 544, height: 316.34 }
          getComputedStyle(dialog) -> margin: "0px", position: "fixed", inset: "0px"
```

```
cmd:      grep -n 'margin' backoffice/src/styles/global.css
observed: 11:  margin: 0;
cmd:      grep -c 'margin' backoffice/src/ui/Dialog.module.css
observed: 0
```

A 544px panel at `x: 0, y: 0` in a 1440px viewport is the corner, not the centre. Escape
closes it and the backdrop click closes it, so the behaviour is right and only the
placement is wrong.

Success criteria:

1. A modal dialog is centred in the viewport at every breakpoint the backoffice supports,
   restored explicitly in `Dialog.module.css` rather than by weakening the global reset.
2. The position is asserted, not eyeballed — a bounding box whose centre matches the
   viewport centre within tolerance — and the assertion is observed failing against the
   current code (`doc:00-constitution#observed-failing`).
