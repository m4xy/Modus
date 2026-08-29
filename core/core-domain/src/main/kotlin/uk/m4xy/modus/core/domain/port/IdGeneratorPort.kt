package uk.m4xy.modus.core.domain.port

/**
 * A fresh opaque identifier, as a dependency rather than as ambient randomness.
 *
 * `doc:00-constitution` §1.3 bans `UUID.randomUUID()` from `core-domain` and
 * `doc:20-ddd-practices` §5.3 names this port as the replacement. Note that the ban is
 * currently unenforced — `NoAmbientRandom` does not exist, and `UUID.randomUUID()` planted
 * in `core-domain` was observed merging green (`bean:0065`). The port is what makes the
 * rule's alternative reachable; closing the rule gap itself is `bean:0027`.
 *
 * ## Why this returns a `String` and not a typed identifier
 *
 * Every identifier in the model is a `@JvmInline value class` in some context's
 * `..published..` package — `ActorId`, `GrantId`, and their equivalents to come. A
 * context-free port returning those types would need **one method per identifier type**,
 * so it would have to know every context's identifier set and would gain a method whenever
 * any context added one. Every context injecting it would then transitively name
 * `identity.published`, whether or not `doc:10-architecture#bounded-contexts` §3.1's
 * allowlist permits it to import `identity` at all.
 *
 * So the port returns the raw string the value classes wrap, and each context wraps it at
 * the call site: `ActorId(ids.newId())`. The type safety is recovered one line later
 * rather than lost — the value classes validate in their `init` block, so a malformed
 * value fails at the wrap instead of travelling as a plausible-looking id.
 *
 * A generic `fun <T> newId(wrap: (String) -> T): T` was rejected: it is this method plus a
 * call at the call site, with no guarantee the plain form lacks.
 *
 * (An earlier draft of `bean:0065` justified the `String` return by claiming
 * `rule:archunit/sharedKernelIsLeaf` would reject a typed one. That was wrong — the rule is
 * scoped by an exact name set and cannot see this package. The argument above is the real
 * one, and it needs no rule.)
 */
public interface IdGeneratorPort {
    /**
     * A fresh identifier, distinct from every value this generator has already returned.
     *
     * The returned value satisfies the shape every identifier value class requires: 1..64
     * characters of `a-z`, `0-9`, `.`, `_` and `-`, opening and closing alphanumeric. That
     * invariant is stated here and enforced where it is declared — in the value class's own
     * `init` block — because a port in a context-free package may not reference a context's
     * published language to reuse the pattern.
     *
     * Uniqueness is per generator instance and per process. This port promises no global
     * or cross-restart uniqueness; a caller needing that is asking for a different port.
     */
    public fun newId(): String
}
