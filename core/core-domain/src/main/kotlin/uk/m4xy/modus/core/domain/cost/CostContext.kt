package uk.m4xy.modus.core.domain.cost

/**
 * Provisional marker for the `cost` bounded context.
 *
 * Spend incurred by executions: token accounting, budgets and the limits a domain enforces.
 *
 * Exists so the package is present in the compiled output and the architecture
 * tests have something to inspect. Delete it as soon as the context has a real
 * aggregate root.
 */
public object CostContext {
    public const val NAME: String = "cost"
}
