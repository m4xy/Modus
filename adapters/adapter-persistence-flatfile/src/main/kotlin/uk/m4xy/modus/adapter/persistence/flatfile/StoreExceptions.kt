package uk.m4xy.modus.adapter.persistence.flatfile

import java.nio.file.Path

/**
 * A lock could not be taken inside its timeout, so the write was abandoned rather than
 * blocked indefinitely (`doc:40-durability` §6.2, §6.3).
 *
 * The REST adapter maps it to `409 Conflict`. It is thrown for an in-process lock that
 * timed out and for a cross-process lock another process holds, because the caller's
 * options are the same in both cases: retry, or give up.
 */
public class StoreContentionException(
    message: String,
) : RuntimeException(message)

/**
 * The document at [path] changed between the read the caller based its write on and the
 * write itself, so the write was refused (`doc:40-durability` §6.4).
 *
 * [current] is carried so the `409 Conflict` body can hold the version the caller must
 * re-read against, and so a test can tell "somebody else wrote" apart from "the store
 * rejected my own version".
 */
public class StaleWriteException(
    public val path: Path,
    public val expected: DocumentVersion,
    public val current: DocumentVersion,
) : RuntimeException(
        "$path was written by someone else: expected version ${expected.hash}, found ${current.hash}",
    )

/**
 * A target path resolved outside the store root, so nothing was written.
 *
 * An id reaches this adapter authorised to become a path segment unencoded
 * (`bean:0009` review thread 6), which makes the store the last check between a crafted
 * identifier and a write anywhere on the volume.
 */
public class PathOutsideStoreException(
    public val path: Path,
    public val root: Path,
) : RuntimeException("$path is not inside the store root $root")
