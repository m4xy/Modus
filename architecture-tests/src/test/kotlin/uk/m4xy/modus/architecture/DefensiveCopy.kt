package uk.m4xy.modus.architecture

/**
 * The source-level analyser behind [DefensiveCopySourceTest]: it reads Kotlin **source** and
 * reports every domain member that lets a collection a type owns escape it
 * (`doc:20-ddd-practices#value-objects` §3.1).
 *
 * **Why source and not bytecode.** `javap -p` on the compiled `GrantIssued` lists fields
 * `grantId, actorId, domainId, occurredAt, issued` and **no `capabilities` field at all** — a
 * Kotlin property with a custom getter compiles to a method and nothing else, so "the backing
 * field is private" has no bytecode witness, and only `data`'s `copy` survives for a bytecode
 * rule to see. That is the weaker half: it misses `bean:0009`'s shape.
 * `PublishedLanguageSourceTest` established the same argument from the other side
 * (`bean:0034`).
 *
 * **Why not Detekt**, which sees a real AST and would be the better home: Modus runs the Detekt
 * 1.23.8 CLI and has never built a custom rule. `bean:0026` records that none of the eleven
 * `doc:30-code-style` §4 rules exist; making the first one exist is that bean's whole subject.
 *
 * **What three rounds of review taught this file, in order.**
 *
 * 1. Plant the shape you claim to catch. A rule nobody watched reject one is a claim
 *    (`doc:00-constitution#observed-failing`).
 * 2. A stated blind-spot list is part of the gate, so plant every claim in **both**
 *    directions. A wrong list stops readers looking exactly as an unfalsifiable gate does.
 * 3. **Plant every claim's enabling condition.** Round two's fixes were all correct and three
 *    escapes still walked in underneath them, through the parser's *input* rather than its
 *    logic: a backing field with an inferred type never became a `Property`, so the function
 *    rule had nothing to compare against; a generic `typealias Bag<T>` was never collected; an
 *    anonymous `object :` pushed no owner. **A lexical gate's real blind-spot list is the list
 *    of shapes its regexes cannot see**, which is not the same list as the rules it does not
 *    implement.
 *
 * 4. **A fix that replaces an assumption with an allowlist has renamed it, not removed it.**
 *    "Requires a type annotation" became "matches one of 32 initialiser verbs"; "no lambda"
 *    became "no `{`". Each replacement was smaller than what it replaced and each still failed
 *    open in the same direction — `.take(`, `.chunked(` and `also(Escapes::capture)` all walked
 *    through. Prefer a rule stated as what must be *absent*: a copy chain now takes no arguments
 *    at all, which is checkable without knowing which verbs exist.
 *
 * `explicitApi()` is assumed nowhere, and this is the third round in which that has been claimed,
 * so it is an audit rather than an assertion. It supplies exactly three optional syntactic
 * elements: a member's visibility modifier, a property's type annotation, and a function's return
 * type. The parser needs none — member-versus-local is brace depth; a property with no declared
 * type is read from its initialiser and, failing that, from being private at all; and a function
 * with no return type is judged by what its body returns. Grep for `substringAfter(':'` and
 * `startsWith(":")` before believing this paragraph again.
 *
 * The parser assumes ktlint `ktlint_official` formatting, which `ktlintMainSourceSetCheck`
 * enforces in the same `qualityCheck` run. That guards **formatting only**, and formatting is
 * not the same thing as shape.
 */
internal object DefensiveCopy {
    /** One member property declaration, as written. */
    internal data class Property(
        val owner: String,
        val ownerKind: String,
        val ownerIsData: Boolean,
        val ownerCtorPrivate: Boolean,
        val name: String,
        val declaredType: String,
        val isPrivate: Boolean,
        /** The accessor's expression, or `null` when the property is stored. */
        val accessor: String?,
        /** Declared in the primary constructor, so a caller supplies it directly. */
        val inConstructor: Boolean,
        /** Stored from a constructor parameter without copying it. */
        val fromParameter: Boolean,
        val collection: Boolean,
        val line: Int,
    ) {
        override fun toString(): String = "$owner.$name: ${declaredType.ifBlank { INFERRED }}"
    }

    /** One member function, with every expression it can return. */
    internal data class Function(
        val owner: String,
        val name: String,
        val isPrivate: Boolean,
        /** Blank when the function has none, which `explicitApi()` permits off the public API. */
        val returnType: String,
        val returned: List<String>,
        val collection: Boolean,
        val line: Int,
    )

