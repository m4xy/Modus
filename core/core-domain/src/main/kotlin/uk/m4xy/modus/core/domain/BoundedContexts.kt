package uk.m4xy.modus.core.domain

import uk.m4xy.modus.core.domain.cost.CostContext
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtContext
import uk.m4xy.modus.core.domain.execution.ExecutionContext
import uk.m4xy.modus.core.domain.memory.MemoryContext
import uk.m4xy.modus.core.domain.work.WorkContext

/**
 * Provisional index of the bounded contexts that make up the Modus domain.
 *
 * Replaced by real domain types in a later work item. `identity` is a literal rather than
 * a marker reference because its marker is gone: the context has a real model, and an
 * edge from here into it would close a package cycle back through
 * `uk.m4xy.modus.core.domain.identity.event`, which depends on [DomainEvent].
 */
public object BoundedContexts {
    public val names: List<String> =
        listOf(
            "identity",
            DomainMgmtContext.NAME,
            WorkContext.NAME,
            MemoryContext.NAME,
            ExecutionContext.NAME,
            CostContext.NAME,
        )
}
