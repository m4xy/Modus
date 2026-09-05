package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.name
import kotlin.test.Test

/**
 * `doc:40-durability#atomic-write`, observed rather than argued.
 *
 * Integration tests, and there is no unit-testable half: every claim here is about what is
 * on a real disk at a real moment (`doc:35-testing#definitions`).
 *
 * What these tests can and cannot decide is worth stating, because the gap is where an
 * over-claim would live. They observe that each `fsync` happens, on which descriptor, and
 * against what state of the directory — the **sequence**, which is the part Modus controls.
 * That the platform then honours a force is the platform's guarantee; no test on this side
 * of the syscall establishes it, and the `SIGKILL` test `doc:40-durability` §5 asks for is
 * `bean:0150`.
 */
class AtomicWriteIntegrationTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `forces the temp file before the rename and the parent directory after it`() {
        val target = directory.resolve("actor.md")
        AtomicFileWriter().write(target, "first".toByteArray())

        val recorder = Recorder(target)
        AtomicFileWriter(recorder).write(target, "second".toByteArray())

        recorder.steps.map { it.descriptor } shouldContainExactly
            listOf(ForcedDescriptor.TEMP_FILE, ForcedDescriptor.PARENT_DIRECTORY)

        val onTempFile = recorder.steps.first()
        // Step 2: the temp file is in the TARGET'S OWN directory, not a system temp dir.
        onTempFile.forcedPath.parent shouldBe directory
        onTempFile.forcedPath.name.endsWith(AtomicFileWriter.TEMP_SUFFIX) shouldBe true
        // Step 4 happens BEFORE step 6: the target still holds the previous version.
        onTempFile.targetContent shouldBe "first"

