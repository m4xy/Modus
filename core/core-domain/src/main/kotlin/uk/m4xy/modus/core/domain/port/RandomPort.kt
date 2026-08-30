package uk.m4xy.modus.core.domain.port

/**
 * Bounded randomness that is not an identifier, as a dependency rather than as ambient
 * state.
 *
 * `doc:20-ddd-practices` §5.3 requires this port beside [ClockPort] and [IdGeneratorPort],
 * and §8's table names `Math.random()` and `kotlin.random.Random.Default` as what it
 * replaces. It is the port for the capability with **no rule at all**: `NoAmbientRandom` is
 * documented in `doc:15-repository-layout` §4.2 and does not exist, and both
 * `Math.random()` and `Random.Default` were observed merging green when planted in
 * `core-domain` (`bean:0065`).
 *
 * It ships with no caller, deliberately, and that is the one thing about it worth arguing.
 * The case against is that an abstraction with no caller gets frozen in place before anyone
 * knows its shape. The case for, which won: §5.3 requires all three, and shipping two would
 * leave the document asserting a port that never arrives with nothing recording the gap —
 * which is the failure mode `doc:00-constitution#observed-failing` exists to stop. The
 * surface is kept to a single method so that being wrong about it is cheap.
 *
 * Identifiers do **not** come from here. They come from [IdGeneratorPort], which promises
 * distinctness rather than distribution.
 */
public interface RandomPort {
    /**
     * A value in `0 until bound`, uniformly distributed.
     *
     * @param bound exclusive upper bound; must be positive. An implementation rejects a
     *   non-positive bound rather than returning a defensible-looking zero, because a
     *   silently absorbed bad bound is indistinguishable from a working generator that
     *   always answers the same.
     */
    public fun nextInt(bound: Int): Int
}
