package uk.m4xy.modus.architecture

/**
 * The analyser behind [AmbientCapabilityPortSourceTest], separated from it so that its
 * **input surface** can be asserted on synthetic sources rather than only through a verdict
 * over the real tree (`doc:35-testing#fixture-variation`, and the pattern
 * `DefensiveCopyInputSurfaceTest` established).
 *
 * ## Why this reads source
 *
 * A bytecode rule cannot see the thing this rule exists to prevent. A `@JvmInline value
 * class` erases to its underlying type in every JVM signature, so
 *
 * ```
 * public fun newActorId(): ActorId
 * ```
 *
 * compiles to `public abstract java.lang.String newActorId-TPKKjuw()`. `ActorId` survives
 * only in the `@kotlin.Metadata` blob and in the mangling suffix, neither of which is a type
 * reference, so `directDependenciesFromSelf` is empty of it and
 * [ArchitectureRulesTest.ambientCapabilityPortsAreLeaf] has nothing to reject. A parameter
 * position erases identically.
 *
 * That matters here more than anywhere: **every identifier in the model is a value class** —
 * `DomainId`, `ActorId`, `GrantId`, `Capability`, `StateName`, `DomainName` — so a bytecode
 * rule guarding the port surface is blind to precisely the family of types it was written to
 * keep off it.
 *
 * This is the same erasure `bean:0034` found in the published-language rule, and the fix is
 * the one that bean established: read source, where a reference is still a reference
 * (`architecture-tests/build.gradle.kts`, the `modus.repoRoot` comment). Neither rule
 * replaces the other — bytecode catches what a compiler generates, source catches what a
 * compiler erases.
 */
object AmbientCapabilityPortSource {
    /** The package this analyser guards, relative to the repository root. */
    const val SOURCE_ROOT: String = "core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/port"

    private const val OWN_PACKAGE = "uk.m4xy.modus.core.domain.port"

    /**
     * Import prefixes an ambient-capability port may name.
     *
     * Deliberately narrower than the bytecode rule's `LEAF_SAFE_PACKAGES`, which also permits
     * `java.util`. A port that belongs to no context and is injected into all of them has no
     * business naming a JDK collection or utility type: Kotlin's own `List` and `Map` need no
     * import. Where the two rules disagree, the stricter one is the guarantee and the looser
     * one is the complement.
     */
    private val ALLOWED_IMPORT_PREFIXES = listOf("java.time.", "kotlin.", "java.lang.")

    private val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
    private val IMPORT = Regex("""^\s*import\s+([A-Za-z_][\w.]*)""", RegexOption.MULTILINE)
    private val PACKAGE_DECLARATION = Regex("""^\s*package\s+[\w.]+""", RegexOption.MULTILINE)
    private val MODUS_QUALIFIED = Regex("""uk\.m4xy\.modus\.[\w.]+""")

    /**
     * Every reason [source] would violate the leaf rule, as human-readable strings.
     *
     * Two shapes, because those are the only two ways one Kotlin file can name a type
     * declared in another package: an `import`, or a fully-qualified name at the use site.
     * Both are plain text and both survive erasure, which is the whole point.
     */
    fun violations(source: String): List<String> {
        val code = stripComments(source)
        val forbiddenImports =
            IMPORT
                .findAll(code)
                .map { it.groupValues[1] }
                .filterNot { imported -> ALLOWED_IMPORT_PREFIXES.any { imported.startsWith(it) } }
                .map { "imports '$it'" }

        // The file's own package declaration is a qualified name and is not a reference.
        val withoutPackageDeclaration = PACKAGE_DECLARATION.replace(code, "")
        val forbiddenQualified =
            MODUS_QUALIFIED
                .findAll(withoutPackageDeclaration)
                .map { it.value }
                .filterNot { it.startsWith("$OWN_PACKAGE.") || it == OWN_PACKAGE }
                .map { "names '$it'" }

        return (forbiddenImports + forbiddenQualified).distinct().toList()
    }

    /**
     * What the scan **read** out of [source], before any judgement about it.
     *
     * [violations] answers "is this file legal"; this answers "what did the regexes actually
     * see". They fail differently, and only the second catches a regex that stopped matching:
     * an `IMPORT` pattern that no longer fires returns no imports, so [violations] returns
     * empty and the verdict is a confident green over a file the scan never read.
     *
     * `DefensiveCopyInputSurfaceTest` records six escapes that all arrived in the parse
     * rather than in the rules. This is that separation applied one gate earlier.
     */
    fun perceived(source: String): Perception {
        val code = stripComments(source)
        return Perception(
            imports = IMPORT.findAll(code).map { it.groupValues[1] }.toList(),
            qualifiedNames =
                MODUS_QUALIFIED
                    .findAll(PACKAGE_DECLARATION.replace(code, ""))
                    .map { it.value }
                    .toList(),
        )
    }

    /** What [perceived] extracted: the two shapes a cross-package reference can take. */
    data class Perception(
        val imports: List<String>,
        val qualifiedNames: List<String>,
    )

    /**
     * KDoc legitimately names other packages in prose — the ports' own documentation
     * discusses `identity.published` at length, and explaining why a type may not appear is
     * not the same as making it appear. Only code counts.
     */
    private fun stripComments(source: String): String =
        source
            .replace(BLOCK_COMMENT, "")
            .lineSequence()
            .joinToString("\n") { it.substringBefore("//") }
}
