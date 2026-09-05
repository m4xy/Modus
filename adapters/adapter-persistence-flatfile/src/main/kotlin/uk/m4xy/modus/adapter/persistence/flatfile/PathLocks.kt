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
 */
public class PathLocks(
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {
    private val stripes = ConcurrentHashMap<Path, ReentrantReadWriteLock>()

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

    private fun stripeFor(path: Path): ReentrantReadWriteLock = stripes.computeIfAbsent(canonical(path)) { ReentrantReadWriteLock() }

    public companion object {
        /** §6.2: "Lock acquisition has a timeout (default 10 s)." */
        public val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)

        /**
         * The key two references to one document must agree on. Absolute and normalised,
         * so `a/b.md` and `a/./x/../b.md` are one stripe and not two.
         */
        public fun canonical(path: Path): Path = path.toAbsolutePath().normalize()
    }
}
