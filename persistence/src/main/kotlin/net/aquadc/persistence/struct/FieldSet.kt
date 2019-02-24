@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.struct

import android.support.annotation.RestrictTo


/**
 * Returns an empty set of fields.
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> emptyFieldSet(): FieldSet<SCH, F> =
        FieldSet(0L)

/**
 * Returns a set of a single field.
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> F.asFieldSet(): FieldSet<SCH, F> =
        FieldSet(1L shl ordinal.toInt())

/**
 * Filters fields of [this] [Schema] with a [predicate] returning a set of suitable ones.
 */
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


/**
 * Returns a set with all fields of [this] [Schema].
 */
fun <SCH : Schema<SCH>> SCH.allFieldSet(): FieldSet<SCH, FieldDef<SCH, *>> =
        FieldSet(
                fields.size.let { size ->
                    // (1L shl size) - 1   :  1L shl 64  will overflow to 1
                    // -1L ushr (64 - size): -1L ushr 64 will remain -1L
                    // the last one is okay, assuming that zero-field structs are prohibited
                    -1L ushr (64 - size)
                }
        )

/**
 * Returns a set of all [FieldDef.Mutable] fields of [this] [Schema].
 */
fun <SCH : Schema<SCH>> SCH.mutableFieldSet(): FieldSet<SCH, FieldDef.Mutable<SCH, *>> =
        fieldsWhere { it is FieldDef.Mutable } as FieldSet<SCH, FieldDef.Mutable<SCH, *>>

/**
 * Returns a set of all [FieldDef.Immutable] fields of [this] [Schema].
 */
fun <SCH : Schema<SCH>> SCH.immutableFieldSet(): FieldSet<SCH, FieldDef.Mutable<SCH, *>> =
        fieldsWhere { it is FieldDef.Immutable } as FieldSet<SCH, FieldDef.Mutable<SCH, *>>


/**
 * Returns the a set representing a union of [this] and [other] [FieldDef]s.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>> F.plus(other: F): FieldSet<SCH, F> =
        FieldSet((1L shl ordinal.toInt()) or (1L shl other.ordinal.toInt()))

/**
 * Returns a set representing a union of [this] set and [other] field.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *>, G : F, H : F> FieldSet<SCH, G>.plus(other: H): FieldSet<SCH, F> =
        FieldSet(bitmask or (1L shl other.ordinal.toInt()))

/**
 * Checks whether [this] set contains that [field].
 */
inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: FieldDef<SCH, *>): Boolean =
        (bitmask and (1L shl field.ordinal.toInt())) != 0L

/**
 * Invokes [func] on each element of the [set].
 */
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
 * Represents an allocation-less [Set]<FieldDef<SCH, FLD>>.
 */
inline class FieldSet<SCH : Schema<SCH>, out FLD : FieldDef<SCH, *>>
/*internal*/ @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @Deprecated("Don't. Touch. This. Directly.") constructor(
        @PublishedApi internal val bitmask: Long
)
