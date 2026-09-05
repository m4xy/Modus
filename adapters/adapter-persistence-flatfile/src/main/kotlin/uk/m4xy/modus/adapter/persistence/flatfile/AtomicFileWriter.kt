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
 * Notified at each of the two points where [AtomicFileWriter] forces a descriptor, with the
 * path that was forced.
 *
 * **What a test built on this can and cannot decide, stated exactly, because an earlier
 * version of this KDoc got it wrong.** It can decide the ORDER of the two notifications and
 * the state of the directory at each one — that the temp file sits in the target's own
 * directory, that the target still holds the previous version at the first, and that it
 * holds the new one at the second. Those are real properties and the assertions on them are
 * load-bearing.
 *
 * It **cannot** decide that `channel.force(true)` was called. This notification is a
 * separate statement standing next to the force, not a consequence of it: delete either
 * `force` and leave the notification, and the whole suite stays green — observed, twice, in
 * `bean:0147`'s review. So a test here is evidence about the **sequence of steps**, and the
 * force inside a step is unevidenced. `doc:00-constitution#observed-failing` says a
 * mechanism nobody has watched reject a violation is a claim, and the honest form is an
 * admitted gap: `bean:0150` owns it, because the `SIGKILL`-at-randomised-points test
 * `doc:40-durability` §5 asks for is the only thing in the plan that could detect a missing
 * `fsync`.
 *
 * The distance between the two is why it is an observer and not a strategy: it is called
 * **after** the real force, so nothing passed here can make the writer skip one. A seam that
 * could disable the mechanism would make every test of that mechanism a test of the seam —
 * and, as the plants above show, the price of that safety is that it cannot witness the
 * mechanism either.
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
 * **Neither `force` is covered by a test, and JaCoCo says otherwise.** The module reports 0
 * missed instructions and 0 missed branches here, and both `channel.force(true)` calls can
 * be deleted with the suite staying green (`bean:0147`, criterion 1). Line coverage counts
 * execution, not consequence. `bean:0150` owns the gap; see [SyncObserver] for what the
 * tests do establish.
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
