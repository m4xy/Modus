package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.net.URI
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.FileChannel
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.ExecutorService

/**
 * The default filesystem provider, with every path unwrapped on the way in, every path
 * wrapped on the way out, and [newFileChannel] returning an [InstrumentedFileChannel].
 *
 * **Why this shape and not a strategy object on the production class.** The property that
 * makes a seam admissible under `doc:00-constitution#observed-failing` is not "could a
 * double be written that skips the mechanism" — true of every double — but **whether
 * production can be configured into a state where the mechanism does not run**. Here it
 * cannot: `AtomicFileWriter` is unchanged, takes no new parameter, calls `force(true)`
 * unconditionally, and production always resolves the default filesystem. Only a test that
 * deliberately builds a path from [InstrumentedFileSystems] ever reaches this class
 * (`bean:0174`).
 *
 * The interception point is a specified one: `FileChannel.open(Path, Set, FileAttribute...)`
 * delegates to `path.getFileSystem().provider().newFileChannel(...)`
 * (`java.base/java/nio/channels/FileChannel.java:300-301`, JDK 25).
 */
class InstrumentedFileSystemProvider internal constructor(
    private val delegate: FileSystemProvider,
    private val calls: FilesystemCalls,
    private val shortWriteLimit: Int?,
) : FileSystemProvider() {
    private val instrumentedFileSystem: InstrumentedFileSystem =
        InstrumentedFileSystem(FileSystems.getDefault(), this)

    /** The filesystem whose paths route through this provider. */
    internal fun fileSystem(): InstrumentedFileSystem = instrumentedFileSystem

    override fun getScheme(): String = delegate.scheme

    // --- interception ------------------------------------------------------

    override fun newFileChannel(
        path: Path,
        options: Set<OpenOption>,
        vararg attrs: FileAttribute<*>,
    ): FileChannel {
        val real = InstrumentedPath.unwrap(path)
        calls.record(FilesystemCall.Kind.OPEN_FILE_CHANNEL, real)
        return InstrumentedFileChannel(
            delegate = delegate.newFileChannel(real, options, *attrs),
            path = real,
            calls = calls,
            shortWriteLimit = shortWriteLimit,
        )
    }

    // --- plain delegation, unwrapping in and wrapping out -------------------

    override fun newFileSystem(
        uri: URI,
        env: Map<String, *>,
    ): FileSystem = instrumentedFileSystem

    override fun getFileSystem(uri: URI): FileSystem = instrumentedFileSystem

    override fun getPath(uri: URI): Path = instrumentedFileSystem.wrap(delegate.getPath(uri))

    override fun newByteChannel(
        path: Path,
        options: Set<OpenOption>,
        vararg attrs: FileAttribute<*>,
    ): SeekableByteChannel = delegate.newByteChannel(InstrumentedPath.unwrap(path), options, *attrs)

    override fun newAsynchronousFileChannel(
        path: Path,
        options: Set<OpenOption>,
        executor: ExecutorService?,
        vararg attrs: FileAttribute<*>,
    ): AsynchronousFileChannel = delegate.newAsynchronousFileChannel(InstrumentedPath.unwrap(path), options, executor, *attrs)

    override fun newDirectoryStream(
        dir: Path,
        filter: DirectoryStream.Filter<in Path>,
    ): DirectoryStream<Path> {
        // The filter is applied to WRAPPED paths, because that is what the caller sees and
        // what its predicate was written against.
        val stream = delegate.newDirectoryStream(InstrumentedPath.unwrap(dir)) { true }
        return object : DirectoryStream<Path> {
            override fun iterator(): MutableIterator<Path> =
                stream
                    .asSequence()
                    .map { instrumentedFileSystem.wrap(it) as Path }
                    .filter { filter.accept(it) }
                    .toMutableList()
                    .iterator()

            override fun close() = stream.close()
        }
    }

    override fun createDirectory(
        dir: Path,
        vararg attrs: FileAttribute<*>,
    ) {
        delegate.createDirectory(InstrumentedPath.unwrap(dir), *attrs)
    }

    override fun delete(path: Path) {
        delegate.delete(InstrumentedPath.unwrap(path))
    }

    override fun deleteIfExists(path: Path): Boolean = delegate.deleteIfExists(InstrumentedPath.unwrap(path))

    override fun createLink(
        link: Path,
        existing: Path,
    ) {
        delegate.createLink(InstrumentedPath.unwrap(link), InstrumentedPath.unwrap(existing))
    }

    override fun createSymbolicLink(
        link: Path,
        target: Path,
        vararg attrs: FileAttribute<*>,
    ) {
        delegate.createSymbolicLink(InstrumentedPath.unwrap(link), InstrumentedPath.unwrap(target), *attrs)
    }

    override fun readSymbolicLink(link: Path): Path = instrumentedFileSystem.wrap(delegate.readSymbolicLink(InstrumentedPath.unwrap(link)))

    override fun copy(
        source: Path,
        target: Path,
        vararg options: CopyOption,
    ) {
        delegate.copy(InstrumentedPath.unwrap(source), InstrumentedPath.unwrap(target), *options)
    }

    override fun move(
        source: Path,
        target: Path,
        vararg options: CopyOption,
    ) {
        delegate.move(InstrumentedPath.unwrap(source), InstrumentedPath.unwrap(target), *options)
    }

    override fun isSameFile(
        path: Path,
        path2: Path,
    ): Boolean = delegate.isSameFile(InstrumentedPath.unwrap(path), InstrumentedPath.unwrap(path2))

    override fun isHidden(path: Path): Boolean = delegate.isHidden(InstrumentedPath.unwrap(path))

    override fun getFileStore(path: Path): FileStore = delegate.getFileStore(InstrumentedPath.unwrap(path))

    override fun checkAccess(
        path: Path,
        vararg modes: AccessMode,
    ) {
        delegate.checkAccess(InstrumentedPath.unwrap(path), *modes)
    }

    override fun <V : FileAttributeView> getFileAttributeView(
        path: Path,
        type: Class<V>,
        vararg options: LinkOption,
    ): V? = delegate.getFileAttributeView(InstrumentedPath.unwrap(path), type, *options)

    override fun <A : BasicFileAttributes> readAttributes(
        path: Path,
        type: Class<A>,
        vararg options: LinkOption,
    ): A = delegate.readAttributes(InstrumentedPath.unwrap(path), type, *options)

    override fun readAttributes(
        path: Path,
        attributes: String,
        vararg options: LinkOption,
    ): MutableMap<String, Any> = delegate.readAttributes(InstrumentedPath.unwrap(path), attributes, *options)

    override fun setAttribute(
        path: Path,
        attribute: String,
        value: Any?,
        vararg options: LinkOption,
    ) {
        delegate.setAttribute(InstrumentedPath.unwrap(path), attribute, value, *options)
    }
}
