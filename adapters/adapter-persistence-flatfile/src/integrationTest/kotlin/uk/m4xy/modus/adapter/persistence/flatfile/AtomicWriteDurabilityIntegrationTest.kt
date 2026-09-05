package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import uk.m4xy.modus.adapter.persistence.flatfile.testfs.InstrumentedFileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.Test

/**
 * The two `fsync` calls of `doc:40-durability#atomic-write`, observed being **issued**.
 *
 * `bean:0147` shipped `AtomicFileWriter` with an admitted gap: deleting `channel.force(true)`
 * on the temp file, or on the parent directory, left all 31 of its tests green, and JaCoCo
 * reported 100% instruction and branch coverage on the class regardless — line coverage
 * counts execution, not consequence. `SyncObserver` could not close it, because it is
 * notified *beside* the force rather than by it, and promoting it to a strategy would have
 * moved the mechanism into a seam that production could be wired to skip.
 *
 * These tests close it without touching production. `AtomicFileWriter` is unmodified, takes
 * no new parameter, and calls `force(true)` unconditionally; the path it is handed belongs
 * to an instrumented filesystem, and `FileChannel.open(Path, Set, FileAttribute...)`
 * delegates to `path.getFileSystem().provider().newFileChannel(...)` by specification
 * (`java.base/java/nio/channels/FileChannel.java:300-301`, JDK 25). Production always
 * resolves the default filesystem and can never reach this (`bean:0174`).
 */
class AtomicWriteDurabilityIntegrationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `both forces are issued, on the temp file and then on the parent directory`() {
        val watched = InstrumentedFileSystems.watching()
        val target = watched.pathTo(directory.resolve("actor.md"))

        AtomicFileWriter().write(target, "first".toByteArray())

        val forced = watched.calls.forcedPaths
        forced.size shouldBe 2
        // Step 4: the temp file, in the target's own directory.
        forced[0].name.endsWith(AtomicFileWriter.TEMP_SUFFIX) shouldBe true
        forced[0].parent shouldBe directory
        // Step 7: the parent directory itself. This is the one doc:40-durability §4 says
        // people skip, and until now nothing in this repository could tell whether it
        // happened.
        forced[1] shouldBe directory

        Files.readString(directory.resolve("actor.md")) shouldBe "first"
    }

    @Test
    fun `the writer opens exactly the two channels it forces`() {
        val watched = InstrumentedFileSystems.watching()

        AtomicFileWriter().write(watched.pathTo(directory.resolve("actor.md")), "first".toByteArray())

        // The vacuity assertion, and it is load-bearing rather than decorative: a seam that
        // was never on the path and a writer that issued no force both report an empty
        // forcedPaths. `bean:0051` records that distinction being the entire difference
        // between an inert check and a passing one.
        watched.calls.interceptedChannelOpens shouldBe 2
        watched.calls.forcedPaths.size shouldBe 2
    }

    @Test
    fun `a second write forces twice again, so the mechanism is per write and not once per process`() {
        val watched = InstrumentedFileSystems.watching()
        val target = watched.pathTo(directory.resolve("actor.md"))

        AtomicFileWriter().write(target, "first".toByteArray())
        AtomicFileWriter().write(target, "second".toByteArray())

        watched.calls.forcedPaths.size shouldBe 4
        watched.calls.forcedPaths.map { it == directory } shouldContainExactly
            listOf(false, true, false, true)
        Files.readString(directory.resolve("actor.md")) shouldBe "second"
    }

    @Test
    fun `a write that fails before the rename forces the temp file and never the directory`() {
        val watched = InstrumentedFileSystems.watching()
        val target = watched.pathTo(directory.resolve("actor.md"))
        AtomicFileWriter().write(target, "first".toByteArray())
        val before = watched.calls.forcedPaths.size

        val failing =
            SyncObserver { descriptor, _ ->
                if (descriptor == ForcedDescriptor.TEMP_FILE) {
                    throw java.io.IOException("simulated failure between the temp file's fsync and the rename")
                }
            }
        runCatching { AtomicFileWriter(failing).write(target, "second".toByteArray()) }
            .isFailure shouldBe true

        // Exactly one further force, and it is not the directory: the crash window
        // doc:40-durability §4.1 describes, now visible as a real syscall count rather than
        // as an observer notification standing next to one.
        val added = watched.calls.forcedPaths.drop(before)
        added.size shouldBe 1
        added.single().name.endsWith(AtomicFileWriter.TEMP_SUFFIX) shouldBe true
        Files.readString(directory.resolve("actor.md")) shouldBe "first"
    }
}
