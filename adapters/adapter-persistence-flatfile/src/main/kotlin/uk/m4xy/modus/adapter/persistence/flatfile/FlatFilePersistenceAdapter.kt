package uk.m4xy.modus.adapter.persistence.flatfile

import uk.m4xy.modus.core.application.ListBoundedContexts

/**
 * Provisional placeholder for the FlatFilePersistence adapter.
 *
 * Durable flat-file store. Flat files are the preferred storage for Modus; a database is not assumed.
 *
 * Holds no behaviour yet; it exists so the module compiles and so the
 * architecture tests have a real class in this package.
 */
public class FlatFilePersistenceAdapter(
    private val listBoundedContexts: ListBoundedContexts = ListBoundedContexts(),
) {
    public fun describe(): String = "FlatFilePersistence adapter, contexts=" + listBoundedContexts.handle(Unit)
}
