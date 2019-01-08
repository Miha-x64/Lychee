@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.struct

import android.support.annotation.RestrictTo


inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> emptyFieldSet(): FieldSet<SCH, F> =
        FieldSet(0L)

inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> F.asFieldSet(): FieldSet<SCH, F> =
        FieldSet(1L shl ordinal.toInt())

inline fun <SCH : Schema<SCH>> SCH.fieldsWhere(predicate: (FieldDef<SCH, *>) -> Boolean): FieldSet<SCH, FieldDef<SCH, *>> {
    var mask = 0L

    var bit = 1L
    val fields = fields
    for (index in fields.indices) {
        if (predicate(fields[index])) {
            mask = mask or bit
        }
        bit = bit shl 1
    }

    return FieldSet(mask)
}


fun <SCH : Schema<SCH>> SCH.allFieldSet(): FieldSet<SCH, FieldDef<SCH, *>> =
        FieldSet((1L shl fields.size) - 1)

fun <SCH : Schema<SCH>> SCH.mutableFieldSet(): FieldSet<SCH, FieldDef.Mutable<SCH, *>> =
        fieldsWhere { it is FieldDef.Mutable } as FieldSet<SCH, FieldDef.Mutable<SCH, *>>

fun <SCH : Schema<SCH>> SCH.immutableFieldSet(): FieldSet<SCH, FieldDef.Mutable<SCH, *>> =
        fieldsWhere { it is FieldDef.Immutable } as FieldSet<SCH, FieldDef.Mutable<SCH, *>>


inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> F.plus(other: F): FieldSet<SCH, F> =
        FieldSet((1L shl ordinal.toInt()) or (1L shl other.ordinal.toInt()))

inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>, G : F, H : F> FieldSet<SCH, G>.plus(more: H): FieldSet<SCH, F> =
        FieldSet(bitmask or (1L shl more.ordinal.toInt()))

inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: FieldDef<SCH, *>): Boolean =
        (bitmask and (1L shl field.ordinal.toInt())) != 0L

inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> SCH.forEach(set: FieldSet<SCH, F>, func: (F) -> Unit) {
    val fields = fields
    var ord = 0
    var mask = set.bitmask
    while (mask != 0L) {
        if ((mask and 1L) == 1L) {
            func(fields[ord] as F)
        }

        mask = mask ushr 1
        ord++
    }
}

/**
 * Similar to [Set]<FieldDef<SCH, FLD>>.
 */
inline class FieldSet<SCH : Schema<SCH>, FLD : FieldDef<SCH, *>>
/*internal*/ @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @Deprecated("Don't. Touch. This. Directly.") constructor(
        @PublishedApi internal val bitmask: Long
)
