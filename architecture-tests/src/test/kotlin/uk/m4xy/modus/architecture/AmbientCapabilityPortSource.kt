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
 * class` erases to its underlying type in every JVM signature, so a port returning `ActorId`
 * compiles to `public abstract java.lang.String newActorId-TPKKjuw()`. `ActorId` survives only
 * in the `@kotlin.Metadata` blob and in the mangling suffix, neither of which is a type
 * reference, so `directDependenciesFromSelf` is empty of it and
 * [ArchitectureRulesTest.ambientCapabilityPortsAreLeaf] has nothing to reject.
 *
 * That matters here more than anywhere: **every identifier in the model is a value class** —
 * `DomainId`, `ActorId`, `GrantId`, `Capability`, `StateName`, `DomainName` — so a bytecode
 * rule guarding the port surface is blind to precisely the family of types it was written to
 * keep off it. Same erasure as `bean:0034`; same remedy.
 *
 * ## Why the stripper is a lexer and not a regex
 *
 * The first version deleted a lazy slash-star regex with `DOT_MATCHES_ALL` **before** anything
 * was parsed. A slash-star inside a **string literal** therefore opened a phantom block comment
 * that ran to the next star-slash anywhere in the file — the close of an ordinary KDoc further
 * down — and everything between was deleted from the scan's input. One line of legal Kotlin
 * blinded the gate: a `@Suppress` whose argument is a slash-star, above a declaration returning
 * a foreign type. `@Suppress` takes `vararg String` and ignores unknown names, so it compiles,
 * and all five guards reported green (`bean:0065`).
 *
 * A lexer that recognises literals **before** comments cannot be opened from inside one. It
 * also fails closed: [Perception.unterminated] is set when the source ends inside a comment or
 * a string, and [violations] reports that as a violation in its own right. A scan that cannot
 * account for the whole file must not return a confident empty verdict.
 */
object AmbientCapabilityPortSource {
    /** The package this analyser guards, relative to the repository root. */
    const val SOURCE_ROOT: String = "core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/port"

    private const val OWN_PACKAGE = "uk.m4xy.modus.core.domain.port"

    /**
     * Prefixes an ambient-capability port may name, in an import **or** at a use site.
     *
     * Deliberately narrower than the bytecode rule's `LEAF_SAFE_PACKAGES`, which also permits
     * `java.util`. A port belonging to no context and injected into all of them has no
     * business naming a JDK collection or utility type; Kotlin's own `List` and `Map` need no
     * import. Where the two rules disagree, the stricter one is the guarantee.
     *
     * **Known gap:** `kotlin.` is a prefix, so `kotlin.io.path.Path` is allow-listed here while
     * `doc:00-constitution` §1.3 bans a path type from the domain outright. The bytecode rule
     * catches that one; this arm does not. Recorded in `bean:0065` rather than fixed, because
     * narrowing `kotlin.` correctly means enumerating the parts of the standard library a port
     * may name, which is a decision rather than a patch.
     */
    private val ALLOWED_PREFIXES = listOf("java.time.", "kotlin.", "java.lang.")

    private val IMPORT = Regex("""^\s*import\s+([A-Za-z_][\w.]*)""", RegexOption.MULTILINE)
    private val PACKAGE_DECLARATION = Regex("""^\s*package\s+[\w.]+""", RegexOption.MULTILINE)

    /**
     * A fully-qualified type reference: one or more lower-case package segments followed by a
     * capitalised type name.
     *
     * **Not** scoped to `uk.m4xy.modus`, which is what the first version did. That made the arm
     * modus-only, so `fun raw(): java.util.UUID` written inline was accepted while
     * `import java.util.UUID` was rejected — and `java.util` is on the bytecode rule's
     * allowlist, so the inline form passed **both** rules (`bean:0065`). The arm now checks
     * every qualified name against [ALLOWED_PREFIXES], exactly as the import arm does.
     */
    private val QUALIFIED_TYPE = Regex("""\b[a-z][A-Za-z0-9_]*(?:\.[a-z][A-Za-z0-9_]*)*\.[A-Z][A-Za-z0-9_]*""")

    private val DECLARATION = Regex("""\bfun\s+([A-Za-z_][A-Za-z0-9_]*)""")

    /** What [perceived] read out of a source, before any judgement about it. */
    data class Perception(
        val imports: List<String>,
        val qualifiedNames: List<String>,
        /** Every `fun` name the scan could see. A **positive** expectation: emptiness is failure. */
        val declarations: List<String>,
        /** True when the source ended inside a comment or a string, so the scan is not trustworthy. */
        val unterminated: Boolean,
    )

    /**
     * Every reason [source] would violate the leaf rule, as human-readable strings.
     *
     * Three shapes: an import outside the allowlist, a qualified type reference outside it,
     * and a source the scan could not fully account for.
     */
    fun violations(source: String): List<String> {
        val perception = perceived(source)
        if (perception.unterminated) {
            return listOf("source ends inside a comment or string literal, so the scan cannot have read it")
        }
        val badImports = perception.imports.filterNot { allowed(it) }.map { "imports '$it'" }
        val badNames = perception.qualifiedNames.filterNot { allowed(it) }.map { "names '$it'" }
        return (badImports + badNames).distinct()
    }

    /**
     * What the scan **read**, before any judgement about it.
     *
     * [violations] answers "is this file legal"; this answers "what did the scan actually see".
     * They fail differently, and only the second catches a scan that read nothing — which is
     * why [declarations] exists. Asserting that imports and qualified names are *empty* cannot
     * distinguish a clean file from a blinded scan, because empty is what both produce:
     * **an assertion whose expected value is also the failure signature asserts nothing**
     * (`bean:0065`).
     */
    fun perceived(source: String): Perception {
        val stripped = strip(source)
        val code = PACKAGE_DECLARATION.replace(stripped.code, "")
        return Perception(
            imports = IMPORT.findAll(code).map { it.groupValues[1] }.toList(),
            qualifiedNames =
                QUALIFIED_TYPE
                    .findAll(code)
                    .map { it.value }
                    .filterNot { it == OWN_PACKAGE || it.startsWith("$OWN_PACKAGE.") }
                    .toList(),
            declarations = DECLARATION.findAll(code).map { it.groupValues[1] }.toList(),
            unterminated = stripped.unterminated,
        )
    }

    private fun allowed(name: String): Boolean = ALLOWED_PREFIXES.any { name.startsWith(it) }

    private data class Stripped(
        val code: String,
        val unterminated: Boolean,
    )

    /**
     * Comments removed, string and character literals emptied, in one left-to-right pass.
     *
     * Order is the whole point: a literal is recognised **before** a comment can open inside
     * it, and a comment before a quote inside it can open a literal. Literal *contents* are
     * discarded rather than kept, so a string can neither open a comment nor look like a
     * qualified type name.
     *
     * Newlines inside every skipped region are preserved, so the `MULTILINE` anchors in
     * [IMPORT] and [PACKAGE_DECLARATION] still see the line structure they were written for.
     */
    private fun strip(source: String): Stripped {
        val out = StringBuilder(source.length)
        var i = 0
        var unterminated = false
        while (i < source.length) {
            val next =
                when {
                    source.startsWith("/*", i) -> skipBlockComment(source, i)
                    source.startsWith("//", i) -> skipLineComment(source, i)
                    source.startsWith("\"\"\"", i) -> skipRawString(source, i)
                    source[i] == '"' || source[i] == '\'' -> skipCharOrString(source, i)
                    else -> ORDINARY
                }
            when (next) {
                ORDINARY -> {
                    out.append(source[i])
                    i++
                }

                UNTERMINATED -> {
                    unterminated = true
                    i = source.length
                }

                else -> {
                    repeat(source.substring(i, next).count { it == '\n' }) { out.append('\n') }
                    i = next
                }
            }
        }
        return Stripped(out.toString(), unterminated)
    }

    /** Kotlin block comments nest, so depth is counted rather than matched. */
    private fun skipBlockComment(
        source: String,
        from: Int,
    ): Int {
        var i = from + 2
        var depth = 1
        while (i < source.length && depth > 0) {
            when {
                source.startsWith("/*", i) -> {
                    depth++
                    i += 2
                }

                source.startsWith("*/", i) -> {
                    depth--
                    i += 2
                }

                else -> {
                    i++
                }
            }
        }
        return if (depth == 0) i else UNTERMINATED
    }

    /** A line comment ends at the newline, which is left for [strip] to preserve. */
    private fun skipLineComment(
        source: String,
        from: Int,
    ): Int {
        var i = from
        while (i < source.length && source[i] != '\n') i++
        return i
    }

    private fun skipRawString(
        source: String,
        from: Int,
    ): Int {
        val end = source.indexOf("\"\"\"", from + 3)
        return if (end < 0) UNTERMINATED else end + 3
    }

    /** A single-quoted or double-quoted literal, honouring backslash escapes. */
    private fun skipCharOrString(
        source: String,
        from: Int,
    ): Int {
        val quote = source[from]
        var i = from + 1
        while (i < source.length) {
            when (source[i]) {
                '\\' -> i += 2
                quote -> return i + 1
                '\n' -> return UNTERMINATED
                else -> i++
            }
        }
        return UNTERMINATED
    }

    /** [strip] sentinels: this character is ordinary, and this region never closed. */
    private const val ORDINARY = -1
    private const val UNTERMINATED = -2
}
