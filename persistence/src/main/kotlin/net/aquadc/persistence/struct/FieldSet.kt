@file:Suppress("NOTHING_TO_INLINE", "INAPPLICABLE_JVM_NAME")
package net.aquadc.persistence.struct

import androidx.annotation.RestrictTo


/**
 * Returns an empty set of fields.
 * Useful when you need a `var` of type `FieldSet<SCH, FieldDef<SCH, *, *>>` for filling it with `+=` later.
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> emptyFieldSet(dummy: Nothing? = null): FieldSet<SCH, F> =
        FieldSet(0L)

/**
 * Returns an empty set of fields.
 * Useful for passing anywhere because it is a subtype of
 * both `FieldSet<SCH, MutableField<SCH, *, *>>` abd `FieldSet<SCH, ImmutableField<SCH, *, *>>`.
 * [SCH] of output type is still not [Nothing] because it is safer to leave it invariant everywhere.
 */
inline fun <SCH : Schema<SCH>> emptyFieldSet(): FieldSet<SCH, Nothing> =
        FieldSet(0L)

/**
 * Returns a set of a single field.
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> F.asFieldSet(): FieldSet<SCH, F> =
        FieldSet(1L shl ordinal.toInt())

/**
 * Filters fields of [this] [Schema] with a [predicate] returning a set of suitable ones.
 */
inline fun <SCH : Schema<SCH>> SCH.fieldsWhere(predicate: (FieldDef<SCH, *, *>) -> Boolean): FieldSet<SCH, FieldDef<SCH, *, *>> {
    var mask = 0L

    var bit = 1L
    forEach(allFieldSet) { field ->
        if (predicate(field)) {
            mask = mask or bit
        }
        bit = bit shl 1
    }

    return FieldSet(mask)
}


/**
 * Returns the a set representing a union of [this] and [other] [FieldDef]s.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> F.plus(other: F): FieldSet<SCH, F> =
        FieldSet((1L shl ordinal.toInt()) or (1L shl other.ordinal.toInt()))

/**
 * Returns a set representing a union of [this] set and [other] field.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : F, H : F> FieldSet<SCH, G>.plus(other: H): FieldSet<SCH, F> =
        FieldSet(bitSet or (1L shl other.ordinal.toInt()))

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, FieldDef<SCH, *, *>>
        .intersect(other: FieldSet<SCH, FieldDef<SCH, *, *>>): FieldSet<SCH, FieldDef<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyMut") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, FieldDef<SCH, *, *>>
        .intersect(other: FieldSet<SCH, MutableField<SCH, *, *>>): FieldSet<SCH, MutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyImm") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, FieldDef<SCH, *, *>>.intersect(
        other: FieldSet<SCH, ImmutableField<SCH, *, *>>
): FieldSet<SCH, ImmutableField<SCH, *, *>> = FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyNuff") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, FieldDef<SCH, *, *>>
        .intersect(other: FieldSet<SCH, Nothing>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectMutAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, FieldDef<SCH, *, *>>): FieldSet<SCH, MutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectMutMut") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, MutableField<SCH, *, *>>): FieldSet<SCH, MutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectMutImm") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, ImmutableField<SCH, *, *>>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectMutNuff") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, Nothing>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectImmAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, ImmutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, FieldDef<SCH, *, *>>): FieldSet<SCH, ImmutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectImmMut") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, ImmutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, MutableField<SCH, *, *>>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectImmImm") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, ImmutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, ImmutableField<SCH, *, *>>): FieldSet<SCH, ImmutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectImmNuff") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, ImmutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, Nothing>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectNuffAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, Nothing>
        .intersect(other: FieldSet<SCH, FieldDef<SCH, *, *>>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectNuffMut") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, Nothing>
        .intersect(other: FieldSet<SCH, MutableField<SCH, *, *>>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectNuffImm") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, Nothing>
        .intersect(other: FieldSet<SCH, ImmutableField<SCH, *, *>>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectNuffNuff") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, Nothing>
        .intersect(other: FieldSet<SCH, Nothing>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/**
 * Returns a set equal to [this] without [other] field.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : F, H : F> FieldSet<SCH, G>.minus(other: H): FieldSet<SCH, F> =
        FieldSet(bitSet and (1L shl other.ordinal.toInt()).inv())

/**
 * Returns a set equal to [this] without fields from [other] set.
 */
inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> FieldSet<SCH, F>.minus(other: FieldSet<SCH, F>): FieldSet<SCH, F> =
        FieldSet(bitSet and other.bitSet.inv())
        // this:          0000000000000000011111111111111111
        // other:         0000000000000010101010101010101010
        // ~other:        1111111111111101010101010101010101
        // this & ~other: 0000000000000000010101010101010101

/**
 * Checks whether [this] set contains that [field].
 */
inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: FieldDef<SCH, *, *>): Boolean =
        (bitSet and (1L shl field.ordinal.toInt())) != 0L

/**
 * Number of fields in this set, ∈ [1, 64]
 */
val FieldSet<*, *>.size: Int
    get() = java.lang.Long.bitCount(bitSet)

/**
 * Whether this set is empty.
 */
val FieldSet<*, *>.isEmpty: Boolean
    get() = bitSet == 0L

/**
 * Returns index of [field] in this set.
 * Memory layouts of partial structs are built on top of this.
 */
inline fun <SCH : Schema<SCH>> FieldSet<SCH, *>.indexOf(field: FieldDef<SCH, *, *>): Int =
    this.bitSet.indexOf(field.ordinal.toInt())

@PublishedApi internal fun Long.indexOf(ordinal: Int): Int {
    val one = 1L shl ordinal
    return if ((this and one) == 0L) -1 else java.lang.Long.bitCount(this and (one - 1L))
} //                          fun lowerOnes(r: Int): Long = ((1L shl r) - 1L) ^^^^^^^^^^

fun <SCH : Schema<SCH>> SCH.toString(fields: FldSet<SCH>): String =
        if (fields.isEmpty) "[]"
        else buildString {
            append('[')
            forEach<SCH, FieldDef<SCH, *, *>>(fields) { field ->
                append(field.name).append(", ")
            }
            setLength(length - 2)
            append(']')
        }

/**
 * Invokes [func] on each element of the [set].
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> SCH.forEach(set: FieldSet<SCH, F>, func: SCH.(F) -> Unit) {
    set.bitSet.forEachBit { _, bitIdx ->
        func(this, fieldAt(bitIdx) as F)
    }
}

/**
 * Asserts [FieldSet.size] is 1 and returns this [FieldDef].
 */
fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> SCH.single(set: FieldSet<SCH, F>): F {
    val bits = set.bitSet
    check(java.lang.Long.bitCount(bits) == 1)
    return fieldAt(java.lang.Long.numberOfTrailingZeros(bits)) as F
}

/**
 * Invokes [func] on each element of the [set].
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> SCH.forEachIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> Unit) {
    set.bitSet.forEachBit { setBitIdx, bitIdx ->
        func(setBitIdx, fieldAt(bitIdx) as F)
    }
}

/**
 * Invokes [func] on each element of the [set].
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, reified R> SCH.mapIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> R): Array<R> {
    val out = arrayOfNulls<R>(set.size)
    set.bitSet.forEachBit { setBitIdx, bitIdx ->
        out[setBitIdx] = func(setBitIdx, fieldAt(bitIdx) as F)
    }
    return out as Array<R>
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun Long.forEachBit(func: (setBitIdx: Int, bitIdx: Int) -> Unit) {
    var idx = 0
    var ord = 0
    var bits = this
    while (bits != 0L) {
        if ((bits and 1L) == 1L) {
            func(idx++, ord)
        }

        bits = bits ushr 1
        ord++
    }
}

typealias FldSet<SCH> = FieldSet<SCH, FieldDef<SCH, *, *>>

/**
 * Represents an allocation-less [Set]<FieldDef<SCH, FLD>>.
 */
inline class FieldSet<SCH : Schema<SCH>, out FLD : FieldDef<SCH, *, *>>
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
        /**
         * A value with bits set according to fields present.
         * A set considered to be 'containing field f' when bitmask has a bit `1 << f.ordinal` set.
         * This value is a part of serialization ABI and has stable format
         * as it is written to and read from persistent storages.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val bitSet: Long
)