    /** An assignment to a property, which is how an `init` block stores a constructor parameter. */
    internal data class Assignment(
        val owner: String,
        val target: String,
        val expression: String,
        val parameters: Set<String>,
        val line: Int,
    )

    /**
     * `typealias Capabilities = Set<Capability>` would otherwise defeat [COLLECTION] with one
     * keyword, so every alias in every scanned file is collected first and fed back in.
     *
     * Resolved to a fixed point, so an alias of an alias counts, and **generic** aliases are
     * matched too: `typealias Bag<T> = List<T>` slipped through the first version of this,
     * because the name was read as `\w+` and a `<` cannot follow one.
     */
    fun collectionAliases(sources: List<String>): Set<String> {
        val declared =
            sources
                .flatMap { source -> strip(source).lines().mapNotNull { TYPEALIAS.find(it) } }
                .associate { it.groupValues[1] to it.groupValues[2] }
        val collections = declared.filterValues { COLLECTION.containsMatchIn(it) }.keys.toMutableSet()
        var grew = true
        while (grew) {
            grew = collections.addAll(declared.filterValues { names(it, collections) }.keys)
        }
        return collections
    }

    /**
     * Every reason [source] breaks §3.1, one line each; empty when it does not.
     *
     * [label] only names the file in the message; nothing is inferred from it.
     */
    fun violations(
        label: String,
        source: String,
        aliases: Set<String> = emptySet(),
    ): List<String> {
        val scan = Scanner(source, aliases).run()
        // Every private property, not only the ones whose type says "collection". Narrowing this
        // to collection-typed properties is what let `private val issued = copyOf(granted)` out:
        // the field a function rule compares against has to exist before the rule can fire, and
        // deciding it does not exist is a decision about the parser's input, not about the rule.
        val backing =
            scan.properties
                .filter { it.isPrivate }
                .map { it.name }
                .toSet()
        val collections =
            scan.properties
                .filter { it.collection }
                .map { it.owner to it.name }
                .toSet()
        val exposed =
            scan.properties.filter { it.collection }.mapNotNull { property ->
                fault(property, scan.secondaries)?.let { "$label:${property.line}: $property — $it" }
            }
        val leaked =
            scan.functions.filter { !it.isPrivate }.flatMap { function ->
                function.returned.mapNotNull { expression -> leak(label, function, expression, backing) }
            }
        val stored =
            scan.assignments
                .filter { (it.owner to it.target) in collections && root(it.expression) in it.parameters }
                .filterNot { isCopyChain(it.expression) }
                .map { "$label:${it.line}: ${it.owner}.${it.target} — $UNCOPIED_PARAMETER" }
        return exposed + leaked + stored
    }

    /**
     * A function leaks when what it returns **mentions** a backing field and is not a copy.
     *
     * Keyed on mention rather than on the expression's root, because the root of
     * `mapOf("all" to held)` is `mapOf` — review's X2. The accessor rule has always demanded a
     * copy chain outright; this is the same demand, and the two paths now agree rather than
     * enforcing different standards on the same escape.
     *
     * A function that mentions a backing field and declares **no return type** is a violation in
     * its own right. That is the last place `explicitApi()` was load-bearing: it forces a return
     * type on public API only, so `internal fun raised() = held.also(::noop)` had nothing to be
     * judged against. Rather than guess the type from the expression — which is where the
     * previous two attempts turned into allowlists — the gate requires the one token that
     * settles it.
     *
     * State the cost at full size, because an earlier draft understated it: the rule is **no
     * non-private function without a return type may mention any private field, of any type**.
     * `internal fun size() = held.size` and `internal fun isFrozen() = frozen` are both rejected,
     * and neither hands anything out. That is deliberate — `backing` is every private field
     * precisely because a field whose type the scan cannot read is the case this exists for — and
     * it fails closed. It costs nothing in `core-domain` today, where no function omits a return
     * type; it is not free in general. `bean:0064` carries narrowing it.
     */
    private fun leak(
        label: String,
        function: Function,
        expression: String,
        backing: Set<String>,
    ): String? {
        val field = mentioned(expression, backing) ?: return null
        val where = "$label:${function.line}: ${function.owner}.${function.name}()"
        return when {
            function.returnType.isBlank() -> "$where — $UNDECLARED_RETURN"

            function.collection && !isCopyChain(expression) -> "$where — ${leakedBy(expression, field)}"

            // A declared return type that names no collection disables the leak arm entirely, so
            // `public fun any(): Any = held` passes. Realistic containers are caught because
            // COLLECTION matches inside the generic — `Pair<List<X>, Int>`, `Map<K, List<V>>` —
            // and what survives is a signature nothing in `core-domain` writes. `bean:0064`, and
            // §3.1's cost table, carry it rather than this comment alone.
            else -> null
        }
    }

