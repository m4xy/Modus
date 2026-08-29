---
# modus-0049
title: docs-lint claims bash 3.2 compatibility and nothing tests it
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# `docs-lint` claims bash 3.2 compatibility and nothing tests it

`tools/docs-lint.sh:6` states, as a constraint on everyone who edits it:

> No bash 4 feature is used (macOS ships 3.2)

Nothing checks it. `build.gradle.kts`'s `docsLint` task runs `commandLine("bash", …)`, which
resolves through `PATH` — on the development machine that is Homebrew's bash 5.3.9, and in CI
it is Linux bash 5. Found by `bean:0035`'s implementation, which had to verify by hand:

```
cmd:      bash --version
observed: GNU bash, version 5.3.9(1)-release        # Homebrew, first on PATH
cmd:      /bin/bash --version
observed: GNU bash, version 3.2.57(1)-release       # what macOS actually ships
```

So the constraint is real for anyone running the script directly on a stock macOS shell, and
is enforced against nobody. A bash 4 feature could be added today and every gate would stay
green until it reached a developer whose `PATH` has no newer bash — the worst possible place
to discover it.

## Success criteria

- Either the script is run under a 3.2-compatible interpreter by the gate, or the constraint
  is struck and the header stops claiming it. Both are defensible; claiming it and not
  checking it is not (`doc:00-constitution#observed-failing`).
- If kept: `docsLint` invokes an interpreter that actually enforces it, and the choice is
  observed rejecting a planted bash 4 construct — an associative array or `mapfile` — before
  the claim is restored.
- If struck: the header says which interpreter the script targets, and `bean:0035`'s
  awk-portability notes are re-read, since they were written for the same reason.
