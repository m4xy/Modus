---
# modus-0142
title: The agent console arrives with a prefilled prompt and a billed run one click away
status: todo
type: fix
priority: normal
created_at: 2026-09-05T00:00:00Z
---

# The agent console arrives with a prefilled prompt and a billed run one click away

Why: `backoffice/src/routes/AgentConsole.tsx` seeds its prompt state with a hardcoded
demo sentence, so the console loads with the textarea already populated and **Run session**
already enabled. The screen's own caption says "Runs are billed to this domain". A single
click on a freshly opened console therefore starts a billed run executing an instruction
the operator never wrote, against whichever domain the switcher last landed on.

The console is otherwise careful about this: an emptied prompt is refused with "Write a
prompt first — an empty run costs money and returns nothing", and an actor without
`agents.run` gets a disabled button. The seeded default routes around the guard the same
file already thought worth writing.

## Observed

Driven in headless Chromium against the MSW-backed dev server.

```
url:      http://localhost:5173/domains/modus/agents   (first load, no typing)
observed: textarea value = "Replace the mock stream transport with a real SSE
                            client, keeping the console untouched."
          "Run session" disabled = false

action:   clear the textarea, click "Run session"
observed: toast "Write a prompt first"; transcript stays IDLE   (guard works)

action:   fresh load, click "Run session" without touching the textarea
observed: transcript status STREAMING, first block —
          YOU  "Replace the mock stream transport with a real SSE client,
                keeping the console untouched."
```

```
cmd:      sed -n '103,105p' backoffice/src/routes/AgentConsole.tsx
observed:   const [prompt, setPrompt] = useState(
              'Replace the mock stream transport with a real SSE client, keeping the console untouched.',
            );
```

The transcript is also not cleared and the prompt not reset when a run completes, so a
second click re-runs the same text against the same domain with no indication that it is a
new session.

Success criteria:

1. The console opens with an empty prompt, and **Run session** is inert until the operator
   has written something.
2. Starting a run states which domain will be billed at the moment of the click, not only
   in static caption text.
3. Asserted in the Playwright suite, observed failing against the current code
   (`doc:00-constitution#observed-failing`).

Related: `bean:0021` owns the real stream transport; this is the console's own affordance
and does not depend on it.
