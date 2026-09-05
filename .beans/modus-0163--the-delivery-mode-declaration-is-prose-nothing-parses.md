---
# modus-0163
title: The delivery-mode declaration is prose, and nothing parses it
status: todo
type: fix
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# The delivery-mode declaration is prose, and nothing parses it

`doc:20-ddd-practices#domain-events` §4.1.8 binds every implementation of
`DomainEventDispatchPort` to two things. The first — do not swallow a handler's exception —
is asserted by tests against each implementation. The second is not asserted by anything:

> every implementation of the dispatch port states which delivery mode it is

It states it in a KDoc. **Nothing parses a KDoc.** An implementation that declares no mode
fails no build, and — worse, because it is the case a reader would trust — an implementation
that declares the mode it is *not* fails no build either. The clause was ruled prose rather
than a checkable requirement on PR #83's approval.

Why the clause exists at all is worth keeping, because the fix must not delete it: §4.1.8's
consequences differ by mode, so a caller that cannot tell which mode it has cannot know
whether a handler's failure will reach it. That question has one answer today and will have
two the moment `bean:0160`'s durable dispatcher lands.

## Shape

Recorded as a direction, not a decision. `doc:00-constitution#observed-failing` warns that
enumerating the accepted shapes fails open, so prefer the form that fails closed — here, one
where an implementation cannot compile without answering.

A marker interface pair, or a sealed `DeliveryMode` the port requires as a member, moves the
declaration into the type system: an implementation that declares nothing does not compile,
and one that declares the wrong mode is at least a reviewable line rather than a sentence in
a comment. **Do it while there is one implementor** — `InProcessDomainEventDispatch` — rather
than after `bean:0160` and the four contexts behind it have arrived.

A source-reading ArchUnit rule over the KDoc text is the shape to avoid, and the reason is
sharper than "it would check that a sentence exists rather than that it is true". **It would
be worse than the admitted gap it replaced.** Satisfying criterion 4 swaps `Enforcement gap:`
for `Enforced by:`, and `doc:README#conventions` admits that line "only once that tool has
been observed rejecting one" — here, rejecting the *missing*-sentence case. The
*wrong*-sentence case, an implementation whose KDoc names the mode it does not exhibit, would
then pass green in perpetuity behind a line telling every reader the rule is enforced. An
admitted gap at least leaves someone looking (`doc:00-constitution#observed-failing`).

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | An implementation of `DomainEventDispatchPort` that declares no delivery mode fails the build, observed failing on a planted implementation and reverted (`doc:00-constitution#observed-failing`) | |
| 2 | The healthy case is shown passing in the same run — `InProcessDomainEventDispatch` declares synchronous and is green — so the mechanism is not one that rejects every input | |
| 3 | Declaring a mode an implementation does not exhibit is caught, or the residual is stated plainly as still unenforceable rather than left implied by criterion 1 | |
| 4 | §4.1.8 carries an `Enforced by:` naming the mechanism. If criterion 3 leaves a residual, the `Enforcement gap:` line is **narrowed to that residual and kept beside it**, never deleted — an `Enforced by:` standing alone claims cover this bean may not have delivered | |
| 5 | `./gradlew qualityCheck` green | |
