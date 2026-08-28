package uk.m4xy.modus.core.application

/**
 * Provisional shape of a Modus use case: one command in, one result out.
 *
 * Ports (repositories, agent supervisors, clocks) will be declared as
 * interfaces alongside this one and implemented by adapters. Superseded by the
 * real application layer in a later work item.
 */
public fun interface UseCase<in COMMAND : Any, out RESULT> {
    public fun handle(command: COMMAND): RESULT
}
