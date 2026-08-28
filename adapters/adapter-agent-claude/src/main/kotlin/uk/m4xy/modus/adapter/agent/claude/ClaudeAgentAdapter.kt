package uk.m4xy.modus.adapter.agent.claude

import uk.m4xy.modus.core.application.ListBoundedContexts

/**
 * Provisional placeholder for the ClaudeAgent adapter.
 *
 * Supervises Claude Code processes and streams their output back to callers.
 *
 * Holds no behaviour yet; it exists so the module compiles and so the
 * architecture tests have a real class in this package.
 */
public class ClaudeAgentAdapter(
    private val listBoundedContexts: ListBoundedContexts = ListBoundedContexts(),
) {
    public fun describe(): String = "ClaudeAgent adapter, contexts=" + listBoundedContexts.handle(Unit)
}
