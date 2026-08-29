package uk.m4xy.modus.core.domain.domainmgmt.event

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import java.time.Instant

/**
 * A tenant now exists, with the process it imposes from the outset.
 *
 * It carries the [process] rather than leaving it to a following `ProcessDefinitionChanged`:
 * a domain has never been without one, so an event saying it changed at creation would
 * describe something that did not happen. `work` consumes this and
 * [ProcessDefinitionChanged] together and needs no port back into this context to know how
 * a domain's work moves (`doc:10-architecture#bounded-contexts` §3.1).
 */
public data class DomainCreated(
    public val domainId: DomainId,
    public val name: DomainName,
    public val process: ProcessDefinition,
    override val occurredAt: Instant,
) : DomainEvent

/**
 * A domain changed how its work moves. `work` consumes this
 * (`doc:10-architecture#bounded-contexts`).
 *
 * It carries the whole definition, not a version to re-read, because a consumer cannot
 * guard a transition without it — and every consumer needing a port back here to fetch one
 * is the coupling the published-language split exists to prevent.
 *
 * Raised only when the definition actually differs. Adopting the process a domain already
 * has raises nothing: an event is a statement that something happened.
 */
public data class ProcessDefinitionChanged(
    public val domainId: DomainId,
    public val process: ProcessDefinition,
    override val occurredAt: Instant,
) : DomainEvent
