package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

/**
 * `doc:40-durability` §6.3 — the cross-process lock — and the ordering §6.2 and §6.3 imply
 * between it and the in-process one.
 */
class CrossProcessLockIntegrationTest {
    @TempDir
    lateinit var root: Path

    private val locksDirectory: Path get() = root.resolve(DocumentStore.LOCKS_DIRECTORY)

    @Test
    fun `a lock held by another process is refused, and is available again once that process exits`() {
        val target = root.resolve("actor.md")
        val lock = CrossProcessLock(locksDirectory)
        val holder = startLockHolder(target)

        try {
            holder.readyLine shouldBe LockHolderProcess.HELD

            // The criterion, verbatim: hold the lock from a second process. A second THREAD
            // cannot produce this outcome — see LockHolderProcess.
            shouldThrow<StoreContentionException> { lock.exclusive(target) { } }

            // A different document is not affected, so the refusal is per-path and not a
            // blanket "the store is busy". Without this, a lock file naming nothing in
            // particular would pass the assertion above.
            val other = root.resolve("other.md")
            lock.exclusive(other) { "acquired" } shouldBe "acquired"
        } finally {
            holder.release()
        }

        holder.process.waitFor(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true
        holder.process.exitValue() shouldBe 0

        // The healthy case: the same call that was refused now succeeds.
        lock.exclusive(target) { "acquired" } shouldBe "acquired"
    }

    @Test
    fun `the sidecar name is derived from the path, so two documents never share one lock`() {
        val alice = root.resolve("identity").resolve("actors").resolve("alice.md")
        val bob = root.resolve("identity").resolve("actors").resolve("bob.md")

        CrossProcessLock.sidecarNameFor(alice) shouldNotBe CrossProcessLock.sidecarNameFor(bob)
        // …and two spellings of one document do share one, which is the half a hash of the
        // path as GIVEN would get wrong.
        CrossProcessLock.sidecarNameFor(alice) shouldBe
            CrossProcessLock.sidecarNameFor(root.resolve("identity/actors/../actors/alice.md"))
    }

    @Test
    fun `the cross-process lock alone cannot serialise two threads of one process`() {
        // Not a defect being documented — it is why DocumentStore takes the in-process lock
        // FIRST. A FileLock is held by the JVM, so the second thread gets an exception about
        // its own process rather than contention with another one. The next test asserts
        // that DocumentStore never produces this.
        val target = root.resolve("actor.md")
        val lock = CrossProcessLock(locksDirectory)
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)

        val holder =
            Thread {
                lock.exclusive(target) {
                    held.countDown()
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.start()
        held.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        shouldThrow<OverlappingFileLockException> { lock.exclusive(target) { } }

        release.countDown()
        holder.join()
    }

    @Test
    fun `DocumentStore serialises writers within one process, so the file lock never overlaps`() {
        val target = root.resolve("actor.md")
        val store = DocumentStore(root)
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        val start = CyclicBarrier(WRITERS)
        val unexpected = AtomicReference<Throwable?>()
        val writers =
            (1..WRITERS).map { writer ->
                Thread {
                    start.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                    repeat(ROUNDS) { round ->
                        try {
                            val current = store.read(target)!!.version
                            store.write(target, current, "writer $writer round $round".toByteArray())
                        } catch (expected: StaleWriteException) {
                            // Losing the optimistic race is the designed outcome (§6.4) and
                            // is not what this test is about. Caught by type, so anything
                            // else — an OverlappingFileLockException above all — falls
                            // through to the recorder below.
                            check(expected.path == target)
                        }
                    }
                }
            }
        writers.forEach { thread -> thread.setUncaughtExceptionHandler { _, thrown -> unexpected.set(thrown) } }
        writers.forEach { it.start() }
        writers.forEach { it.join() }

        // The failure this asserts the absence of is OverlappingFileLockException: with the
        // two locks taken the other way round, DocumentStore lets two threads of one JVM
        // reach `tryLock` on one sidecar and the JVM refuses its own process.
        unexpected.get() shouldBe null
        // And the store is still coherent: the surviving bytes are one writer's whole record.
        val survived = store.read(target)!!.bytes.decodeToString()
        survived.startsWith("writer ") shouldBe true
    }

    private fun startLockHolder(target: Path): LockHolder {
        val java =
            ProcessHandle
                .current()
                .info()
                .command()
                .orElseThrow { IllegalStateException("this JVM cannot name its own executable") }
        val process =
            ProcessBuilder(
                java,
                "-cp",
                childClasspath(),
                LockHolderProcess::class.java.name,
                locksDirectory.toString(),
                target.toString(),
            ).redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        // Blocks until the child has the lock. No polling and no sleeping: the pipe is the
        // handshake (`rule:archunit/nothingSleepsTheThread` binds the whole repository).
        val ready = process.inputStream.bufferedReader().readLine()
        return LockHolder(process, ready)
    }

    private class LockHolder(
        val process: Process,
        val readyLine: String?,
    ) {
        fun release() {
            process.outputStream.use { out ->
                out.write('\n'.code)
                out.flush()
            }
        }
    }

    private companion object {
        const val AWAIT_SECONDS = 30L
        const val WRITERS = 8
        const val ROUNDS = 25

        /**
         * Built from the code sources of the three things the child needs, not from
         * `java.class.path`.
         *
         * A Gradle test worker is launched with the worker jar alone on `java.class.path`
         * and loads the test classpath through a classloader of its own, so the obvious
         * property would name a classpath the child cannot run from. A `CodeSource`
         * location is where the class actually came from, whichever loader found it.
         */
        fun childClasspath(): String =
            listOf(LockHolderProcess::class.java, CrossProcessLock::class.java, Unit::class.java)
                .map {
                    Path
                        .of(
                            it.protectionDomain.codeSource.location
                                .toURI(),
                        ).toString()
                }.distinct()
                .joinToString(File.pathSeparator)
    }
}
