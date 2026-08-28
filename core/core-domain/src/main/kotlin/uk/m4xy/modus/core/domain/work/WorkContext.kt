package uk.m4xy.modus.core.domain.work

/**
 * Provisional marker for the `work` bounded context.
 *
 * Work items — the markdown-backed units of work Modus tracks, their lifecycle, and the relationships between them.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object WorkContext {
    public const val NAME: String = "work"
}
