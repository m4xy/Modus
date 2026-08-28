package uk.m4xy.modus.core.domain

import uk.m4xy.modus.core.domain.cost.CostContext
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtContext
import uk.m4xy.modus.core.domain.execution.ExecutionContext
import uk.m4xy.modus.core.domain.identity.IdentityContext
import uk.m4xy.modus.core.domain.memory.MemoryContext
import uk.m4xy.modus.core.domain.work.WorkContext

/**
 * Provisional index of the bounded contexts that make up the Modus domain.
 *
 * Replaced by real domain types in a later work item.
 */
public object BoundedContexts {
    public val names: List<String> =
        listOf(
            IdentityContext.NAME,
            DomainMgmtContext.NAME,
            WorkContext.NAME,
            MemoryContext.NAME,
            ExecutionContext.NAME,
            CostContext.NAME,
        )
}
