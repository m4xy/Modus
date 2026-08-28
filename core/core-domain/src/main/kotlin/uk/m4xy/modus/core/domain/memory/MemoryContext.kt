package uk.m4xy.modus.core.domain.memory

/**
 * Provisional marker for the `memory` bounded context.
 *
 * Durable knowledge a domain accumulates: notes, decisions, and retrieved context offered to agents.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object MemoryContext {
    public const val NAME: String = "memory"
}
