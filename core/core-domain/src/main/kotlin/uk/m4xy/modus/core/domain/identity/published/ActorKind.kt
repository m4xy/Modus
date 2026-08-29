package uk.m4xy.modus.core.domain.identity.published

/**
 * What kind of principal an actor is.
 *
 * It confers no privilege of its own — an agent is authorised by its grants exactly as a
 * human is — but it is the attribution recorded against every run and every spend entry.
 */
public enum class ActorKind {
    HUMAN,
    AGENT,
}
