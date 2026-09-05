package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One call the production code made against the filesystem, as the seam saw it.
 *
 * [path] is the real, unwrapped path, so an assertion reads the same string a person would
 * see on disk.
 */
data class FilesystemCall(
    val kind: Kind,
    val path: Path,
) {
    enum class Kind {
        /** A channel was opened through [InstrumentedFileSystemProvider.newFileChannel]. */
        OPEN_FILE_CHANNEL,

        /** `FileChannel.force` returned normally. The descriptor is [path]. */
        FORCE,
    }
}

/**
 * What the instrumented filesystem saw.
 *
 * The recording is the whole point of the seam. `AtomicFileWriter` calls `force(true)` twice
 * and nothing in `bean:0147` could tell whether it did — both calls could be deleted with
 * its 31 tests staying green. This can tell, and the production class gains no parameter to
 * make it possible (`bean:0174`).
 *
 * Thread-safe: `AtomicFileWriter` is driven from several threads elsewhere in this suite.
 */
class FilesystemCalls {
    private val recorded = CopyOnWriteArrayList<FilesystemCall>()

    /** Every call, in the order it completed. */
    val calls: List<FilesystemCall> get() = recorded.toList()

    /** The paths that were forced, in order. Empty means no `fsync` reached the disk. */
    val forcedPaths: List<Path>
        get() = recorded.filter { it.kind == FilesystemCall.Kind.FORCE }.map { it.path }

    /**
     * How many channels the seam intercepted.
     *
     * This is the vacuity figure and it is not decoration. A recorder that saw no forces and
     * a recorder that was never on the path both report an empty [forcedPaths]; `bean:0051`
     * records that distinction being the entire difference between an inert check and a
     * passing one. Every test that asserts on [forcedPaths] asserts on this too.
     */
    val interceptedChannelOpens: Int
        get() = recorded.count { it.kind == FilesystemCall.Kind.OPEN_FILE_CHANNEL }

    internal fun record(
        kind: FilesystemCall.Kind,
        path: Path,
    ) {
        recorded += FilesystemCall(kind, path)
    }
}
