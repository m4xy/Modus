package uk.m4xy.modus.core.domain.identity

/**
 * The outcome of one authorisation question, as a domain concept rather than a scatter of
 * `if` statements in a transport. Sealed and exhaustive, so a transport maps it with no
 * `else` branch.
 *
 * [domainIsVisible] is the 404-not-403 rule (`doc:00-constitution#domain-scoping`,
 * `doc:10-architecture#domain-root-convention` §5.3) expressed in the domain: a denial
 * that leaves the domain invisible MUST NOT be rendered as anything admitting it exists.
 */
public sealed class AccessDecision(
    /** The requested capability is granted. */
    public val isPermitted: Boolean,
    /** The actor is already entitled to know this domain exists. */
    public val domainIsVisible: Boolean,
) {
    /** A live grant covering this actor and domain carries the capability. */
    public object Permitted : AccessDecision(isPermitted = true, domainIsVisible = true)

    /**
     * No live grant covers this actor and domain, so the actor is not entitled to learn
     * whether the domain exists. Indistinguishable from a domain never created.
     */
    public object DomainNotVisible : AccessDecision(isPermitted = false, domainIsVisible = false)

    /** The actor sees the domain but no grant of its carries the capability. */
    public object CapabilityNotGranted : AccessDecision(isPermitted = false, domainIsVisible = true)
}
