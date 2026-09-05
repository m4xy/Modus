package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test

/**
 * `doc:40-durability` §6.2 — the in-process half of the locking model.
 *
 * An integration test rather than a unit test even though nothing here touches a disk:
 * the lock is keyed by [Path], and `rule:archunit/unitTestsDoNotTouchTheFilesystemOrTheNetwork`
 * bans `java.nio.file..` from a unit test outright. The taxonomy is decided by what the
 * test needs to name, not by what it happens to do with it (`doc:35-testing#definitions`).
 */
class PathLockingIntegrationTest {
    @TempDir
    lateinit var root: Path

    @Test
    fun `two writers to one path are serialised, and the second one times out rather than blocking`() {
        val locks = PathLocks(SHORT_TIMEOUT)
        val target = root.resolve("actor.md")
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)

        val holder =
            Thread {
                locks.exclusive(target) {
                    held.countDown()
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.start()
        held.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        shouldThrow<StoreContentionException> { locks.exclusive(target) { } }

        release.countDown()
        holder.join()

        // The healthy case, on the same lock: once the holder is gone the same call that
        // just timed out succeeds. Without this half, a lock that never granted anything
        // would pass the assertion above.
        locks.exclusive(target) { "written" } shouldBe "written"
    }

    @Test
    fun `the default timeout is the ten seconds the durability model states`() {
        // A figure that must match an authority rather than a preference: `doc:40-durability`
        // §6.2 writes "(default 10 s)", and a constant that drifts from the document it
        // implements is the shape `bean:0090` records. Asserted here so the drift is a red
        // test rather than a reader's discovery.
        PathLocks.DEFAULT_TIMEOUT shouldBe Duration.ofSeconds(10)

        // …and the no-argument constructor uses it, so the constant is not merely declared.
        PathLocks().exclusive(root.resolve("actor.md")) { "acquired" } shouldBe "acquired"
    }

    @Test
    fun `two writers to different paths do not contend`() {
        val locks = PathLocks(SHORT_TIMEOUT)
        val first = root.resolve("alice.md")
        val second = root.resolve("bob.md")
        val bothHeld = CountDownLatch(2)
        val release = CountDownLatch(1)

        val threads =
            listOf(first, second).map { path ->
                Thread {
                    locks.exclusive(path) {
                        bothHeld.countDown()
                        release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                    }
                }
            }
        threads.forEach { it.start() }

        // Both locks are held at the same moment. A single global lock would never let the
        // count reach zero, which is what makes this an assertion about STRIPING and not
        // merely about two calls returning.
        bothHeld.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        release.countDown()
        threads.forEach { it.join() }
    }

    @Test
    fun `two spellings of one path are one lock`() {
        val locks = PathLocks(SHORT_TIMEOUT)
        val direct = root.resolve("actor.md")
        val roundabout = root.resolve("identity").resolve("..").resolve("actor.md")
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)

        val holder =
            Thread {
                locks.exclusive(direct) {
                    held.countDown()
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.start()
        held.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        // Two references to one document must be one stripe. Keying on the path as given
        // would make these two locks and let both writers in.
        shouldThrow<StoreContentionException> { locks.exclusive(roundabout) { } }

        release.countDown()
        holder.join()
    }

    @Test
    fun `a shared lock excludes a writer, which is what a consistent multi-file read needs`() {
        val locks = PathLocks(SHORT_TIMEOUT)
        val target = root.resolve("actor.md")
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)

        val reader =
            Thread {
                locks.shared(target) {
                    held.countDown()
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        reader.start()
        held.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        shouldThrow<StoreContentionException> { locks.exclusive(target) { } }
        // …and another reader is admitted, so it is a read-write lock and not an
        // exclusive one wearing the name.
        locks.shared(target) { "read" } shouldBe "read"

        release.countDown()
        reader.join()
    }

    @Test
    fun `the multi-lock helper acquires in canonical order, whatever order it was given`() {
        // The probe timeout must be far SHORTER than how long the holder keeps `alice`, and
        // this is load-bearing rather than tuning. With the two equal, the first version of
        // this test passed against the unordered mutation by 250 ms: the holder's own await
        // expired, it released `alice`, the mutated helper finished, and the probe acquired
        // `bob` just inside its window. The mutation survived a test written to kill it.
        val locks = PathLocks(PROBE_TIMEOUT)
        // "alice.md" sorts before "bob.md", so canonical order is alice-then-bob.
        val alice = root.resolve("alice.md")
        val bob = root.resolve("bob.md")

        val aliceHeld = CountDownLatch(1)
        val releaseAlice = CountDownLatch(1)
        val holder =
            Thread {
                locks.exclusive(alice) {
                    aliceHeld.countDown()
                    releaseAlice.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.start()
        aliceHeld.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        val enteredBoth = CountDownLatch(1)
        val pairFailure = AtomicReference<Throwable?>()
        val pair =
            Thread {
                runCatching {
                    // Named in the REVERSE of canonical order on purpose.
                    locks.exclusiveAll(listOf(bob, alice)) { enteredBoth.countDown() }
                }.onFailure { pairFailure.set(it) }
            }
        pair.start()

        // It cannot be inside the action: `alice` is held by another thread.
        enteredBoth.await(SETTLE_MILLIS, TimeUnit.MILLISECONDS) shouldBe false
        // And it is blocked on `alice` — the canonically FIRST path — rather than sitting
        // on `bob`, which it was asked for first. This is the assertion: with the ordering
        // removed, the helper holds `bob` here and this call times out instead.
        locks.exclusive(bob) { "free" } shouldBe "free"

        releaseAlice.countDown()
        holder.join()
        enteredBoth.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true
        pair.join()
        pairFailure.get() shouldBe null
    }

    @Test
    fun `two callers naming the same pair in opposite orders both complete`() {
        val locks = PathLocks(MEDIUM_TIMEOUT)
        val alice = root.resolve("alice.md")
        val bob = root.resolve("bob.md")
        val bothDone = CountDownLatch(2)
        val failures = AtomicReference<Throwable?>()

        val threads =
            listOf(listOf(alice, bob), listOf(bob, alice)).map { pair ->
                Thread {
                    runCatching {
                        repeat(ROUNDS) { locks.exclusiveAll(pair) { } }
                        bothDone.countDown()
                    }.onFailure { failures.set(it) }
                }
            }
        threads.forEach { it.start() }

        // The interleaving that deadlocks a pair of unordered acquirers. With a total
        // ordering it cannot be constructed; without one, both callers hit SHORT_TIMEOUT
        // and this fails as a StoreContentionException rather than as a hung build — which
        // is the reason §6.2 requires the timeout as well as the ordering.
        bothDone.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true
        threads.forEach { it.join() }
        failures.get() shouldBe null
    }

    @Test
    fun `the multi-lock helper holds every path it was given, not merely the first`() {
        val locks = PathLocks(SHORT_TIMEOUT)
        val alice = root.resolve("alice.md")
        val bob = root.resolve("bob.md")
        val insideBoth = CountDownLatch(1)
        val release = CountDownLatch(1)

        val holder =
            Thread {
                locks.exclusiveAll(listOf(alice, bob)) {
                    insideBoth.countDown()
                    release.await(AWAIT_SECONDS, TimeUnit.SECONDS)
                }
            }
        holder.start()
        insideBoth.await(AWAIT_SECONDS, TimeUnit.SECONDS) shouldBe true

        shouldThrow<StoreContentionException> { locks.exclusive(alice) { } }
        shouldThrow<StoreContentionException> { locks.exclusive(bob) { } }

        release.countDown()
        holder.join()

        locks.exclusiveAll(listOf(alice, bob)) { "both" } shouldBe "both"
    }

    private companion object {
        val SHORT_TIMEOUT: Duration = Duration.ofMillis(250)
        val MEDIUM_TIMEOUT: Duration = Duration.ofSeconds(2)

        /**
         * The lock timeout of the ordered-acquisition test, deliberately far shorter than
         * [AWAIT_SECONDS]: the probe has to give up while the other path is still held.
         */
        val PROBE_TIMEOUT: Duration = Duration.ofSeconds(5)

        /** How long a thread holds a lock for another thread to observe. */
        const val AWAIT_SECONDS = 60L
        const val SETTLE_MILLIS = 250L
        const val ROUNDS = 200
    }
}