    private fun mentioned(
        expression: String,
        backing: Set<String>,
    ): String? = backing.firstOrNull { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(expression) }

    /** Every member property in [source]; the gate counts these to prove it examined something. */
    fun properties(
        source: String,
        aliases: Set<String> = emptySet(),
    ): List<Property> = Scanner(source, aliases).run().properties

    /** Every member function in [source]. `DefensiveCopyInputSurfaceTest` asserts against this. */
    fun functions(
        source: String,
        aliases: Set<String> = emptySet(),
    ): List<Function> = Scanner(source, aliases).run().functions

    /** Types declaring a secondary constructor a caller can reach. Part of the input surface. */
    fun secondaries(source: String): Set<String> = Scanner(source, emptySet()).run().secondaries

    /** Assignments to a property, which is how an `init` block stores a constructor parameter. */
    fun assignments(source: String): List<Assignment> = Scanner(source, emptySet()).run().assignments

    /** §3.1 has two halves, and so does this: nothing gets in uncopied, nothing gets out uncopied. */
    private fun fault(
        property: Property,
        secondaries: Set<String>,
    ): String? = if (property.ownerIsData) DATA_CLASS else copyIn(property, secondaries) ?: copyOut(property)

    /**
     * The ways a caller's collection becomes this type's backing collection.
     *
     * Structural, not semantic: the constructor must be unreachable, so the only way in is a
     * named factory. Whether that factory copies is not checked — see §3.1's blind-spot table.
     */
    private fun copyIn(
        property: Property,
        secondaries: Set<String>,
    ): String? =
        when {
            property.inConstructor && !property.ownerCtorPrivate -> PUBLIC_CONSTRUCTOR
            property.inConstructor && property.owner in secondaries -> SECONDARY
            property.fromParameter -> UNCOPIED_PARAMETER
            else -> null
        }

    /** The ways the backing collection becomes a reference a caller keeps. */
    private fun copyOut(property: Property): String? =
        when {
            property.isPrivate -> null
            property.ownerKind == "interface" && property.accessor == null -> null
            property.accessor == null -> PUBLISHED_FIELD
            !isCopyChain(property.accessor) -> notACopy(property.accessor)
            else -> null
        }

    private fun notACopy(accessor: String): String =
        "the accessor is `${accessor.collapsed()}`, which is not a copy chain. It must be a plain " +
            "chain of calls ending in one of ${COPY_CALLS.joinToString(", ")} — no block body, no lambda, " +
            "and none of $CONTROL_WORDS. Anything else can hand out a live reference down one of its " +
            "paths: a block getter whose last statement copies still leaks through its early return, and " +
            "`steps.also { sink(it) }.toList()` leaks the receiver while returning a copy (bean:0036)."

    private fun leakedBy(
        expression: String,
        field: String,
    ): String =
        "it returns `${expression.collapsed()}`, a live view of the backing collection `$field`, not a copy " +
            "(doc:20-ddd-practices §3.1). `asReversed`, `subList` and a bare field all write through."

    /**
     * True only for an expression that **is** a copy: a plain chain of member accesses and calls
     * whose last call allocates, with no block, no lambda and no control flow.
     *
     * Ending in a copy call is not enough, and neither is a chain whose lambdas are ignored.
     * Round two rejected blocks and control flow but let `skeleton` delete lambda bodies, so
     * `steps.also { Sink.capture(it) }.toList()` reduced to `steps.also.toList` and passed while
     * handing the live list to a sink. A lambda in a copy chain is now a violation; the cost is
     * that `sorted { }.toList()` must be written `sorted(Comparator).toList()`, and that cost
     * falls on the side that fails closed.
     */
    private fun isCopyChain(expression: String): Boolean {
        val expr = expression.trim()
        // Whitespace removed, not collapsed: ktlint wraps a long getter onto its own line and the
        // dot lands after a newline, which a single-call pattern must not be sensitive to.
        val one = expr.replace(WHITESPACE, "")
        return expr.isNotEmpty() &&
            argumentFree(expr) &&
            !CONTROL_FLOW.containsMatchIn(expr) &&
            COPY_CALLS.any { one == it || (SINGLE_CALL.matches(one) && one.endsWith(".$it")) }
    }

