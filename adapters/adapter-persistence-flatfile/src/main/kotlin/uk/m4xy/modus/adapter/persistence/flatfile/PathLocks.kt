package uk.m4xy.modus.adapter.persistence.flatfile

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * The in-process half of `doc:40-durability` §6.2: a striped [ReentrantReadWriteLock] keyed
 * by canonical path.
 *
 * Two rules from §6.2 are structural here rather than left to discipline:
 *
 * - **Acquisition always has a timeout.** Every entry point goes through [acquire], which
 *   uses `tryLock(timeout)`; there is no method that blocks indefinitely, so no caller can
 *   choose to. A timeout is [StoreContentionException], which the REST adapter maps to
 *   `409 Conflict`.
 * - **Two locks are only ever taken through [exclusiveAll]**, which sorts by canonical path
 *   first. Deadlock avoidance by total ordering needs every acquirer to agree on the order,
 *   and the only way to guarantee that is for one function to impose it. [exclusive] takes
 *   exactly one lock, so a caller that wants two has nowhere else to go.
 *
 * The key is the absolute, normalised path rather than `toRealPath()`: a document that does
 * not exist yet has no real path, and the write that creates it must be serialised against
 * the read that will follow it.
 *
 * **The stripes are JVM-wide, and that is a correctness requirement rather than a cache.**
 * This lock exists to reduce the process to one contender before [CrossProcessLock] asks the
 * operating system for a `FileLock`, and a `FileLock` is held by the **JVM**. Two locks can
 * only be ordered if they have the same scope: with a stripe map per instance, two
 * [DocumentStore]s over one root are two disjoint sets of stripes, both writers reach
 * `tryLock` on one sidecar, and the JVM refuses its own process with
 * [java.nio.channels.OverlappingFileLockException] — the failure this class is ordered ahead
 * of `FileLock` to prevent. That was the shipped behaviour until it was found in review, and
 * it was reachable by accident: constructing a second `DocumentStore(root)` was enough.
 *
 * A mandatory constructor argument was the alternative. It is rejected because it moves the
 * hazard into the wiring rather than removing it — every future call site would have to know
 * to share one instance, and `doc:00-constitution` §9 says a rule anyone has to remember is
 * eventually broken. The timeout stays per-instance: it is a policy, not a scope.
 */
public class PathLocks(
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {
    /** Runs [action] holding the write lock for [path], and nothing else. */
    public fun <T> exclusive(
        path: Path,
        action: () -> T,
    ): T = acquire(stripeFor(path).writeLock(), path, action)

    /**
     * Runs [action] holding the read lock for [path].
     *
     * Reading a single document needs no lock at all — the rename gives a reader atomicity
     * for free (§6.1). This is for the consistent multi-file view §6.1's second sentence
     * describes, where the reader must exclude a writer for the span of several reads.
     */
    public fun <T> shared(
        path: Path,
        action: () -> T,
    ): T = acquire(stripeFor(path).readLock(), path, action)

    /**
     * Runs [action] holding the write lock for every path in [paths], acquired in canonical
     * path order and released in the reverse.
     *
     * The ordering is the whole point and is applied to the caller's collection rather than
     * demanded of it: two callers naming the same pair in opposite orders is the deadlock,
     * and a rule that says "pass them sorted" is a rule someone eventually does not.
     */
    public fun <T> exclusiveAll(
        paths: Collection<Path>,
        action: () -> T,
    ): T {
        val ordered = paths.map { canonical(it) }.distinct().sorted()
        return acquireAll(ordered, 0, action)
    }

    private fun <T> acquireAll(
        ordered: List<Path>,
        index: Int,
        action: () -> T,
    ): T =
        if (index == ordered.size) {
            action()
        } else {
            val path = ordered[index]
            acquire(stripeFor(path).writeLock(), path) { acquireAll(ordered, index + 1, action) }
        }

    private fun <T> acquire(
        lock: Lock,
        path: Path,
        action: () -> T,
    ): T {
        if (!lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw StoreContentionException("timed out after $timeout waiting for the lock on $path")
        }
        try {
            return action()
        } finally {
            lock.unlock()
        }
    }

    private fun stripeFor(path: Path): ReentrantReadWriteLock = STRIPES.computeIfAbsent(canonical(path)) { ReentrantReadWriteLock() }

    public companion object {
        /**
         * One stripe per canonical path, for the whole JVM. See the class KDoc: the scope
         * is what makes this lock orderable against a `FileLock`, which is JVM-held.
         *
         * It never evicts, so it grows with the number of distinct paths a process has ever
         * touched. That is bounded by the store's document count in a long-lived server and
         * is `bean:0152`, which is a memory question and not a correctness one — a stripe
         * that is dropped while nobody holds it and recreated on the next acquisition is
         * still the same lock to every acquirer, so eviction is safe and simply not yet
         * needed.
         */
        private val STRIPES = ConcurrentHashMap<Path, ReentrantReadWriteLock>()

        /** §6.2: "Lock acquisition has a timeout (default 10 s)." */
        public val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)

        /**
         * The key two references to one document must agree on. Absolute and normalised,
         * so `a/b.md` and `a/./x/../b.md` are one stripe and not two.
         */
        public fun canonical(path: Path): Path = path.toAbsolutePath().normalize()
    }
}
