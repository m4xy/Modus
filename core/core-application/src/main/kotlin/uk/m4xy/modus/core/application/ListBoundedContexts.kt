package uk.m4xy.modus.core.application

import uk.m4xy.modus.core.domain.BoundedContexts

/**
 * Provisional use case, kept only so the application layer has a real, compiled
 * dependency on the domain for the architecture tests to observe.
 */
public class ListBoundedContexts : UseCase<Unit, List<String>> {
    override fun handle(command: Unit): List<String> = BoundedContexts.names
}
