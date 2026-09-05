package uk.m4xy.modus.adapter.persistence.flatfile

import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.file.Path

/**
 * A **second process** that takes the store's cross-process lock and holds it.
 *
 * `doc:40-durability` §6.3's lock is a [java.nio.channels.FileLock], which is held by the
 * JVM and not by a thread: a second thread of the same process gets
 * [java.nio.channels.OverlappingFileLockException], never contention. So a second thread
 * cannot stand in for a second process here, and the bean's criterion — hold the lock from
 * a second process — is only satisfiable by launching one.
 *
 * The handshake is the process's own pipes rather than a marker file, so nothing polls and
 * nothing sleeps: the parent's `readLine()` blocks until this writes, and this blocks on
 * `System.in` until the parent replies. It writes through a raw [FileOutputStream] on
 * [FileDescriptor.out] rather than `println`, so `rule:archunit/nothingWritesToTheStandardStreams`
 * — which binds every class in the repository, not only production ones — has nothing to
 * catch even if this source set is ever put on the analysed classpath.
 */
object LockHolderProcess {
    /** Written to stdout once the lock is held, and the only thing this ever writes. */
    const val HELD: String = "HELD"

    @JvmStatic
    fun main(args: Array<String>) {
        val locksDirectory = Path.of(args[0])
        val target = Path.of(args[1])

        CrossProcessLock(locksDirectory).exclusive(target) {
            FileOutputStream(FileDescriptor.out).use { out ->
                out.write("$HELD\n".toByteArray())
                out.flush()
            }
            // Blocks until the parent writes a byte or closes the pipe. The lock is held
            // for exactly as long as the parent needs it to be.
            System.`in`.read()
        }
    }
}
