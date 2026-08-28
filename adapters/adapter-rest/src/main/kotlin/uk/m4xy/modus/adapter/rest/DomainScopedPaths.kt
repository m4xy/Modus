package uk.m4xy.modus.adapter.rest

/**
 * Provisional home for the one routing fact that is already decided: every
 * Modus API path is scoped to a domain.
 */
public object DomainScopedPaths {
    public const val DOMAIN_ROOT: String = "/domains/{domainId}"
}
