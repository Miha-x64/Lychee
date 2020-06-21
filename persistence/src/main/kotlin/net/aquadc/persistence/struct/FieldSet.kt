@file:Suppress("NOTHING_TO_INLINE", "INAPPLICABLE_JVM_NAME")
package net.aquadc.persistence.struct

import androidx.annotation.RestrictTo
import net.aquadc.persistence.type.DataType


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
    FieldSet(1L shl ordinal)
inline fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>> F.asFieldSet(): FieldSet<SCH, F> =
    FieldSet(1L shl ordinal)
inline fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>> F.asFieldSet(): FieldSet<SCH, F> =
    FieldSet(1L shl ordinal)

/**
 * Filters fields of [this] [Schema] with a [predicate] returning a set of suitable ones.
 */
inline fun <SCH : Schema<SCH>> SCH.fieldsWhere(predicate: (FieldDef<SCH, *, *>) -> Boolean): FldSet<SCH> {
    var mask = 0L

    var bit = 1L
    val set: FieldSet<SCH, FieldDef<SCH, *, *>> = allFieldSet
    forEach(set) { field: FieldDef<SCH, *, *> ->
        if (predicate(field)) {
            mask = mask or bit
        }
        bit = bit shl 1
    }

    return FieldSet(mask)
}


/**
 * Returns a set with all fields of [this] [Schema].
 */
@Deprecated("use member property instead", ReplaceWith("this.allFieldSet"), DeprecationLevel.ERROR)
inline fun <SCH : Schema<SCH>> SCH.allFieldSet(): Nothing =
    throw AssertionError()

/**
 * Returns a set of all [MutableField]s of [this] [Schema].
 */
@Deprecated("use member property instead", ReplaceWith("this.mutableFieldSet"), DeprecationLevel.ERROR)
inline fun <SCH : Schema<SCH>> SCH.mutableFieldSet(): Nothing =
    throw AssertionError()

/**
 * Returns a set of all [ImmutableField]s of [this] [Schema].
 */
@Deprecated("use member property instead", ReplaceWith("this.immutableFieldSet"), DeprecationLevel.ERROR)
inline fun <SCH : Schema<SCH>> SCH.immutableFieldSet(): Nothing =
    throw AssertionError()


/**
 * Returns the a set representing a union of [this] and [other] [FieldDef]s.
 */
inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>> F.plus(other: F): FieldSet<SCH, F> =
    FieldSet((1L shl ordinal) or (1L shl other.ordinal))
inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>> F.plus(other: F): FieldSet<SCH, F> =
    FieldSet((1L shl ordinal) or (1L shl other.ordinal))
inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, G : ImmutableField<SCH, *, *>> F.plus(other: G): FieldSet<SCH, F> =
    FieldSet((1L shl ordinal) or (1L shl other.ordinal))
inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, G : MutableField<SCH, *, *>> F.plus(other: G): FieldSet<SCH, F> =
    FieldSet((1L shl ordinal) or (1L shl other.ordinal))

/**
 * Returns a set representing a union of [this] set and [other] field.
 */
@JvmName("fs+mf") inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))
@JvmName("fs+if") inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))
@JvmName("ms+mf") inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, MutableField<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))
@JvmName("ms+if") inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))
@JvmName("is+mf") inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))
@JvmName("is+if") inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.plus(other: G): FieldSet<SCH, ImmutableField<SCH, *, *>> =
    FieldSet(bitSet or (1L shl other.ordinal))

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, *>
        .intersect(other: FieldSet<SCH, *>): FieldSet<SCH, FieldDef<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyMut") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, *>
        .intersect(other: FieldSet<SCH, MutableField<SCH, *, *>>): FieldSet<SCH, MutableField<SCH, *, *>> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyImm") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, *>.intersect(
        other: FieldSet<SCH, ImmutableField<SCH, *, *>>
): FieldSet<SCH, ImmutableField<SCH, *, *>> = FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectAnyNuff") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, *>
        .intersect(other: FieldSet<SCH, Nothing>): FieldSet<SCH, Nothing> =
        FieldSet(this.bitSet and other.bitSet)

