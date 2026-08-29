package uk.m4xy.modus.core.domain

import java.time.Instant

/**
 * Something a bounded context has decided has happened.
 *
 * Raised by an aggregate root and accumulated on it; drained and dispatched by the
 * application layer once the write is durable (`doc:20-ddd-practices#domain-events`).
 * The domain never dispatches, and never asks what time it is: [occurredAt] is supplied
 * by the caller.
 */
public interface DomainEvent {
    public val occurredAt: Instant
}
