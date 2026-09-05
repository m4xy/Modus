package uk.m4xy.modus.adapter.persistence.flatfile

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Which descriptor a durability barrier was applied to.
 *
 * The two values are the two `fsync` calls of `doc:40-durability#atomic-write`: step 4 on
 * the temp file, step 7 on the directory the rename published into.
 */
public enum class ForcedDescriptor {
    /** Step 4: the temp file's own descriptor, before the rename. */
    TEMP_FILE,

    /** Step 7: the target's parent directory, after the rename. */
    PARENT_DIRECTORY,
}

/**
 * Notified after each `fsync` [AtomicFileWriter] performs, with the path it was applied to.
 *
 * It exists because durability is otherwise unobservable: a test can assert that a force
 * *happened*, in what order, and against what state of the directory, but no test on this
 * side of the syscall can assert that the platform then honoured it.
 *
 * Deliberately an observer and not a strategy. It is called **after** the real force, so
 * nothing passed here can make the writer skip one — a seam that could disable the
 * mechanism would make every test of that mechanism a test of the seam
 * (`doc:00-constitution#observed-failing`).
 */
public fun interface SyncObserver {
    public fun forced(
        descriptor: ForcedDescriptor,
        path: Path,
    )

    public companion object {
        /** Observes nothing. The production default. */
        public val NONE: SyncObserver = SyncObserver { _, _ -> }
    }
}

/**
 * The one place in Modus that writes bytes to a target path
 * (`doc:40-durability#atomic-write`).
 *
 * Every step of §4's sequence is here and none is optional. Step 7 — forcing the parent
 * directory after the rename — is the one that gets skipped: without it POSIX permits the
 * directory entry to be lost in a crash even though the file's data was synced, leaving a
 * durable file nobody can find and the old content still visible.
 *
 * **A failed write deliberately leaves its temp file behind.** §4.1 states what a crash
 * leaves — the previous version intact plus an orphan `.tmp` — and §7 makes sweeping those
 * a startup action. Deleting the temp in a `catch` would make the swept-for condition
 * unreachable from any failure this code can see, and it cannot be reached from a real
 * crash either, because a crash runs no `catch` block.
 */
public class AtomicFileWriter(
    private val observer: SyncObserver = SyncObserver.NONE,
) {
    /**
     * Replaces [target] with [bytes], atomically, or leaves it exactly as it was.
     *
     * A reader concurrent with this call sees the whole previous version or the whole new
     * one — never an empty file and never a mixture — because the only thing that touches
     * [target] is the rename.
     */
    public fun write(
        target: Path,
        bytes: ByteArray,
    ) {
        val directory =
            requireNotNull(target.parent) {
                "an atomic write needs a parent directory to rename within: $target"
            }

        // Step 2: the temp file goes in the TARGET'S OWN DIRECTORY. Not the system temp
        // directory, and not a `.modus/tmp` — rename is only atomic within one filesystem,
        // and any other location is a copy that can tear.
        val temp = Files.createTempFile(directory, "${target.fileName}.", TEMP_SUFFIX)

        FileChannel.open(temp, StandardOpenOption.WRITE).use { channel ->
            // Step 3: every byte, looping. FileChannel.write may perform a short write and
            // there is no API that asserts a single syscall (§4.2), so the loop is the
            // mechanism rather than a defensive flourish.
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
            // Step 4: the data is on stable storage before anything points at it.
            channel.force(true)
            observer.forced(ForcedDescriptor.TEMP_FILE, temp)
        }

        // Step 6: the atomic publish. REPLACE_EXISTING because a document is written whole
        // (§2.1.6); ATOMIC_MOVE because a fallback copy is exactly the tear this avoids.
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)

        // Step 7: the rename itself is durable only once the directory is forced. A
        // directory is opened READ rather than WRITE — POSIX has no writable directory
        // descriptor, and READ is what Linux and macOS accept here. Windows rejects it,
        // and is not a platform Modus claims: the failure is loud rather than swallowed.
        FileChannel.open(directory, StandardOpenOption.READ).use { channel ->
            channel.force(true)
            observer.forced(ForcedDescriptor.PARENT_DIRECTORY, directory)
        }
    }

    public companion object {
        /** The suffix §4.1 and §7 name, so the recovery sweep can recognise an orphan. */
        public const val TEMP_SUFFIX: String = ".tmp"
    }
}
