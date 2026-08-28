package uk.m4xy.modus.module.beans

import uk.m4xy.modus.core.domain.BoundedContexts

/**
 * Provisional descriptor for the Beans module.
 *
 * Work tracking: markdown work items following the hmans/beans schema.
 *
 * Modules are installed per domain and may be invisible to other domains; the
 * installation model itself is owned by a later work item.
 */
public object BeansModule {
    public const val ID: String = "beans"

    public fun knownContexts(): List<String> = BoundedContexts.names
}