    /**
     * True when the expression contains no lambda and no call that takes an argument.
     *
     * Banning the literal `{` was an allowlist wearing a rule's clothes:
     * `granted.also(Escapes::capture).toSet()` has no brace, reduces to a clean spine, and hands
     * the receiver to a sink. Any call that takes an argument can be given the receiver, so a
     * copy chain may take none. `sortedWith(cmp).toList()` therefore fails closed and must be
     * written `sorted().toList()`, or copied first and transformed after — a cost no accessor in
     * `core-domain` pays today.
     */
    private fun argumentFree(expression: String): Boolean {
        var text = expression
        while (true) {
            val next = text.replace("()", "")
            if (next == text) return !next.contains('(') && !next.contains('{')
            text = next
        }
    }

    /** The leading identifier of an expression, which is the value the rest of it is derived from. */
    private fun root(expression: String): String =
        ROOT
            .find(expression.trim())
            ?.groupValues
            ?.get(1)
            .orEmpty()

    // --- the scan -----------------------------------------------------------

    private data class Owner(
        val name: String,
        val kind: String,
        val isData: Boolean,
        val ctorPrivate: Boolean,
    )

    private class Scan(
        val properties: List<Property>,
        val functions: List<Function>,
        val secondaries: Set<String>,
        val assignments: List<Assignment>,
    )

    private data class Tail(
        val type: String,
        val accessor: String?,
        val initialiser: String?,
    )

