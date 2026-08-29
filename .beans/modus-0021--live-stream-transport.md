---
# modus-0021
title: The live SSE stream transport
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0018, modus-0020]
---

# The live SSE stream transport

Why: `backoffice/src/agent/transport.ts` defines the seam and `mockTransport.ts` is its
only implementation, so the console replays a canned session where `doc:10-architecture`
§6.1 requires live agent output.

Success criteria:

- A domain-scoped streaming endpoint emitting the `StreamEvent` shapes `transport.ts`
  declares, in order, additively — never a full transcript.
- A `StreamTransport` implementation beside `MockStreamTransport`, selected by
  configuration; the console keeps not knowing which it holds, and the mock stays as the
  offline fixture.
- Cancellation honoured end to end: dropping the `StreamSubscription` cancels the run
  rather than orphaning it. No blocking call on the streaming path — one stalled
  dispatcher stutters every live stream.
- SSE before WebSocket: the flow is server-to-client and additive, so the bidirectional
  transport is not chosen without a consumer that needs it.
- A Playwright assertion on live output (`doc:00-constitution` §10).

Blocks `bean:0022`.
