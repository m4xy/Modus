package uk.m4xy.modus.core.domain

import uk.m4xy.modus.core.domain.cost.CostContext
import uk.m4xy.modus.core.domain.execution.ExecutionContext
import uk.m4xy.modus.core.domain.memory.MemoryContext
import uk.m4xy.modus.core.domain.work.WorkContext

/**
 * Provisional index of the bounded contexts that make up the Modus domain.
 *
 * Replaced by real domain types in a later work item. `identity` and `domainmgmt` are
 * literals rather than marker references because their markers are gone: both contexts have
 * a real model, and an edge from here into either would close a package cycle back through
 * its `..event` package, which depends on [DomainEvent].
 *
 * It stays for now, deliberately (`bean:0009`, review thread 8). It is not dead code: it
 * is read by `ListBoundedContexts`, by `BeansModule` and `CostModule`, and through the use
 * case by three adapters, so removing it is a six-module change that belongs with the
 * removal of the other five markers, not with modelling one context. `doc:35-testing`
 * §8 also uses it as the worked example for two coverage evidence passages, which would
 * have to be re-derived. Its 31 `<clinit>` instructions do inflate the `:core-domain`
 * baseline, but the ratchet exists to make that deletion a reviewable one-line diff in
 * `config/coverage/baseline.tsv`, not to prevent it — and the behavioural floor that
 * would notice a real regression is the 100% aggregate-branch rule, which this type is
 * outside of.
 */
public object BoundedContexts {
    public val names: List<String> =
        listOf(
            "identity",
            "domainmgmt",
            WorkContext.NAME,
            MemoryContext.NAME,
            ExecutionContext.NAME,
            CostContext.NAME,
        )
}
