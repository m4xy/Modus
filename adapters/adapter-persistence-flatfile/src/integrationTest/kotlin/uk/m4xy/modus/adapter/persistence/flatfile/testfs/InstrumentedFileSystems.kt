package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * A filesystem that is the real one, watched.
 *
 * The only entry point a test needs. Build one, ask it for a path under a `@TempDir`, and
 * hand that path to production code: the bytes land on the real disk, and [calls] records
 * what the production code asked the filesystem to do.
 *
 * ```
 * val watched = InstrumentedFileSystems.watching()
 * AtomicFileWriter().write(watched.pathTo(directory.resolve("actor.md")), bytes)
 * watched.calls.forcedPaths.size shouldBe 2
 * ```
 *
 * @param shortWriteLimit the most bytes one `FileChannel.write` will consume, or null for
 *   the real behaviour. `doc:40-durability` §4.2 requires the append path to loop until
 *   every byte is written; this is how that is provoked rather than argued (`bean:0175`).
 */
class InstrumentedFileSystems private constructor(
    shortWriteLimit: Int?,
) {
    /** What the production code asked the filesystem to do. */
    val calls: FilesystemCalls = FilesystemCalls()

    private val provider =
        InstrumentedFileSystemProvider(
            delegate = FileSystems.getDefault().provider(),
            calls = calls,
            shortWriteLimit = shortWriteLimit,
        )

    /**
     * [path], rebound to this filesystem.
     *
     * The returned path is equal, as a string, to the one passed in; what changes is that
     * `getFileSystem().provider()` now answers with the instrumented provider, which is what
     * `FileChannel.open` dispatches through.
     */
    fun pathTo(path: Path): Path = provider.fileSystem().wrap(InstrumentedPath.unwrap(path))

    companion object {
        /** A filesystem that records and constrains nothing beyond recording. */
        fun watching(): InstrumentedFileSystems = InstrumentedFileSystems(shortWriteLimit = null)

        /**
         * A filesystem whose channels consume at most [bytes] per `write` call.
         *
         * Not used by `bean:0174`, which owns only the recording half. It ships here rather
         * than in `bean:0175` because a seam with one instrument is a seam that gets rebuilt
         * for the second; `InstrumentedFileSystemIntegrationTest` drives it, so it is live
         * code rather than an unexercised parameter waiting for its first caller.
         *
         * **It constrains `write(ByteBuffer)` and nothing else** — see the boundary note on
         * [InstrumentedFileChannel]. `bean:0175`'s appender must write through that overload
         * or its short-write criterion passes vacuously.
         */
        fun writingAtMost(bytes: Int): InstrumentedFileSystems = InstrumentedFileSystems(shortWriteLimit = bytes)
    }
}