        val onDirectory = recorder.steps.last()
        onDirectory.forcedPath shouldBe directory
        // Step 7 happens AFTER step 6: the new version is published and the temp is gone.
        onDirectory.targetContent shouldBe "second"
        onDirectory.tempFiles shouldContainExactly emptyList()
    }

    @Test
    fun `a reader never observes a partial document, and observes both versions`() {
        val target = directory.resolve("large.md")
        // Large enough that a write that was not a rename would be caught mid-flight: a
        // half-megabyte record cannot reach the disk in one indivisible step, and
        // doc:40-durability §4.2 records that no record size on a regular file can.
        val first = "a".repeat(HALF_A_MEGABYTE)
        val second = "b".repeat(HALF_A_MEGABYTE)
        val writer = AtomicFileWriter()
        writer.write(target, first.toByteArray())

        val observed = ConcurrentHashMap.newKeySet<String>()
        val failure = AtomicReference<Throwable?>()
        val stillWriting = AtomicBoolean(true)
        val writing =
            Thread {
                try {
                    repeat(WRITE_ROUNDS) { round ->
                        writer.write(target, (if (round % 2 == 0) second else first).toByteArray())
                    }
                } finally {
                    stillWriting.set(false)
                }
            }
        // Reads for as long as there is a writer, rather than a fixed count: an `fsync` per
        // write makes the writer orders of magnitude slower than the reader, so a bounded
        // reader would finish before the first rename and observe only the version it
        // started with — passing while having watched nothing.
        val reading =
            Thread {
                runCatching {
                    while (stillWriting.get()) {
                        if (Files.isRegularFile(target)) {
                            observed += Files.readAllBytes(target).decodeToString()
                        }
                    }
                }.onFailure { failure.set(it) }
            }

        writing.start()
        reading.start()
        writing.join()
        reading.join()

        failure.get() shouldBe null
        // Every byte sequence a reader saw is one of the two whole versions. A mixture, a
        // truncation or an empty file would appear here as a third member.
        observed shouldBe setOf(first, second)
    }

    @Test
    fun `a write that fails after the temp file is synced leaves the target and an orphan beside it`() {
        val target = directory.resolve("actor.md")
        AtomicFileWriter().write(target, "first".toByteArray())

        val failingAfterTheTempFileIsSynced =
            SyncObserver { descriptor, _ ->
                if (descriptor == ForcedDescriptor.TEMP_FILE) {
                    throw IOException("simulated failure between the temp file's fsync and the rename")
                }
            }

        shouldThrow<IOException> {
            AtomicFileWriter(failingAfterTheTempFileIsSynced).write(target, "second".toByteArray())
        }

        // doc:40-durability §4.1: the previous version is intact, and the evidence of the
        // failed write is an orphan `.tmp` BESIDE the target. Sweeping it is bean:0150;
        // producing it is the property this asserts.
        Files.readString(target) shouldBe "first"
        val orphans = tempFilesIn(directory)
        orphans.size shouldBe 1
        orphans.single().parent shouldBe directory
    }

    @Test
    fun `a failure after the rename leaves the new version published, because the rename already happened`() {
        val target = directory.resolve("actor.md")
        AtomicFileWriter().write(target, "first".toByteArray())

        val failingOnTheDirectoryForce =
            SyncObserver { descriptor, _ ->
                if (descriptor == ForcedDescriptor.PARENT_DIRECTORY) {
                    throw IOException("simulated failure after the rename")
                }
            }

        shouldThrow<IOException> {
            AtomicFileWriter(failingOnTheDirectoryForce).write(target, "second".toByteArray())
        }

        // The other side of the crash window. Past the rename there is no rollback and none
        // is wanted: §4.1's guarantee is "the previous version or the new one", and this is
        // the branch where it is the new one. No orphan, because the temp file became the
        // target.
        Files.readString(target) shouldBe "second"
        tempFilesIn(directory) shouldContainExactly emptyList()
    }

    @Test
    fun `the healthy write leaves no orphan at all`() {
        val target = directory.resolve("actor.md")

        AtomicFileWriter().write(target, "first".toByteArray())
        AtomicFileWriter().write(target, "second".toByteArray())

        Files.readString(target) shouldBe "second"
        tempFilesIn(directory) shouldContainExactly emptyList()
    }

    @Test
    fun `a target with no parent directory is refused rather than written somewhere else`() {
        shouldThrow<IllegalArgumentException> {
            AtomicFileWriter().write(Path.of("actor.md"), "content".toByteArray())
        }
    }

    @Test
    fun `an in-flight write is not published until the rename, so a reader mid-write sees the old bytes`() {
        val target = directory.resolve("actor.md")
        AtomicFileWriter().write(target, "first".toByteArray())

        val insideTheWrite = CountDownLatch(1)
        val readerHasLooked = CountDownLatch(1)
        val seenMidWrite = AtomicReference<String?>()
        val pausingAfterTheTempFileIsSynced =
            SyncObserver { descriptor, _ ->
                if (descriptor == ForcedDescriptor.TEMP_FILE) {
                    insideTheWrite.countDown()
                    readerHasLooked.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }

        val writing = Thread { AtomicFileWriter(pausingAfterTheTempFileIsSynced).write(target, "second".toByteArray()) }
        writing.start()
        insideTheWrite.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        // The new bytes are on disk and synced. They are in the temp file, and the target
        // still reads as the previous version — which is the whole content of "no reader
        // ever observes a partially written document".
        seenMidWrite.set(Files.readString(target))
        tempFilesIn(directory).size shouldBe 1

        readerHasLooked.countDown()
        writing.join()

        seenMidWrite.get() shouldBe "first"
        Files.readString(target) shouldBe "second"
        tempFilesIn(directory) shouldContainExactly emptyList()
    }

    @Test
    fun `the recorder used by these tests would notice a missing force`() {
        // A negative control on the instrument rather than on the mechanism: a recorder that
        // reported the same thing whatever happened would make every assertion above
        // vacuous. An observer that is never called records nothing, and `shouldNotBe`
        // states that the populated case is distinguishable from it.
        val target = directory.resolve("actor.md")
        val recorder = Recorder(target)

        recorder.steps shouldContainExactly emptyList()
        AtomicFileWriter(recorder).write(target, "first".toByteArray())
        recorder.steps shouldNotBe emptyList<Step>()
    }

    /** What was true on disk at the moment one `fsync` returned. */
    private data class Step(
        val descriptor: ForcedDescriptor,
        val forcedPath: Path,
        val targetContent: String?,
        val tempFiles: List<Path>,
    )

    /**
     * Reads the directory at each force. It observes; it cannot disable a force, because
     * [SyncObserver] is called after the real one.
     */
    private class Recorder(
        private val target: Path,
    ) : SyncObserver {
        val steps: MutableList<Step> = mutableListOf()

        override fun forced(
            descriptor: ForcedDescriptor,
            path: Path,
        ) {
            steps +=
                Step(
                    descriptor = descriptor,
                    forcedPath = path,
                    targetContent = if (Files.isRegularFile(target)) Files.readString(target) else null,
                    tempFiles = tempFilesIn(target.parent),
                )
        }
    }

    private companion object {
        const val HALF_A_MEGABYTE = 512 * 1024
        const val WRITE_ROUNDS = 40
        const val AWAIT_SECONDS = 10L

        fun tempFilesIn(directory: Path): List<Path> =
            Files.list(directory).use { entries ->
                entries.filter { it.name.endsWith(AtomicFileWriter.TEMP_SUFFIX) }.toList()
            }
    }
}
