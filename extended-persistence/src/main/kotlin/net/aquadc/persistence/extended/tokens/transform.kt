@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.extended.tokens

import net.aquadc.persistence.tokens.TokenStream
import net.aquadc.persistence.type.DataType

typealias Predicate = (Any?) -> Boolean

/**
 * Outline key-value pairs matching [what]
 * located inside dictionaries at [path]
 * to a nested dictionary with name [destName]
 * and rename keys with [rename].
 * {k: v, ...} -> {destName:{renamedK: v}, ...}
 */
inline fun TokenStream.outline(path: Array<Predicate>, noinline what: Predicate, destName: Any, noinline rename: (Any?) -> Any?): TokenStream =
        OutlineTokens(this, path, what, destName, rename)

/**
 * {victimName:{k: v}, ...} -> {k: v, ...}
 */
inline fun TokenStream.inline(path: Array<Predicate>, noinline isVictim: Predicate, noinline rename: (Any?) -> Any?,
                              noinline merge: (target: MutableMap<Any?, Any?>, key: Any?, value: Any?) -> Unit,
                              buffer: MutableMap<Any?, Any?> = LinkedHashMap()
): TokenStream =
        InlineTokens(this, path, isVictim, rename, merge, buffer)

class MergeStrategy private constructor(
        private val failOnDupe: Boolean
) : (@ParameterName("target") MutableMap<Any?, Any?>, @ParameterName("key") Any?, @ParameterName("value") Any?) -> Unit {

    override fun invoke(target: MutableMap<Any?, Any?>, key: Any?, value: Any?) {
        if (failOnDupe && key in target) throw IllegalArgumentException("duplicate key: '$key' while inlining")
        target[key] = value
    }

    companion object {

        /**
         * Fail when keys from outer and inlined maps collide.
         * Makes sense when you want to preserve all information available in a stream.
         */
        @JvmField val Fail: (MutableMap<Any?, Any?>, key: Any?, value: Any?) -> Unit =
                MergeStrategy(true)

        /**
         * Replace mappings from outer map by ones from the nested map.
         * Makes sense when you know exactly which mappings you are going to use.
         */
        @JvmField val Replace: (MutableMap<Any?, Any?>, key: Any?, value: Any?) -> Unit =
                MergeStrategy(false)
    }

}
