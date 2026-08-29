package uk.m4xy.modus.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * `DomainId` is shared kernel, not `identity`'s (`adr:0004-domain-id-shared-kernel`), so
 * its tests live beside it rather than in `identity`'s published-language suite. Moved
 * unaltered from `IdentityPublishedLanguageTest`; the invariant did not change.
 */
class DomainIdTest {
    @Test
    fun `accepts a domain id that is a slug`() {
        DomainId("modus-core").value shouldBe "modus-core"
    }

    @Test
    fun `refuses a domain id that is not a slug`() {
        shouldThrow<IllegalArgumentException> { DomainId("Modus Core") }
            .message shouldBe "domainId must be a slug: 'Modus Core'"
    }

    /**
     * Separate from the test above, which fails on the *space*: the case half of the rule
     * is what stops `Modus-Core/` and `modus-core/` being one directory on the
     * case-insensitive volumes the flat-file store runs on.
     */
    @Test
    fun `refuses a domain id that is not lower case`() {
        shouldThrow<IllegalArgumentException> { DomainId("Modus-Core") }
            .message shouldBe "domainId must be a slug: 'Modus-Core'"
    }
}