    /**
     * Walks the stripped source once, tracking paren and brace depth, and attributes every
     * declaration to the type whose body it sits directly in.
     *
     * A `val`/`var` at paren depth above zero is a primary-constructor property — no other
     * parameter list in Kotlin may carry `val`. A `val`/`var` at brace depth equal to the
     * enclosing type's body level is a member; anything deeper is inside a function, an
     * accessor or a lambda, and is a local. No visibility modifier is consulted, and no type
     * annotation is required.
     */
    private class Scanner(
        source: String,
        private val aliases: Set<String>,
    ) {
        private val lines = strip(source).lines()
        private val properties = mutableListOf<Property>()
        private val functions = mutableListOf<Function>()
        private val secondaries = mutableSetOf<String>()
        private val assignments = mutableListOf<Assignment>()
        private val owners = ArrayDeque<Triple<Int, Owner, Set<String>>>()
        private var pending: Owner? = null
        private var pendingParams = mutableSetOf<String>()
        private var paren = 0
        private var brace = 0

        fun run(): Scan {
            lines.indices.forEach { visit(it) }
            return Scan(properties, functions, secondaries, assignments)
        }

        private fun visit(index: Int) {
            val line = lines[index]
            val current = owners.lastOrNull()?.second ?: FILE_OWNER
            if (paren == 0) header(line, current)
            if (paren > 0) PARAM.find(line)?.let { pendingParams += it.groupValues[1] }
            if (paren > 0 || brace == (owners.lastOrNull()?.first ?: 0)) declaration(index, line, current)
            if (paren == 0 && owners.isNotEmpty()) other(index, line, current)
            advance(line)
        }

        private fun header(
            line: String,
            current: Owner,
        ) {
            val type = TYPE_DECL.find(line)
            val named =
                type?.let {
                    Owner(
                        it.groupValues[3],
                        it.groupValues[2],
                        "data" in it.modifiers(),
                        PRIVATE_CTOR.containsMatchIn(line),
                    )
                }
            val next =
                when {
                    named != null -> named
                    COMPANION.containsMatchIn(line) -> current.copy(kind = "object", isData = false, ctorPrivate = true)
                    ANONYMOUS.containsMatchIn(line) -> Owner(ANON, "object", false, true)
                    line.isBlank() || FUN_DECL.containsMatchIn(line) -> null
                    else -> return
                }
            if (next !== pending) pendingParams = mutableSetOf()
            pending = next
        }

        private fun declaration(
            index: Int,
            line: String,
            current: Owner,
        ) {
            PROPERTY.find(line)?.let { properties += property(it, pending ?: current, index) }
            if (paren == 0) FUN_DECL.find(line)?.let { functions += function(it, pending ?: current, index) }
        }

        /**
         * The two ways in that are neither the primary constructor nor a property initialiser: a
         * secondary constructor a caller can reach, and an assignment in an `init` block.
         */
        private fun other(
            index: Int,
            line: String,
            current: Owner,
        ) {
            SECONDARY_CTOR.find(line)?.takeIf { "private" !in it.modifiers() }?.let { secondaries += current.name }
            ASSIGN.find(line)?.let {
                assignments +=
                    Assignment(current.name, it.groupValues[1], it.groupValues[2].trim(), owners.last().third, index + 1)
            }
        }

        private fun advance(line: String) {
            val delta = line.count { it == '{' } - line.count { it == '}' }
            paren += line.count { it == '(' } - line.count { it == ')' }
            pending?.takeIf { delta > 0 }?.let {
                owners.addLast(Triple(brace + 1, it, pendingParams.toSet()))
                pending = null
                pendingParams = mutableSetOf()
            }
            brace += delta
            while (owners.isNotEmpty() && brace < owners.last().first) owners.removeLast()
        }

        private fun property(
            match: MatchResult,
            owner: Owner,
            index: Int,
        ): Property {
            val tail = readTail(match.groupValues[3].trim(), lines, index)
            val params = if (paren > 0) pendingParams else owners.lastOrNull()?.third.orEmpty()
            val initialiser = tail.initialiser
            return Property(
                owner = owner.name,
                ownerKind = owner.kind,
                ownerIsData = owner.isData,
                ownerCtorPrivate = owner.ctorPrivate,
                name = match.groupValues[2],
                declaredType = tail.type,
                isPrivate = "private" in match.modifiers(),
                accessor = tail.accessor,
                inConstructor = paren > 0,
                fromParameter = initialiser != null && root(initialiser) in params && !isCopyChain(initialiser),
                collection = isCollection(tail.type, aliases) || (tail.type.isBlank() && inferredCollection(initialiser)),
                line = index + 1,
            )
        }

        /**
         * Reads a function's signature across however many lines ktlint wrapped it onto, then its
         * body — the expression after `=`, or every `return` in a block.
         */
        private fun function(
            match: MatchResult,
            owner: Owner,
            index: Int,
        ): Function {
            val (endLine, endCol) = closingParen(index, match.range.last)
            val tail = lines[endLine].drop(endCol + 1)
            val equals = tail.indexOf('=')
            val opens = tail.indexOf('{')
            val expression = equals >= 0 && (opens < 0 || equals < opens)
            // Cut at the body FIRST, then look for the `:`. Reading the colon first found the one
            // inside `= granted.also(::noop)` and reported a return type of `:noop)`, which made an
            // undeclared return look declared — review's X4, and a parse bug rather than a rule gap.
            val head = tail.take(cut(tail)).trim()
            val type = if (head.startsWith(":")) head.drop(1).trim() else ""
            return Function(
                owner = owner.name,
                name = match.groupValues[2],
                isPrivate = "private" in match.modifiers(),
                returnType = type,
                returned = if (expression) listOf(continued(tail.drop(equals + 1).trim(), lines, endLine)) else block(endLine),
                collection = isCollection(type, aliases),
                line = index + 1,
            )
        }

        private fun cut(tail: String): Int = listOf(tail.indexOf('='), tail.indexOf('{')).filter { it >= 0 }.minOrNull() ?: tail.length

        /** Every `return <expression>` in the block body opening on [from]. */
        private fun block(from: Int): List<String> {
            var depth = 0
            var opened = false
            val returns = mutableListOf<String>()
            for (i in from until lines.size) {
                RETURN.findAll(lines[i]).forEach { returns += it.groupValues[1].trim() }
                depth += lines[i].count { it == '{' } - lines[i].count { it == '}' }
                if (depth > 0) opened = true
                if (opened && depth <= 0) break
            }
            return returns
        }

        /** The position of the `)` closing the parameter list opened at [startLine]/[startCol]. */
        private fun closingParen(
            startLine: Int,
            startCol: Int,
        ): Pair<Int, Int> {
            var depth = 0
            var column = startCol
            for (i in startLine until lines.size) {
                while (column < lines[i].length) {
                    if (lines[i][column] == '(') depth++
                    if (lines[i][column] == ')' && --depth == 0) return i to column
                    column++
                }
                column = 0
            }
            return startLine to startCol
        }
    }

