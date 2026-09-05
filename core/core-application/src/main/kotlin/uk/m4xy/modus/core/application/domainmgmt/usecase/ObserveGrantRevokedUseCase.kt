package uk.m4xy.modus.core.application.domainmgmt.usecase

import uk.m4xy.modus.core.application.event.DomainEventHandler
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.port.DomainRepository
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked

/** A `GrantRevoked` named a domain `domainmgmt` has never heard of. */
public class UnknownDomainOnGrantRevoked(
    domainId: DomainId,
) : IllegalStateException(
        "GrantRevoked names domain ${domainId.value}, which domainmgmt does not hold",
    )

/**
 * `domainmgmt`'s side of edge 1 of `doc:10-architecture#bounded-contexts` §3 — the only one
 * of the seven `Consumes` edges whose publisher, event type and consuming context all exist.
 *
 * **What it does today, stated plainly rather than dressed up:** it resolves the domain the
 * revoked grant named, and refuses an event naming a domain this context does not hold.
 * That is the whole of the reaction available, and it is a real one — a revocation against
 * an unknown domain is a broken publisher or a broken store, and `DomainRepository`'s own
 * contract says a `null` means "no such domain" and never "could not read", so the
 * distinction is decidable here.
 *
 * **What it does not do yet:** revise module visibility. `doc:10-architecture#bounded-contexts`
 * §3 gives `domainmgmt` "module installation, module visibility", and neither exists —
 * `ModuleInstallation` is `bean:0031`'s aggregate, and `core-domain/.../domainmgmt/README.md`
 * already records this handler as the thing `bean:0031` is waiting for. Writing a
 * visibility revision against an aggregate that does not exist would be one more entry in
 * the fiction this bean was raised to remove. The seam is here; the reaction lands with the
 * aggregate it acts on.
 *
 * **Published language only.** The imports above are the assertion:
 * `identity.event.GrantRevoked`, `DomainId` from the shared kernel, and this context's own
 * port. No `identity` aggregate, no `identity` port, no `identity` use case
 * (`doc:10-architecture#bounded-contexts` §3.1). `bean:0023` makes that mechanical; until it
 * lands, the import list is what a reviewer checks.
 */
public class ObserveGrantRevokedUseCase(
    private val domains: DomainRepository,
) : DomainEventHandler<GrantRevoked> {
    /**
     * Pre: none. Post: the domain [GrantRevoked.domainId] names has been resolved, or
     * [UnknownDomainOnGrantRevoked] has been thrown.
     *
     * Throwing rather than returning is deliberate, and it is the case that makes this
     * handler load-bearing rather than decorative: a handler that cannot fail cannot be
     * observed to have run.
     */
    override fun handle(event: GrantRevoked) {
        domains.findById(event.domainId) ?: throw UnknownDomainOnGrantRevoked(event.domainId)
    }
}
