package uk.m4xy.modus.core.domain.execution

/**
 * Provisional marker for the `execution` bounded context.
 *
 * Runs of an agent against a work item: triggers, supervision, lifecycle and the output stream produced.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object ExecutionContext {
    public const val NAME: String = "execution"
}
