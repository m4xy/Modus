package uk.m4xy.modus.adapter.persistence.flatfile

import java.nio.file.Files
import java.nio.file.Path

/**
 * A document as the store hands it out: its bytes, and the [DocumentVersion] a write based
 * on them must be conditional on.
 *
 * The two travel together deliberately. §6.4's check is only as good as the version the
 * caller quotes, and a caller that has to compute one for itself will eventually quote the
 * version of the bytes it *wrote* rather than of the bytes it *read*.
 */
public class StoredDocument(
    private val content: ByteArray,
    public val version: DocumentVersion,
) {
    /** A fresh copy every read: the caller cannot mutate the store's answer. */
    public val bytes: ByteArray get() = content.copyOf()
}

/**
 * The store's public surface for documents (`doc:40-durability` §2.1, §4, §6).
 *
 * Not a hexagonal port and it must not become one. `doc:00-constitution` §1.2 puts a port
 * where the domain or a use case calls it; nothing outside this adapter calls
 * `write(path, expected, bytes)` — `ActorRepository` and `PermissionGrantRepository` are
 * what the outside sees, and they are built on this. Declaring a path-and-bytes interface
 * in `core-domain` would put `java.nio.file` in the domain's vocabulary, which §1.3 forbids
 * outright. "The store's public port" in §6.4 names this surface, not a layer.
 *
 * Three properties are structural rather than documented:
 *
 * - **There is no unconditional write.** [write] takes a [DocumentVersion], and
 *   [DocumentVersion.ABSENT] is how a create states what it expected to find. §6.4 says
 *   "there is no unconditional overwrite API"; the way to have none is not to write one.
 * - **Reads take no lock** (§6.1). The rename gives a reader a complete version for free.
 * - **The in-process lock is taken before the cross-process one.** See [CrossProcessLock].
 */
public class DocumentStore(
    root: Path,
    private val locks: PathLocks = PathLocks(),
    private val writer: AtomicFileWriter = AtomicFileWriter(),
) {
    private val root: Path = PathLocks.canonical(root)
    private val crossProcess = CrossProcessLock(this.root.resolve(LOCKS_DIRECTORY))

    /**
     * The document at [target], or null if there is none.
     *
     * Lock-free by design (§6.1). A concurrent write is a rename, so this sees the whole
     * previous version or the whole new one.
     */
    public fun read(target: Path): StoredDocument? {
        val path = within(target)
        if (!Files.isRegularFile(path)) {
            return null
        }
        val bytes = Files.readAllBytes(path)
        return StoredDocument(bytes, DocumentVersion.of(bytes))
    }

    /**
     * Replaces the document at [target] with [bytes], provided it still holds [expected].
     *
     * Returns the version the caller may base its next write on. Throws
     * [StaleWriteException] if someone else wrote in between — the caller re-reads,
     * re-applies and decides for itself whether retrying is safe (§6.4).
     */
    public fun write(
        target: Path,
        expected: DocumentVersion,
        bytes: ByteArray,
    ): DocumentVersion {
        val path = within(target)
        return locks.exclusive(path) {
            crossProcess.exclusive(path) {
                // Re-read and re-hash UNDER the lock. Checking against the version the
                // caller happens to be holding would be checking against a read that
                // finished before the lock was taken, which is the race §6.4 exists to
                // close rather than to move.
                val current = read(path)?.version ?: DocumentVersion.ABSENT
                if (current != expected) {
                    throw StaleWriteException(path, expected, current)
                }
                writer.write(path, bytes)
                DocumentVersion.of(bytes)
            }
        }
    }

    /**
     * [target], canonicalised, having established that it is inside the store root.
     *
     * An identifier arrives at this adapter authorised to be used unencoded as a path
     * segment (`bean:0009` review thread 6), so this is the last thing between a crafted
     * one and a write anywhere on the volume. Normalisation happens before the check, not
     * after: `root/../../etc/passwd` is inside the root only until it is resolved.
     */
    private fun within(target: Path): Path {
        val path = PathLocks.canonical(target)
        if (!path.startsWith(root)) {
            throw PathOutsideStoreException(path, root)
        }
        return path
    }

    public companion object {
        /** §3: lock files live here, and `.modus/` is git-ignored in its entirety. */
        public const val LOCKS_DIRECTORY: String = ".modus/locks"
    }
}
