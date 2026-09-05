package uk.m4xy.modus.core.application.identity.usecase

import uk.m4xy.modus.core.application.UseCase
import uk.m4xy.modus.core.application.event.WriteThenDispatch
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.identity.port.PermissionGrantRepository
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import uk.m4xy.modus.core.domain.port.ClockPort

/**
 * Withdraw one actor's access to one domain.
 *
 * Named by actor and domain as well as by grant because `PermissionGrantRepository` has no
 * `findById`: its reads are all domain-scoped, deliberately, so that no read can return a
 * grant the caller was not already entitled to name
 * (`doc:00-constitution#domain-scoping`). Adding a global `findById` for the convenience of
 * this use case would be the first read in the system that is not scoped by a domain.
 */
public data class RevokeGrantCommand(
    public val grantId: GrantId,
    public val actorId: ActorId,
    public val domainId: DomainId,
)

/** No live or revoked grant with that id is held by that actor on that domain. */
public class NoSuchGrant(
    command: RevokeGrantCommand,
) : IllegalArgumentException(
        "no grant ${command.grantId.value} for actor ${command.actorId.value} on domain ${command.domainId.value}",
    )

/**
 * The first use case in Modus to write an aggregate and dispatch what it raised, and the
 * end-to-end proof of edge 1 of `doc:10-architecture#bounded-contexts` §3: `identity`
 * publishes `GrantRevoked`, `domainmgmt` consumes it.
 *
 * It owns no ordering of its own. [WriteThenDispatch] does, which is the point: the next use
 * case, in whichever context, inherits the ordering by using the same collaborator rather
 * than by remembering the rule.
 */
public class RevokeGrantUseCase(
    private val grants: PermissionGrantRepository,
    private val clock: ClockPort,
    private val write: WriteThenDispatch,
) : UseCase<RevokeGrantCommand, Unit> {
    /**
     * Pre: the grant exists and is live. Post: it permits nothing, the store holds the
     * revoked grant, and `GrantRevoked` has been dispatched exactly once.
     *
     * Revoking an already-revoked grant is refused by `PermissionGrant.revoke`'s own
     * `check`, before anything is written — so a repeated command neither writes nor
     * dispatches.
     */
    override fun handle(command: RevokeGrantCommand) {
        val grant =
            grants
                .grantsFor(command.actorId, command.domainId)
                .firstOrNull { it.id == command.grantId }
                ?: throw NoSuchGrant(command)
        grant.revoke(clock.now())
        write.write(grant) { grants.save(it) }
    }
}
