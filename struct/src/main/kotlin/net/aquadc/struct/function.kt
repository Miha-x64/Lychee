@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.struct

@PublishedApi
internal class Getter<STRUCT : Struct<STRUCT>, T>(
        private val struct: STRUCT?,
        private val field: Field<STRUCT, T, *>
) : () -> T, (STRUCT) -> T {

    override fun invoke(): T =
            struct!!.getValue(field)

    override fun invoke(p1: STRUCT): T =
            p1.getValue(field)

}

/**
 * Creates a getter, i. e. a function which returns value of a pre-set [field] of a given [STRUCT].
 */
inline fun <STRUCT : Struct<STRUCT>, T> getterOf(field: Field<STRUCT, T, *>): (STRUCT) -> T =
        Getter(null, field)

/**
 * Creates a getter applied to [this] [STRUCT],
 * i. e. a function which returns a value of a pre-set [field] of a pre-set (struct)[this].
 */
inline fun <STRUCT : Struct<STRUCT>, T> STRUCT.getterOf(field: Field<STRUCT, T, *>): () -> T =
        Getter(this, field)
