package uk.m4xy.modus.core.domain.domainmgmt

/**
 * Provisional marker for the `domainmgmt` bounded context.
 *
 * Domains themselves: creation, configuration, the rules a domain imposes, and which Modules are installed in it.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object DomainMgmtContext {
    public const val NAME: String = "domainmgmt"
}
