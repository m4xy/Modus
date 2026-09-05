package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Path

/**
 * A real [FileChannel] with two instruments attached: it records every `force`, and it can
 * be asked to write fewer bytes than it was given.
 *
 * Both instruments observe or constrain the *channel*. Neither touches the code under test,
 * which is the property that makes this seam admissible where a strategy object is not
 * (`bean:0174`): production resolves the default filesystem, gets an ordinary channel, and
 * cannot be configured into a state where a `force` does not happen.
 *
 * **What this seam does NOT see, stated so `bean:0175` inherits the hazard rather than
 * discovering it.** Every item is a live trap for the next bean, not a caveat.
 *
 * - **`newByteChannel` is not instrumented**, and so neither are `Files.write`,
 *   `Files.newOutputStream`, `Files.newBufferedWriter` or `Files.copy`. This is the one that
 *   will bite: an `O_APPEND` appender written the idiomatic way goes through
 *   `newByteChannel`, and every durability assertion then reads "no forces missing" while
 *   observing nothing — the vacuity failure this seam exists to prevent, one bean
 *   downstream. `bean:0175`'s appender must open a `FileChannel` explicitly, and its tests
 *   must assert `interceptedChannelOpens` is non-zero.
 * - **`shortWriteLimit` constrains `write(ByteBuffer)` and nothing else.**
 *   `write(src, position)`, the gathering `write(srcs, offset, length)`, `transferFrom` and
 *   `map` all bypass it, so a loop driven through any of them never short-writes and
 *   `bean:0175`'s criterion 2 would pass without exercising the loop at all.
 * - **`lock` and `tryLock` return a `FileLock` bound to the delegate**, so `acquiredBy()` is
 *   not this object. Harmless for `CrossProcessLock`, which only releases it; an assertion
 *   on the lock's channel identity would be wrong.
 * - **`newWatchService`, `Path.register`, `getFileStore`, `getFileStores`, `toFile` and
 *   `toUri` are forwarded unwrapped** and driven by no test. A `File` obtained from `toFile`
 *   leaves the seam entirely.
 *
 * None of these is a defect today: nothing here uses them, and instrumenting every path into
 * the filesystem before there is a caller is speculative surface with no test to hold it
 * honest. They are written down because the failure they produce is **silent**, and a silent
 * gap nobody wrote down is indistinguishable from a mechanism that works.
 *
 * @param shortWriteLimit the most bytes a single [write] will consume, or null for the real
 *   channel's behaviour. `doc:40-durability` §4.2 says `FileChannel.write` may perform a
 *   short write and that no API can assert a single syscall; this makes that happen on
 *   demand, for `bean:0175`.
 */
class InstrumentedFileChannel(
    private val delegate: FileChannel,
    private val path: Path,
    private val calls: FilesystemCalls,
    private val shortWriteLimit: Int? = null,
) : FileChannel() {
    override fun force(metaData: Boolean) {
        delegate.force(metaData)
        // Recorded AFTER the delegate returns, so a force that threw is not recorded as
        // having happened.
        calls.record(FilesystemCall.Kind.FORCE, path)
    }

    override fun write(src: ByteBuffer): Int {
        val limit = shortWriteLimit ?: return delegate.write(src)
        if (!src.hasRemaining()) {
            return delegate.write(src)
        }
        // Hand the delegate a view of at most `limit` bytes, then advance the caller's
        // buffer by exactly what was taken. A caller that ignores the return value and
        // assumes the whole buffer went writes a truncated record.
        val allowed = minOf(limit, src.remaining())
        val slice = src.slice().limit(allowed)
        val written = delegate.write(slice)
        src.position(src.position() + written)
        return written
    }

    override fun write(
        src: ByteBuffer,
        position: Long,
    ): Int = delegate.write(src, position)

    override fun write(
        srcs: Array<out ByteBuffer>,
        offset: Int,
        length: Int,
    ): Long = delegate.write(srcs, offset, length)

    override fun read(dst: ByteBuffer): Int = delegate.read(dst)

    override fun read(
        dst: ByteBuffer,
        position: Long,
    ): Int = delegate.read(dst, position)

    override fun read(
        dsts: Array<out ByteBuffer>,
        offset: Int,
        length: Int,
    ): Long = delegate.read(dsts, offset, length)

    override fun position(): Long = delegate.position()

    override fun position(newPosition: Long): FileChannel {
        delegate.position(newPosition)
        return this
    }

    override fun size(): Long = delegate.size()

    override fun truncate(size: Long): FileChannel {
        delegate.truncate(size)
        return this
    }

    override fun transferTo(
        position: Long,
        count: Long,
        target: WritableByteChannel,
    ): Long = delegate.transferTo(position, count, target)

    override fun transferFrom(
        src: ReadableByteChannel,
        position: Long,
        count: Long,
    ): Long = delegate.transferFrom(src, position, count)

    override fun map(
        mode: MapMode,
        position: Long,
        size: Long,
    ): MappedByteBuffer = delegate.map(mode, position, size)

    override fun lock(
        position: Long,
        size: Long,
        shared: Boolean,
    ): FileLock = delegate.lock(position, size, shared)

    override fun tryLock(
        position: Long,
        size: Long,
        shared: Boolean,
    ): FileLock? = delegate.tryLock(position, size, shared)

    override fun implCloseChannel() {
        delegate.close()
    }
}
