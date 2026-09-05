package uk.m4xy.modus.adapter.persistence.flatfile

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

/**
 * The cross-process half of `doc:40-durability` §6.2–§6.3: an advisory [java.nio.channels.FileLock]
 * on a sidecar file in `.modus/locks/`, named after a hash of the target path.
 *
 * A sidecar rather than a lock on the document itself, because the atomic write replaces
 * the document by rename: a lock held on the old inode says nothing about the new one.
 *
 * **This is the second lock, never the first.** A `FileLock` is held by the *JVM*, not by
 * the thread, so two threads of one process asking for the same region get
 * [java.nio.channels.OverlappingFileLockException] rather than one waiting for the other.
 * [DocumentStore] therefore takes [PathLocks] first, which reduces this process to a single
 * contender before it ever gets here. That exception is deliberately not caught and not
 * translated: it means the ordering was inverted, which is a defect in Modus rather than
 * contention with another process, and the two must not report as the same thing.
 *
 * Breaking a lock whose holder is dead is a startup-recovery action (§6.3, §7) and belongs
 * to `bean:0133`. This class refuses a held lock and never breaks one.
 */
public class CrossProcessLock(
    private val locksDirectory: Path,
) {
    /**
     * Runs [action] holding the exclusive cross-process lock for [target].
     *
     * Throws [StoreContentionException] if another process holds it. `tryLock` and not
     * `lock`: §6.2 forbids an acquisition that can block indefinitely, and a lock held by a
     * process this one cannot see is precisely the case where blocking would.
     */
    public fun <T> exclusive(
        target: Path,
        action: () -> T,
    ): T {
        Files.createDirectories(locksDirectory)
        val sidecar = locksDirectory.resolve(sidecarNameFor(target))
        FileChannel.open(sidecar, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val held =
                channel.tryLock()
                    ?: throw StoreContentionException("another process holds the store lock for $target")
            held.use { return action() }
        }
    }

    public companion object {
        private const val ALGORITHM = "SHA-256"

        /**
         * The sidecar file name for [target]: a hash, so a path of any length or shape
         * becomes one file name that no filesystem can refuse and that cannot itself
         * traverse anywhere.
         */
        public fun sidecarNameFor(target: Path): String {
            val canonical = PathLocks.canonical(target).toString()
            val digest = MessageDigest.getInstance(ALGORITHM).digest(canonical.toByteArray())
            return digest.joinToString("") { byte -> "%02x".format(byte) } + ".lock"
        }
    }
}