/** Returns a set representing intersection of [this] set and the [other] set. */
@JvmName("intersectMutAny") inline infix fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>
        .intersect(other: FieldSet<SCH, *>): FieldSet<SCH, MutableField<SCH, *, *>> =
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
        .intersect(other: FieldSet<SCH, *>): FieldSet<SCH, ImmutableField<SCH, *, *>> =
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
        .intersect(other: FieldSet<SCH, *>): FieldSet<SCH, Nothing> =
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
@JvmName("fs-mf") inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())
@JvmName("fs-if") inline operator fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, FieldDef<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())
@JvmName("ms-mf") inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, MutableField<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())
@Deprecated("set of mutable fields might not contain any immutable fields", ReplaceWith("this"))
@JvmName("ms-if") inline operator fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, MutableField<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())
@Deprecated("set of immutable fields might not contain any mutable fields", ReplaceWith("this"))
@JvmName("is-mf") inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, G : MutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, ImmutableField<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())
@JvmName("is-if") inline operator fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, G : ImmutableField<SCH, *, *>>
    FieldSet<SCH, F>.minus(other: G): FieldSet<SCH, ImmutableField<SCH, *, *>> =
    FieldSet(bitSet and (1L shl other.ordinal).inv())

/**
 * Returns a set equal to [this] without fields from [other] set.
 */
inline operator fun <SCH : Schema<SCH>, F : NamedLens<SCH, *, *, *, *>> FieldSet<SCH, F>.minus(other: FieldSet<SCH, F>): FieldSet<SCH, F> =
        FieldSet(bitSet and other.bitSet.inv())
        // this:          0000000000000000011111111111111111
        // other:         0000000000000010101010101010101010
        // ~other:        1111111111111101010101010101010101
        // this & ~other: 0000000000000000010101010101010101

/**
 * Checks whether [this] set contains that [field].
 */
inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: FieldDef<SCH, *, *>): Boolean =
        (bitSet and (1L shl field.ordinal)) != 0L
/**
 * Checks whether [this] set contains that [field].
 */
inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: MutableField<SCH, *, *>): Boolean =
        (bitSet and (1L shl field.ordinal)) != 0L
/**
 * Checks whether [this] set contains that [field].
 */
inline operator fun <SCH : Schema<SCH>> FieldSet<SCH, *>.contains(field: ImmutableField<SCH, *, *>): Boolean =
        (bitSet and (1L shl field.ordinal)) != 0L

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
inline fun <SCH : Schema<SCH>> FieldSet<SCH, *>.indexOf(field: MutableField<SCH, *, *>): Int =
    this.bitSet.indexOf(field.ordinal)
inline fun <SCH : Schema<SCH>> FieldSet<SCH, *>.indexOf(field: ImmutableField<SCH, *, *>): Int =
    this.bitSet.indexOf(field.ordinal)

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
    var ord = 0
    var mask = set.bitSet
    while (mask != 0L) {
        if ((mask and 1L) == 1L) {
            @Suppress("UNCHECKED_CAST")
            func(this, fieldAt(ord) as F)
        }

        mask = mask ushr 1
        ord++
    }
}
@JvmName("forEachM")
inline fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>> SCH.forEach_(set: FieldSet<SCH, F>, func: SCH.(F) -> Unit): Unit =
    forEach(set.upcast()) { f: FieldDef<SCH, *, *> -> func(MutableField<SCH, Any?, DataType<Any?>>(f.value) as F) }
@JvmName("forEachI")
inline fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>> SCH.forEach_(set: FieldSet<SCH, F>, func: SCH.(F) -> Unit): Unit =
    forEach(set.upcast()) { f: FieldDef<SCH, *, *> -> func(ImmutableField<SCH, Any?, DataType<Any?>>(f.value) as F) }

/**
 * Asserts [FieldSet.size] is 1 and returns this [FieldDef].
 */
fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> SCH.single(set: FieldSet<SCH, F>): F {
    var ord = 0
    var mask = set.bitSet
    while ((mask and 1L) == 0L) {
        mask = mask ushr 1
        ord++
    }
    check(mask == 1L)
    return fieldAt(ord) as F
}
@JvmName("singleM") inline fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>> SCH.single_(set: FieldSet<SCH, F>): F =
    MutableField<SCH, Any?, DataType<Any?>>(single(set as FieldSet<SCH, FieldDef<SCH, *, *>>).value) as F
@JvmName("singleI") inline fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>> SCH.single_(set: FieldSet<SCH, F>): F =
    MutableField<SCH, Any?, DataType<Any?>>(single(set as FieldSet<SCH, FieldDef<SCH, *, *>>).value) as F

/**
 * Invokes [func] on each element of the [set].
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>> SCH.forEachIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> Unit) {
    var idx = 0
    var ord = 0
    var mask = set.bitSet
    while (mask != 0L) {
        if ((mask and 1L) == 1L) {
            func(idx++, fieldAt(ord) as F)
        }

        mask = mask ushr 1
        ord++
    }
}
@JvmName("forEachIdxM")
inline fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>> SCH.forEachIndexed_(set: FieldSet<SCH, F>, func: (Int, F) -> Unit): Unit =
    forEachIndexed(set.upcast()) { index, field -> func(index, MutableField<SCH, Any?, DataType<Any?>>(field.value) as F) }
@JvmName("forEachIdxI")
inline fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>> SCH.forEachIndexed_(set: FieldSet<SCH, F>, func: (Int, F) -> Unit): Unit =
    forEachIndexed(set.upcast()) { index, field -> func(index, ImmutableField<SCH, Any?, DataType<Any?>>(field.value) as F) }

/**
 * Invokes [func] on each element of the [set].
 */
inline fun <SCH : Schema<SCH>, F : FieldDef<SCH, *, *>, reified R> SCH.mapIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> R): Array<R> {
    val out = arrayOfNulls<R>(set.size)
    var idx = 0
    var ord = 0
    var mask = set.bitSet
    while (mask != 0L) {
        if ((mask and 1L) == 1L) {
            out[idx] = func(idx, fieldAt(ord) as F)
            idx += 1
        }

        mask = mask ushr 1
        ord++
    }
    return out as Array<R>
}
@JvmName("mapIndexedM")
inline fun <SCH : Schema<SCH>, F : MutableField<SCH, *, *>, reified R> SCH.mapIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> R): Array<R> =
    mapIndexed(set.upcast()) { index, field -> func(index, MutableField<SCH, Any?, DataType<Any?>>(field.value) as F) }
@JvmName("mapIndexedI")
inline fun <SCH : Schema<SCH>, F : ImmutableField<SCH, *, *>, reified R> SCH.mapIndexed(set: FieldSet<SCH, F>, func: (Int, F) -> R): Array<R> =
    mapIndexed(set.upcast()) { index, field -> func(index, ImmutableField<SCH, Any?, DataType<Any?>>(field.value) as F) }

typealias FldSet<SCH> = FieldSet<SCH, FieldDef<SCH, *, *>>

@JvmSynthetic @JvmName("dwngrdSetOfMut")
inline fun <SCH : Schema<SCH>> FieldSet<SCH, MutableField<SCH, *, *>>.upcast(): FldSet<SCH> =
    FieldSet(this.bitSet)

@JvmSynthetic @JvmName("dwngrdSetOfImm")
inline fun <SCH : Schema<SCH>> FieldSet<SCH, ImmutableField<SCH, *, *>>.upcast(): FldSet<SCH> =
    FieldSet(this.bitSet)

/**
 * Represents an allocation-less [Set]<FieldDef<SCH, FLD>>.
 */
inline class FieldSet<SCH : Schema<SCH>, out FLD : NamedLens<SCH, *, *, *, *>>
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
        /**
         * A value with bits set according to fields present.
         * A set considered to be 'containing field f' when bitmask has a bit `1 << f.ordinal` set.
         * This value is a part of serialization ABI and has stable format
         * as it is written to and read from persistent storages.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val bitSet: Long
)
