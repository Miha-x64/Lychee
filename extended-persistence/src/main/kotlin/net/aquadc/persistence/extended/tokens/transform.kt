@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.extended.tokens

import net.aquadc.persistence.tokens.TokenStream
import net.aquadc.persistence.type.DataType

typealias Predicate = (Any?) -> Boolean

@Retention(AnnotationRetention.BINARY)
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalTransforms

// region inlining and outlining

/**
 * Outline key-value pairs matching [what]
 * located inside dictionaries at [path]
 * to a nested dictionary with name [destName]
 * and rename keys with [rename].
 * {k: v, ...} -> {destName:{renamedK: v}, ...}
 * @see inline which does the opposite
 */
@ExperimentalTransforms
inline fun TokenStream.outline(path: Array<Predicate>, noinline what: Predicate, destName: Any, noinline rename: (Any?) -> Any?): TokenStream =
        OutlineTokens(this, path, what, destName, rename)

/**
 * Inline key-value pairs from [isVictim] dictionary
 * to the outer dictionary at [path]
 * and rename inlined keys with [rename].
 * {victimName:{k: v}, ...} -> {k: v, ...}
 * @see MergeStrategy for ways of merging mappings into the root
 * @see outline which does the opposite
 */
@ExperimentalTransforms
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

// endregion inlining and outlining

// region dictionary entries

/**
 * {1: x, 2: y, ...} -> [{id: 1, val: x}, {id: 2, val: y}, ...] or
 * {1: x, 2: y, ...} -> [[1, x], [2, y], ...]
 * @see com.google.gson.GsonBuilder.enableComplexMapKeySerialization is the latter one
 * @see associate which does the opposite
 */
@ExperimentalTransforms
inline fun TokenStream.entries(path: Array<Predicate>, nameKey: Any?, valueKey: Any?): TokenStream =
        DissociateTokens(this, path, nameKey, valueKey)

/**
 * [{id: 1, val: x}, {id: 2, val: y}, ...] -> {1: x, 2: y, ...} or
 * [[1, x], [2, y], ...] -> {1: x, 2: y, ...}
 * @see com.google.gson.GsonBuilder.enableComplexMapKeySerialization is the opposite of the latter one
 * @see entries which does the opposite
 */
@ExperimentalTransforms
inline fun TokenStream.associate(path: Array<Predicate>, nameKey: Any?, valueKey: Any?): TokenStream =
        AssociateTokens(this, path, nameKey, valueKey)

/** A hint which makes [entries] easier to find */
@Deprecated(
        "use entries()",
        ReplaceWith("this.entries(path, nameKey, valueKey)", "net.aquadc.persistence.extended.tokens.entries"),
        DeprecationLevel.ERROR
)
fun TokenStream.dissociate(path: Array<Predicate>, nameKey: Any?, valueKey: Any?): TokenStream = throw AssertionError()

/** A hint which makes [entries] easier to find */
@Deprecated(
        "use entries()",
        ReplaceWith("this.entries(path, nameKey, valueKey)", "net.aquadc.persistence.extended.tokens.entries"),
        DeprecationLevel.ERROR
)
fun TokenStream.mappings(path: Array<Predicate>, nameKey: Any?, valueKey: Any?): TokenStream = throw AssertionError()

// endregion dictionary entries
