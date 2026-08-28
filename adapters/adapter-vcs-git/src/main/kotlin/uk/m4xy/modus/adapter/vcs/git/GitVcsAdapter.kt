package uk.m4xy.modus.adapter.vcs.git

import uk.m4xy.modus.core.application.ListBoundedContexts

/**
 * Provisional placeholder for the GitVcs adapter.
 *
 * Git-backed repository operations: clone, branch, commit, push.
 *
 * Holds no behaviour yet; it exists so the module compiles and so the
 * architecture tests have a real class in this package.
 */
public class GitVcsAdapter(
    private val listBoundedContexts: ListBoundedContexts = ListBoundedContexts(),
) {
    public fun describe(): String = "GitVcs adapter, contexts=" + listBoundedContexts.handle(Unit)
}
