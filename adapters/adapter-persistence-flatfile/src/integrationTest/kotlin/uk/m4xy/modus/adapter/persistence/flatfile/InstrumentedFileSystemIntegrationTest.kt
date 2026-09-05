package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import uk.m4xy.modus.adapter.persistence.flatfile.testfs.FilesystemCall
import uk.m4xy.modus.adapter.persistence.flatfile.testfs.InstrumentedFileSystems
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.name
import kotlin.test.Test

/**
 * The seam itself, tested before anything is tested through it.
 *
 * A delegating filesystem is ~350 lines of mechanical forwarding, and the defect it invites
 * is a method that forwards **incorrectly** rather than one that fails to compile: an
 * override returning a delegate path where it should return a wrapped one turns every later
 * test into one that quietly bypasses the seam. Reading the forwarding by eye is exactly how
 * that is missed, so it is asserted instead (`bean:0174`).
 */
class InstrumentedFileSystemIntegrationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `a wrapped path reaches the real disk, so a test using it exercises the real bytes`() {
        val watched = InstrumentedFileSystems.watching()
        val target = watched.pathTo(directory.resolve("actor.md"))

        Files.writeString(target, "written through the seam")

        // Read back through the DEFAULT filesystem, on a path this test built for itself.
        // If the seam had diverted the write anywhere, this is where it would show.
        Files.readString(directory.resolve("actor.md")) shouldBe "written through the seam"
        Files.readString(target) shouldBe "written through the seam"
        target.toString() shouldBe directory.resolve("actor.md").toString()
    }

    @Test
    fun `every path operation returns a path still bound to the seam`() {
        val watched = InstrumentedFileSystems.watching()
        val root = watched.pathTo(directory)
        val provider = root.fileSystem.provider()

        // The leak this asserts against compiles, passes, and silently stops exercising the
        // seam from the point of the leak onwards.
        root.resolve("a.md").fileSystem.provider() shouldBe provider
        root
            .resolve("a.md")
            .parent!!
            .fileSystem
            .provider() shouldBe provider
        root
            .resolve("x")
            .resolve("..")
            .resolve("a.md")
            .normalize()
            .fileSystem
            .provider() shouldBe provider
        root.toAbsolutePath().fileSystem.provider() shouldBe provider
        root
            .resolve("a.md")
            .fileName!!
            .fileSystem
            .provider() shouldBe provider
        root.root!!.fileSystem.provider() shouldBe provider

        Files.createFile(root.resolve("listed.md"))
        Files.list(root).use { entries ->
            entries.toList().map { it.fileSystem.provider() } shouldContainExactly listOf(provider)
        }
    }

    @Test
    fun `a channel opened on a wrapped path is intercepted, and one on a plain path is not`() {
        val watched = InstrumentedFileSystems.watching()

        FileChannel
            .open(
                watched.pathTo(directory.resolve("watched.md")),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            ).use { it.force(true) }

        watched.calls.interceptedChannelOpens shouldBe 1
        watched.calls.forcedPaths.map { it.name } shouldContainExactly listOf("watched.md")

        // The negative control, and the reason the interception count exists: an ordinary
        // path must NOT be recorded. A seam that recorded everything would make every
        // assertion about what production did meaningless.
        FileChannel
            .open(
                directory.resolve("unwatched.md"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            ).use { it.force(true) }

        watched.calls.interceptedChannelOpens shouldBe 1
        watched.calls.forcedPaths.map { it.name } shouldContainExactly listOf("watched.md")
    }

    @Test
    fun `a force that throws is not recorded as having happened`() {
        val watched = InstrumentedFileSystems.watching()
        val channel =
            FileChannel.open(
                watched.pathTo(directory.resolve("closed.md")),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
        channel.close()

        runCatching { channel.force(true) }.isFailure shouldBe true

        // The recorder is placed after the delegate returns, so it records forces that
        // happened rather than forces that were attempted.
        watched.calls.forcedPaths shouldContainExactly emptyList()
        watched.calls.interceptedChannelOpens shouldBe 1
    }

    @Test
    fun `a short-writing channel consumes only what it was allowed, and the caller must loop`() {
        val watched = InstrumentedFileSystems.writingAtMost(SHORT_WRITE_BYTES)
        val target = watched.pathTo(directory.resolve("record.ndjson"))
        val record = "x".repeat(RECORD_BYTES).toByteArray()

        // A single unchecked write — the shape doc:40-durability §4.2 says is wrong, because
        // FileChannel.write may perform a short write and no API asserts a single syscall.
        FileChannel.open(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.write(java.nio.ByteBuffer.wrap(record))
        }
        Files.size(directory.resolve("record.ndjson")) shouldBe SHORT_WRITE_BYTES.toLong()

        // The same channel, written by a caller that loops. This is the mechanism §4.2 step
        // 3 mandates on the append path, and bean:0175 is where it becomes production code.
        val looped = watched.pathTo(directory.resolve("looped.ndjson"))
        FileChannel.open(looped, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val buffer = java.nio.ByteBuffer.wrap(record)
            while (buffer.hasRemaining()) {
                channel.write(buffer)
            }
        }
        Files.size(directory.resolve("looped.ndjson")) shouldBe RECORD_BYTES.toLong()
        Files.readAllBytes(directory.resolve("looped.ndjson")).decodeToString() shouldBe record.decodeToString()
    }

    @Test
    fun `the seam records opens and forces as separate kinds, in order`() {
        val watched = InstrumentedFileSystems.watching()

        FileChannel
            .open(
                watched.pathTo(directory.resolve("ordered.md")),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            ).use { it.force(true) }

        watched.calls.calls.map { it.kind } shouldContainExactly
            listOf(FilesystemCall.Kind.OPEN_FILE_CHANNEL, FilesystemCall.Kind.FORCE)
    }

    private companion object {
        const val SHORT_WRITE_BYTES = 8
        const val RECORD_BYTES = 4096
    }
}
