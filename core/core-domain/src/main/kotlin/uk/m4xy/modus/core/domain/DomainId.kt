package uk.m4xy.modus.core.domain

/**
 * The tenant identifier that scopes every other bounded context.
 *
 * Invariant: a slug, because it is the first path segment after `/domains`
 * (`doc:10-architecture#domain-root-convention`) and a directory name in the store.
 *
 * Lower case is load-bearing, not cosmetic. The store is a flat file tree, and on a
 * case-insensitive volume — APFS on the development platform, NTFS in CI containers —
 * `Modus-Core/` and `modus-core/` are one directory. Two ids unequal in the domain would
 * be one tenant on disk, and the 404-not-403 rule cannot help when both ids resolve.
 *
 * **Shared kernel, not `identity`'s** (`adr:0004-domain-id-shared-kernel`). It sits beside
 * [DomainEvent] rather than in a context's published package because every context's
 * events name the domain they concern, and `doc:10-architecture#bounded-contexts` §3.1
 * makes a published package a leaf: a type in `..domain.event..` may reference its own
 * context's published language and nothing else. Two contexts each declaring a `DomainId`
 * would be two unequal types for one tenant. It lived in `identity.published` only
 * because `identity` was modelled first.
 */
@JvmInline
public value class DomainId(
    public val value: String,
) {
    init {
        require(SLUG.matches(value)) { "domainId must be a slug: '$value'" }
    }

    private companion object {
        /**
         * Lower kebab, 3..64 characters, no leading or trailing hyphen.
         *
         * Inside the class, not a top-level `private val`, because a top-level property
         * compiles to a synthetic `DomainIdKt` facade class — which is not in the shared
         * kernel and so is not leaf-safe. `sharedKernelIsLeaf` caught exactly that on its
         * first run. `Capability` holds its regex the same way.
         */
        private val SLUG = Regex("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$")
    }
}
