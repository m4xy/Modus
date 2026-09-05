package uk.m4xy.modus.adapter.persistence.flatfile

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

/**
 * `doc:40-durability` §6.4 — every document write is conditional on the version the writer
 * read — and §3, the store root a write may not escape.
 */
class OptimisticConcurrencyIntegrationTest {
    @TempDir
    lateinit var root: Path

    private val store: DocumentStore get() = DocumentStore(root)

    @Test
    fun `a document is created by writing against the absent version`() {
        val target = root.resolve("actor.md")

        store.read(target) shouldBe null
        val version = store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        Files.readString(target) shouldBe "first"
        store.read(target)!!.version shouldBe version
        version shouldNotBe DocumentVersion.ABSENT
    }

    @Test
    fun `a write conditional on the current version succeeds`() {
        val target = root.resolve("actor.md")
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        val read = store.read(target)!!
        read.bytes.decodeToString() shouldBe "first"
        store.write(target, read.version, "second".toByteArray())

        Files.readString(target) shouldBe "second"
    }

    @Test
    fun `a write conditional on a version someone else replaced is refused, carrying the current one`() {
        val target = root.resolve("actor.md")
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        // Two writers, both holding the version they read. The second one lands first.
        val readByBoth = store.read(target)!!.version
        val nowCurrent = store.write(target, readByBoth, "written by the other one".toByteArray())

        val refused =
            shouldThrow<StaleWriteException> {
                store.write(target, readByBoth, "written by the one that read first".toByteArray())
            }

        refused.path shouldBe target
        refused.expected shouldBe readByBoth
        // The current version travels with the refusal so the caller can re-read against it,
        // which is what makes §6.4's "re-read, re-apply, retry" a bounded loop rather than a
        // guess.
        refused.current shouldBe nowCurrent
        Files.readString(target) shouldBe "written by the other one"
    }

    @Test
    fun `re-reading after a refusal and writing against the new version succeeds`() {
        val target = root.resolve("actor.md")
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())
        val stale = store.read(target)!!.version
        store.write(target, stale, "second".toByteArray())

        shouldThrow<StaleWriteException> { store.write(target, stale, "third".toByteArray()) }

        store.write(target, store.read(target)!!.version, "third".toByteArray())
        Files.readString(target) shouldBe "third"
    }

    @Test
    fun `creating a document that already exists is refused, so a create cannot silently replace`() {
        val target = root.resolve("actor.md")
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        val refused =
            shouldThrow<StaleWriteException> {
                store.write(target, DocumentVersion.ABSENT, "second".toByteArray())
            }

        refused.expected shouldBe DocumentVersion.ABSENT
        refused.current shouldNotBe DocumentVersion.ABSENT
        Files.readString(target) shouldBe "first"
    }

    @Test
    fun `a hand edit outside Modus is seen, because the version is a hash of the bytes on disk`() {
        // doc:40-durability §8: hand-editing a file outside Modus is a supported workflow,
        // and a counter only Modus maintains could not see one. This is why the version is
        // a content hash, and it is asserted rather than argued.
        val target = root.resolve("actor.md")
        val version = store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        Files.writeString(target, "edited by a human in an editor")

        shouldThrow<StaleWriteException> { store.write(target, version, "second".toByteArray()) }
        Files.readString(target) shouldBe "edited by a human in an editor"
    }

    @Test
    fun `the document the store hands out is a copy, so a caller cannot mutate the store's answer`() {
        val target = root.resolve("actor.md")
        store.write(target, DocumentVersion.ABSENT, "first".toByteArray())

        val read = store.read(target)!!
        read.bytes[0] = 'X'.code.toByte()

        read.bytes.decodeToString() shouldBe "first"
    }

    @Test
    fun `a target outside the store root is refused rather than written`() {
        val outside = root.parent.resolve("escaped.md")

        val refused =
            shouldThrow<PathOutsideStoreException> {
                store.write(outside, DocumentVersion.ABSENT, "content".toByteArray())
            }
        // The refusal names both halves, so an operator reading the 409 can tell a crafted
        // identifier from a misconfigured root.
        refused.path shouldBe outside
        refused.root shouldBe PathLocks.canonical(root)

        shouldThrow<PathOutsideStoreException> { store.read(outside) }
        Files.exists(outside) shouldBe false
    }

    @Test
    fun `a traversal that only leaves the root once resolved is refused`() {
        // `root/../escaped.md` starts with the root as a string and is outside it as a path.
        // An identifier arrives here authorised to be a path segment unencoded
        // (bean:0009 review thread 6), so the check has to be on the resolved path.
        val traversal = root.resolve("..").resolve("escaped.md")

        shouldThrow<PathOutsideStoreException> {
            store.write(traversal, DocumentVersion.ABSENT, "content".toByteArray())
        }
        Files.exists(PathLocks.canonical(traversal)) shouldBe false
    }

    @Test
    fun `a nested target inside the root is written, so the guard is not simply refusing everything`() {
        val nested = root.resolve("identity").resolve("actors").resolve("alice.md")
        Files.createDirectories(nested.parent)

        store.write(nested, DocumentVersion.ABSENT, "alice".toByteArray())

        Files.readString(nested) shouldBe "alice"
    }
}
