package uk.m4xy.modus.module.cost

import uk.m4xy.modus.core.domain.BoundedContexts

/**
 * Provisional descriptor for the Cost module.
 *
 * LLM spend tracking and per-domain budgets.
 *
 * Modules are installed per domain and may be invisible to other domains; the
 * installation model itself is owned by a later work item.
 */
public object CostModule {
    public const val ID: String = "cost"

    public fun knownContexts(): List<String> = BoundedContexts.names
}
