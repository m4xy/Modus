package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

/**
 * A [Path] that is the real path in every respect except the one that matters:
 * [getFileSystem] answers with the instrumented filesystem, so
 * `FileChannel.open(path, …)` dispatches to [InstrumentedFileSystemProvider].
 *
 * That dispatch is the whole seam and it is a JDK guarantee rather than a trick —
 * `FileChannel.open(Path, Set, FileAttribute...)` delegates to
 * `path.getFileSystem().provider().newFileChannel(...)`
 * (`java.base/java/nio/channels/FileChannel.java:300-301`, JDK 25).
 *
 * **Every operation that returns a path returns a wrapped one.** An override that leaks a
 * delegate path is the defect this class invites: the leak compiles, the test still passes,
 * and it silently stops exercising the seam from that point on.
 * `InstrumentedFileSystemIntegrationTest` asserts the wrapping survives every path-returning
 * method on this class and on the provider and filesystem beside it, because reading the
 * forwarding by eye is exactly how such a defect is missed — and because leaking seven of
 * them at once was measured giving 41 tests and 0 failures before those assertions existed.
 */
class InstrumentedPath internal constructor(
    internal val delegate: Path,
    private val fileSystem: InstrumentedFileSystem,
) : Path {
    private fun wrap(path: Path): InstrumentedPath = InstrumentedPath(path, fileSystem)

    private fun wrapOrNull(path: Path?): InstrumentedPath? = path?.let { wrap(it) }

    override fun getFileSystem(): FileSystem = fileSystem

    override fun isAbsolute(): Boolean = delegate.isAbsolute

    override fun getRoot(): Path? = wrapOrNull(delegate.root)

    override fun getFileName(): Path? = wrapOrNull(delegate.fileName)

    override fun getParent(): Path? = wrapOrNull(delegate.parent)

    override fun getNameCount(): Int = delegate.nameCount

    override fun getName(index: Int): Path = wrap(delegate.getName(index))

    override fun subpath(
        beginIndex: Int,
        endIndex: Int,
    ): Path = wrap(delegate.subpath(beginIndex, endIndex))

    override fun startsWith(other: Path): Boolean = delegate.startsWith(unwrap(other))

    override fun endsWith(other: Path): Boolean = delegate.endsWith(unwrap(other))

    override fun normalize(): Path = wrap(delegate.normalize())

    override fun resolve(other: Path): Path = wrap(delegate.resolve(unwrap(other)))

    override fun relativize(other: Path): Path = wrap(delegate.relativize(unwrap(other)))

    override fun toUri(): URI = delegate.toUri()

    override fun toAbsolutePath(): Path = wrap(delegate.toAbsolutePath())

    override fun toRealPath(vararg options: LinkOption): Path = wrap(delegate.toRealPath(*options))

    override fun toFile(): File = delegate.toFile()

    override fun register(
        watcher: WatchService,
        events: Array<out WatchEvent.Kind<*>>,
        vararg modifiers: WatchEvent.Modifier,
    ): WatchKey = delegate.register(watcher, events, *modifiers)

    override fun compareTo(other: Path): Int = delegate.compareTo(unwrap(other))

    override fun equals(other: Any?): Boolean = other is InstrumentedPath && delegate == other.delegate && fileSystem === other.fileSystem

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    companion object {
        /**
         * The real path behind [path], or [path] itself if it never came from this
         * filesystem.
         *
         * Mixed arguments are normal — `dir.resolve(Path.of("x"))` hands a default-provider
         * path to a wrapped one — so this tolerates both rather than requiring the caller to
         * know which it holds.
         */
        fun unwrap(path: Path): Path = if (path is InstrumentedPath) path.delegate else path
    }
}
