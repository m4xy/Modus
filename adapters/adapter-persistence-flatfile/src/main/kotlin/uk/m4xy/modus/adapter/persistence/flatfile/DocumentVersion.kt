package uk.m4xy.modus.adapter.persistence.flatfile

import java.security.MessageDigest

/**
 * The version a document write is conditional on: a content hash of the bytes the writer
 * last read for that path (`doc:40-durability` §6.4).
 *
 * A content hash rather than a counter, because the store must not assume it wrote the
 * last version — §8 makes hand-editing a file outside Modus a supported workflow, and a
 * counter only a writer maintains cannot see one.
 *
 * [ABSENT] is the version of a path that holds no document. It is a real value and not a
 * null so that creating a document is the same conditional operation as replacing one:
 * there is no entry point that writes without stating what it expected to find, which is
 * what §6.4 means by "there is no unconditional overwrite API".
 */
@JvmInline
public value class DocumentVersion private constructor(
    public val hash: String,
) {
    public companion object {
        private const val ALGORITHM = "SHA-256"

        /** The version of a path that holds no document. */
        public val ABSENT: DocumentVersion = DocumentVersion("absent")

        /** The version of a document whose bytes are exactly [bytes]. */
        public fun of(bytes: ByteArray): DocumentVersion =
            DocumentVersion(
                MessageDigest
                    .getInstance(ALGORITHM)
                    .digest(bytes)
                    .joinToString("") { byte -> "%02x".format(byte) },
            )
    }
}
