@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.struct

@PublishedApi
internal class Getter<DEF : StructDef<DEF>, T>(
        private val struct: Struct<DEF>?,
        private val field: FieldDef<DEF, T>
) : () -> T, (Struct<DEF>) -> T {

    override fun invoke(): T =
            struct!!.getValue(field)

    override fun invoke(p1: Struct<DEF>): T =
            p1.getValue(field)

}

/**
 * Creates a getter, i. e. a function which returns value of a pre-set [field] of a given [DEF].
 */
inline fun <DEF : StructDef<DEF>, T> getterOf(field: FieldDef<DEF, T>): (Struct<DEF>) -> T =
        Getter(null, field)

/**
 * Creates a getter applied to [this] [DEF],
 * i. e. a function which returns a value of a pre-set [field] of a pre-set (struct)[this].
 */
inline fun <DEF : StructDef<DEF>, T> Struct<DEF>.getterOf(field: FieldDef<DEF, T>): () -> T =
        Getter(this, field)
