package uk.m4xy.modus.core.domain.identity

import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AGENTS_RUN
import uk.m4xy.modus.core.domain.identity.IdentityFixture.ALICE
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AT
import uk.m4xy.modus.core.domain.identity.IdentityFixture.MODUS
import uk.m4xy.modus.core.domain.identity.aggregate.Actor
import uk.m4xy.modus.core.domain.identity.event.ActorRegistered
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import kotlin.test.Test

class ActorTest {
    @Test
    fun `registering an actor raises ActorRegistered at the instant it was given`() {
        val actor = Actor.register(ALICE, ActorKind.AGENT, AT)

        actor.id shouldBe ALICE
        actor.kind shouldBe ActorKind.AGENT

        val registered = actor.pendingEvents.single() as ActorRegistered
        registered.actorId shouldBe ALICE
        registered.kind shouldBe ActorKind.AGENT
        registered.occurredAt shouldBe AT
    }

    @Test
    fun `a registered actor can reach no domain until a grant says otherwise`() {
        Actor.register(ALICE, ActorKind.HUMAN, AT)

        PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, emptyList()) shouldBe
            AccessDecision.DomainNotVisible
    }
}
