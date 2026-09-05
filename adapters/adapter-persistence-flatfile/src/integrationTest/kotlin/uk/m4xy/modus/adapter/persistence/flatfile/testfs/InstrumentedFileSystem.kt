package uk.m4xy.modus.adapter.persistence.flatfile.testfs

import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.WatchService
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider

/**
 * The default filesystem, answering with [InstrumentedFileSystemProvider] and handing out
 * [InstrumentedPath]s.
 *
 * [close] is deliberately a no-op: this wraps the default filesystem, which is not closeable,
 * and a test that closed it would take the JVM's filesystem down with it.
 */
class InstrumentedFileSystem internal constructor(
    private val delegate: FileSystem,
    private val instrumentedProvider: InstrumentedFileSystemProvider,
) : FileSystem() {
    internal fun wrap(path: Path): InstrumentedPath = InstrumentedPath(path, this)

    override fun provider(): FileSystemProvider = instrumentedProvider

    override fun close() {
        // The default filesystem cannot be closed, and this is a view of it.
    }

    override fun isOpen(): Boolean = delegate.isOpen

    override fun isReadOnly(): Boolean = delegate.isReadOnly

    override fun getSeparator(): String = delegate.separator

    override fun getRootDirectories(): Iterable<Path> = delegate.rootDirectories.map { wrap(it) }

    override fun getFileStores(): Iterable<FileStore> = delegate.fileStores

    override fun supportedFileAttributeViews(): Set<String> = delegate.supportedFileAttributeViews()

    override fun getPath(
        first: String,
        vararg more: String,
    ): Path = wrap(delegate.getPath(first, *more))

    override fun getPathMatcher(syntaxAndPattern: String): PathMatcher {
        val matcher = delegate.getPathMatcher(syntaxAndPattern)
        return PathMatcher { path -> matcher.matches(InstrumentedPath.unwrap(path)) }
    }

    override fun getUserPrincipalLookupService(): UserPrincipalLookupService = delegate.userPrincipalLookupService

    override fun newWatchService(): WatchService = delegate.newWatchService()
}