    private fun MatchResult.modifiers(): List<String> = groupValues[1].trim().split(WHITESPACE)

    private fun isCollection(
        type: String,
        aliases: Set<String>,
    ): Boolean = COLLECTION.containsMatchIn(type) || names(type, aliases)

    private fun names(
        type: String,
        aliases: Set<String>,
    ): Boolean = aliases.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(type) }

    /**
     * A property with no declared type is read from its initialiser.
     *
     * `private val issued = granted.toSet()` is a collection, and until review planted it, it
     * was not a `Property` at all — so `raised(): Set<Capability> = issued` had nothing to be
     * compared against and the gate passed a capability being added to a fact already stated.
     * The old rule required an explicit `: Type`, which `explicitApi()` forces on public API
     * only: every `private` field and every member of an `internal` class was outside it.
     */
    private fun inferredCollection(initialiser: String?): Boolean = initialiser != null && COLLECTION_EXPR.containsMatchIn(initialiser)

    /**
     * Splits what follows the property's name into its declared type, its accessor and its
     * initialiser. Any of the three may be absent.
     */
    private fun readTail(
        rest: String,
        lines: List<String>,
        index: Int,
    ): Tail =
        when {
            rest.startsWith(":") -> typed(rest.drop(1), lines, index)
            rest.startsWith("=") -> Tail("", null, continued(rest.drop(1).trim(), lines, index))
            rest.startsWith(GET) -> Tail("", continued(rest.drop(GET.length).trimStart('=', ' '), lines, index), null)
            else -> Tail("", nextLineGetter(lines, index), null)
        }

    private fun typed(
        tail: String,
        lines: List<String>,
        index: Int,
    ): Tail {
        val getter = tail.indexOf(GET)
        val assign = tail.indexOf('=')
        return when {
            getter >= 0 && (assign < 0 || getter < assign) -> {
                Tail(tail.take(getter).asType(), continued(tail.drop(getter + GET.length).trimStart('=', ' '), lines, index), null)
            }

            assign >= 0 -> {
                Tail(tail.take(assign).asType(), null, continued(tail.drop(assign + 1).trim(), lines, index))
            }

            else -> {
                Tail(tail.asType(), nextLineGetter(lines, index), null)
            }
        }
    }

    /** The `get() = …` or `get() { … }` form ktlint moves to its own line when the declaration is long. */
    private fun nextLineGetter(
        lines: List<String>,
        index: Int,
    ): String? {
        val at = (index + 1 until lines.size).firstOrNull { lines[it].isNotBlank() } ?: return null
        val body = lines[at].trimStart()
        return if (body.startsWith(GET)) continued(body.removePrefix(GET).trimStart('=', ' '), lines, at) else null
    }

    /** Appends the lines a wrapped expression or a block continues onto: more indented, and not a member. */
    private fun continued(
        head: String,
        lines: List<String>,
        index: Int,
    ): String {
        val indent = lines[index].takeWhile { it == ' ' }.length
        val rest =
            lines.drop(index + 1).takeWhile { line ->
                line.isNotBlank() &&
                    line.takeWhile { it == ' ' }.length > indent &&
                    PROPERTY.find(line) == null &&
                    TYPE_DECL.find(line) == null &&
                    !line.trimStart().startsWith("fun ")
            }
        return (listOf(head) + rest).joinToString(" ")
    }

    private fun String.asType(): String = trim().trimEnd(',').trim()

    private fun String.collapsed(): String = trim().replace(WHITESPACE, " ")

    /**
     * Blanks comments, string literals **and character literals**, preserving every offset and
     * newline so line numbers survive.
     *
     * The character literal is not an afterthought: `public val open: Char = '('` used to leave
     * an unbalanced paren in the stripped text, after which paren depth never returned to zero,
     * type headers stopped being recognised and every later declaration in the file was
     * attributed to the wrong owner or exempted outright (`bean:0036`, review).
     */
    private fun strip(source: String): String {
        val out = StringBuilder(source.length)
        var i = 0
        while (i < source.length) {
            val end = literalEnd(source, i)
            if (end > i) {
                (i until end).forEach { out.append(if (source[it] == '\n') '\n' else ' ') }
                i = end
            } else {
                out.append(source[i])
                i++
            }
        }
        return out.toString()
    }

    /** The exclusive end of the comment or literal starting at [i], or [i] if there is none. */
    private fun literalEnd(
        source: String,
        i: Int,
    ): Int =
        when {
            source.startsWith(BLOCK_OPEN, i) -> past(source, BLOCK_CLOSE, i + BLOCK_OPEN.length)
            source.startsWith(LINE_COMMENT, i) -> past(source, "\n", i, include = false)
            source.startsWith(RAW_QUOTE, i) -> past(source, RAW_QUOTE, i + RAW_QUOTE.length)
            source[i] == '"' -> delimited(source, i, '"')
            source[i] == '\'' -> delimited(source, i, '\'')
            else -> i
        }

    /** The offset just past the next [close] at or after [from], or the end of [source]. */
    private fun past(
        source: String,
        close: String,
        from: Int,
        include: Boolean = true,
    ): Int {
        val at = source.indexOf(close, from)
        return when {
            at < 0 -> source.length
            include -> at + close.length
            else -> at
        }
    }

    private fun delimited(
        source: String,
        i: Int,
        quote: Char,
    ): Int {
        var j = i + 1
        while (j < source.length && source[j] != quote && source[j] != '\n') {
            j += if (source[j] == '\\') 2 else 1
        }
        return minOf(j + 1, source.length)
    }

    private val FILE_OWNER = Owner("<file>", "object", false, true)

    private const val ANON = "<object>"

    private const val INFERRED = "<inferred>"

    private const val BLOCK_OPEN = "/*"

    private const val BLOCK_CLOSE = "*/"

    private const val LINE_COMMENT = "//"

    private const val RAW_QUOTE = "\"\"\""

    private const val GET = "get()"

    private const val CONTROL_WORDS = "`return`, `if`, `when`, `else`, `try`, `throw`, `?:` or `;`"

    private const val DATA_CLASS =
        "a `data class` cannot own a collection. Its generated constructor binds the caller's instance and " +
            "`copy` hands one straight back, so every invariant holds exactly once (doc:20-ddd-practices §3.1). " +
            "Use a private constructor, a factory that copies in, getters that copy out, and hand-written " +
            "equals/hashCode."

    private const val PUBLIC_CONSTRUCTOR =
        "a collection reaches this type through a constructor a caller can call, so it is never copied IN: the " +
            "caller keeps a live reference to what this type decides with, whatever the getters do " +
            "(doc:20-ddd-practices §3.1). Make the primary constructor `private` and add a named factory that " +
            "copies, the shape `PermissionGrant.issue` and `ProcessDefinition.of` both use."

    private const val UNDECLARED_RETURN =
        "this mentions a private field and declares no return type, so nothing can judge whether it hands out a " +
            "collection (doc:20-ddd-practices §3.1). The check is over EVERY private field, not only " +
            "collection-typed ones, because a field whose type the scan cannot read is exactly the case this " +
            "exists for — so it fires on `internal fun isFrozen() = frozen` too. That is a fail-closed cost, not " +
            "a leak: `explicitApi()` requires a return type on public API only; declare one here."

    private const val UNCOPIED_PARAMETER =
        "this stores a constructor parameter without copying it, so the caller's collection IS the backing " +
            "collection and every invariant holds exactly once (doc:20-ddd-practices §3.1). Copy on the way in."

    private const val SECONDARY =
        "the primary constructor is private, but a secondary constructor a caller can reach delegates to it, so " +
            "a collection still enters uncopied (doc:20-ddd-practices §3.1). Make the secondary constructor " +
            "`private` too, or copy in it."

    private const val PUBLISHED_FIELD =
        "a non-private stored collection property publishes the backing instance. Kotlin's `Set`/`List` are " +
            "read-only views rather than immutable types, so at size two or more a caller down-casts and mutates " +
            "what this type decides with (bean:0009, bean:0030). Make it `private` and expose a copying getter."

    private const val TYPE_MODIFIER =
        "public|internal|private|protected|abstract|open|sealed|final|inner|value|annotation|enum|data|expect|actual|external|fun"

    private const val PROPERTY_MODIFIER =
        "public|internal|private|protected|override|open|final|const|lateinit|abstract|vararg"

    private const val FUN_MODIFIER =
        "public|internal|private|protected|override|open|final|abstract|inline|operator|infix|suspend|tailrec|external"

    private val WHITESPACE = Regex("""\s+""")

    private val TYPE_DECL = Regex("""^[ \t]*((?:(?:$TYPE_MODIFIER)[ \t]+)*)(class|object|interface)[ \t]+(\w+)""")

    private val PRIVATE_CTOR = Regex("""\bprivate[ \t]+constructor\b""")

    private val COMPANION = Regex("""^[ \t]*(?:(?:public|internal|private|protected)[ \t]+)*companion[ \t]+object\b""")

    /** `object : Steps { … }` declares members and has no name for [TYPE_DECL] to find. */
    private val ANONYMOUS = Regex("""\bobject[ \t]*:""")

    private val PROPERTY = Regex("""^[ \t]*((?:(?:$PROPERTY_MODIFIER)[ \t]+)*)va[lr][ \t]+(\w+)\b[ \t]*(.*)$""")

    private val PARAM = Regex("""^[ \t]*(?:(?:$PROPERTY_MODIFIER)[ \t]+)*(?:va[lr][ \t]+)?(\w+)[ \t]*:""")

    private val FUN_DECL = Regex("""^[ \t]*((?:(?:$FUN_MODIFIER)[ \t]+)*)fun[ \t]+(?:<[^>]*>[ \t]*)?(\w+)[ \t]*\(""")

    private val TYPEALIAS =
        Regex("""^[ \t]*(?:(?:public|internal|private)[ \t]+)*typealias[ \t]+(\w+)(?:<[^>]*>)?[ \t]*=[ \t]*(.+)$""")

    private val RETURN = Regex("""\breturn\b[ \t]+([^\n]+)""")

    /** A body-level `constructor(`, which the primary one on the class header line cannot match. */
    private val SECONDARY_CTOR = Regex("""^[ \t]*((?:(?:$FUN_MODIFIER)[ \t]+)*)constructor[ \t]*\(""")

    /** A bare assignment statement, never a declaration: `val x = y` starts with `val`. */
    private val ASSIGN = Regex("""^[ \t]+(?:this\.)?(\w+)[ \t]*=(?!=)[ \t]*(.+)$""")

    private val ROOT = Regex("""^(?:this\.)?([A-Za-z_]\w*)""")

    private val CONTROL_FLOW = Regex("""\b(?:return|if|when|else|try|catch|throw|do|while|for)\b|\?:|;""")

    /** `field.copy()` and nothing else: one receiver, one call, no argument. */
    private val SINGLE_CALL = Regex("""^(?:this\.)?[A-Za-z_]\w*\.[A-Za-z_]\w*\(\)$""")

    /**
     * A Kotlin `List`/`Set`/`Map` is a read-only *view*: `toSet()` over two or more elements
     * returns a `LinkedHashSet` a caller can down-cast and mutate. `Array` is mutable outright,
     * and `Sequence` is single-use and aliases its source.
     */
    private val COLLECTION =
        Regex(
            """\b(?:Mutable)?(?:List|Set|Map|Collection|Iterable|Sequence|Array|ArrayList|""" +
                """HashSet|LinkedHashSet|TreeSet|SortedSet|HashMap|LinkedHashMap|TreeMap|SortedMap|ArrayDeque)\b""",
        )

    /** What an initialiser looks like when the property's type is a collection but is not written down. */
    private val COLLECTION_EXPR =
        Regex(
            """\b(?:mutableListOf|listOf|listOfNotNull|mutableSetOf|setOf|setOfNotNull|mutableMapOf|mapOf|""" +
                """arrayListOf|hashSetOf|linkedSetOf|sortedSetOf|hashMapOf|linkedMapOf|sortedMapOf|arrayOf|""" +
                """emptyList|emptySet|emptyMap|buildList|buildSet|buildMap|ArrayList|HashSet|LinkedHashSet|""" +
                """TreeSet|HashMap|LinkedHashMap|TreeMap|ArrayDeque)\s*[<(]""" +
                """|\.(?:toList|toSet|toMap|toMutableList|toMutableSet|toMutableMap|toTypedArray|toSortedSet|""" +
                """toSortedMap|sorted|sortedBy|sortedWith|reversed|distinct|filter|filterNot|map|flatMap)\s*[({]""",
        )

    /** The forms known to return a fresh instance. Extending this list is a deliberate edit. */
    private val COPY_CALLS =
        listOf(
            "toList()",
            "toSet()",
            "toMap()",
            "toSortedSet()",
            "toSortedMap()",
            "toTypedArray()",
            "toMutableList()",
            "toMutableSet()",
            "toMutableMap()",
            "emptyList()",
            "emptySet()",
            "emptyMap()",
        )
}
