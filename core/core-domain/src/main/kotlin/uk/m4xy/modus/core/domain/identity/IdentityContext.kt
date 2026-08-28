package uk.m4xy.modus.core.domain.identity

/**
 * Provisional marker for the `identity` bounded context.
 *
 * Who is acting: users, agents, service principals, and the credentials and permissions attached to them.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object IdentityContext {
    public const val NAME: String = "identity"
}
