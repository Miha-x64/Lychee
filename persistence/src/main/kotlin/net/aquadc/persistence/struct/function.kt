@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.struct

@PublishedApi
internal class Getter<SCH : Schema<SCH>, T>(
        private val struct: Struct<SCH>,
        private val field: FieldDef<SCH, T, *>
) : () -> T {

    override fun invoke(): T =
            struct[field]

}

/**
 * Creates a getter applied to [this] [SCH],
 * i. e. a function which returns a value of a pre-set [field] of a pre-set (struct)[this].
 */
inline fun <SCH : Schema<SCH>, T> Struct<SCH>.getterOf(field: FieldDef<SCH, T, *>): () -> T =
        Getter(this, field)
