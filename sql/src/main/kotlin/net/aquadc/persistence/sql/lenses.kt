@file:[JvmName("Lenses") Suppress("NOTHING_TO_INLINE")]
package net.aquadc.persistence.sql

import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.reallyEqual
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable


/**
 * String concatenation factory to build names for nested lenses.
 */
interface NamingConvention {
    fun concatNames(outer: CharSequence, nested: CharSequence): CharSequence
}


@JvmName("structLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.NotNull.Partial<Struct<US>, US>,
        V, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, Struct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V, VD> =
    Telescope(null, this, nested)

@JvmName("partialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.NotNull.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType.NotNull<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, DataType.Nullable<V, VD>> =
    Telescope(null, this, nested)

@JvmName("partialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.NotNull.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, VD> =
    Telescope(null, this, nested)

@JvmName("nullablePartialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.NotNull.Partial<UR, US>>,
        V : Any, VD : DataType.NotNull<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, DataType.Nullable<V, VD>> =
    Telescope(null, this, nested)

@JvmName("nullablePartialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.NotNull.Partial<UR, US>>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, VD> =
    Telescope(null, this, nested)


/* This overload makes no sense: 'Schema' is the only 'magic' Partial with all fields set, and T is obviously not 'Schema'
@JvmName("structLens") inline operator fun <
        TS : Schema<TS>, T,
        US : Schema<US>,
        V, VD : DataType<V>
        >
        StoredLens<TS, T, out DataType.Partial<T, US>>.rem(
        nested: StoredLens<US, V, VD>
): StoredLens<TS, V, VD> =
    Telescope(null, nested.type, this, nested)*/

@JvmName("partialToNonNullableLens") inline operator fun <
        TS : Schema<TS>, T,
        US : Schema<US>,
        V : Any, VD : DataType.NotNull<V>
        >
        StoredLens<TS, T, out DataType.NotNull.Partial<T, US>>.rem(
        nested: StoredLens<US, V, VD>
): StoredLens<TS, V?, DataType.Nullable<V, VD>> =
    Telescope(null, this, nested)

@JvmName("partialToNullableLens") inline operator fun <
        TS : Schema<TS>, T,
        US : Schema<US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        StoredLens<TS, T, out DataType.NotNull.Partial<T, US>>.rem(
        nested: StoredLens<US, V?, VD>
): StoredLens<TS, V?, VD> =
    Telescope(null, this, nested)

@JvmName("nullablePartialToNonNullableLens") inline operator fun <
        TS : Schema<TS>, T : Any,
        US : Schema<US>,
        V : Any, VD : DataType.NotNull<V>
        >
        StoredLens<TS, T?, out DataType.Nullable<T, out DataType.NotNull.Partial<T, US>>>.rem(
        nested: StoredLens<US, V, VD>
): StoredLens<TS, V?, DataType.Nullable<V, VD>> =
    Telescope(null, this, nested)

@JvmName("nullablePartialToNullableLens") inline operator fun <
        TS : Schema<TS>, T : Any,
        US : Schema<US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        StoredLens<TS, T?, out DataType.Nullable<T, out DataType.NotNull.Partial<T, US>>>.rem(
        nested: StoredLens<US, V?, VD>
): StoredLens<TS, V?, VD> =
    Telescope(null, this, nested)


@Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
internal fun <SCH : Schema<SCH>, PRT : PartialStruct<SCH>, STR : Struct<SCH>> NamingConvention?.concatErased(
        thisSchema: SCH, thatSchema: Schema<*>,
        dis: StoredLens<SCH, *, *>, that: StoredLens<*, *, *>
): Lens<SCH, PRT, STR, *, *> {

    val name = if (this !== null && dis is Named<*> && that is Named<*>)
        concatNames((dis as Named<SCH>).name(thisSchema), (that as Named<Schema<*>>).name(thatSchema))
    else
        null
    return Telescope<SCH, PRT, STR, NullSchema, PartialStruct<NullSchema>, Any?, DataType<Any?>>(
        name,
        dis as Lens<SCH, PRT, STR, out PartialStruct<NullSchema>?, *>,
        that as Lens<NullSchema, PartialStruct<NullSchema>, Struct<NullSchema>, out Any?, *>
    )
}

/**
 * Generates names concatenated with snake_case
 */
@JvmField val SnakeCase: NamingConvention = ConcatConvention('_')

/**
 * Generates names concatenated with camelCase
 */
@JvmField val CamelCase: NamingConvention = object : NamingConvention {

    override fun concatNames(outer: CharSequence, nested: CharSequence): String = buildString(outer.length + nested.length) {
        append(outer)
        if (nested.isNotEmpty()) {
            val firstCodePoint = Character.codePointAt(nested, 0)
            appendCodePoint(Character.toTitleCase(firstCodePoint))
            append(nested, Character.charCount(firstCodePoint), nested.length)
        }
    }

}

/**
 * Generates names concatenated with a dot.
 */
@JvmField val NestingCase: NamingConvention = ConcatConvention('.')

private class ConcatConvention(private val delimiter: Char) : NamingConvention {
    override fun concatNames(outer: CharSequence, nested: CharSequence): String =
            buildString(outer.length + 1 + nested.length) {
                append(outer).append(delimiter).append(nested)
            }
    // I could create a pre-sized char[] and use String#getChars(dest) but
    // those Strings are identifiers, i. e. they are stored as ASCII in 99.9+% of cases.
    // Could create special version for Java 9+ but this does not seem to be a hot place.
}

@Suppress("UNCHECKED_CAST")
@PublishedApi internal class
Telescope<TS : Schema<TS>, TR : PartialStruct<TS>, S : Struct<TS>, US : Schema<US>, T, U, UD : DataType<U>>
@PublishedApi internal constructor(
        name: CharSequence?,
        private val outer: StoredLens<TS, out T?, *>,
        private val nested: StoredLens<US, out U, *>
) : BaseLens<TS, TR, S, U, UD>(name) {

    @PublishedApi internal constructor(
            name: CharSequence?,
            outer: Lens<TS, TR, S, out T?, *>,
            nested: Lens<US, PartialStruct<US>, Struct<US>, out U, *>
    ) : this(name, outer as StoredLens<TS, out T?, *>, nested as StoredLens<US, out U, *>)

    // schema |> outer.type |> unwrap |> nested.type |> propagateNullability
    override fun type(mySchema: TS): UD {
        @Suppress("UPPER_BOUND_VIOLATED") // some mischief here
        nested as StoredLens<Schema<*>, Any?, DataType<Any?>>

        val nestedType = outer.type(mySchema)
        if (nestedType is Schema<*>) return nested.type(nestedType) as UD

        val nestedSchema = ((
                if (nestedType is DataType.Nullable<*, *>) nestedType.actualType else nestedType /*either Schema or Partial*/
            ) as DataType.NotNull.Partial<*, *>).schema as TS

        val outerType = nested.type(nestedSchema)
        return (
            if (nestedType is Schema<*> || outerType is DataType.Nullable<*, *>) outerType
            //  ^^^^^ leave as is ^^^^^ or ^^^^^^ it is already nullable ^^^^^^
            else nullable(outerType as DataType.NotNull<Any>)
        ) as UD
    }

    override fun hasValue(struct: TR): Boolean =
            (outer as Lens<TS, TR, S, out T?, *>).hasValue(struct) &&
                    (nested as Lens<US, PartialStruct<US>, Struct<US>, out U, *>).hasValue(outer.ofPartial(struct) as PartialStruct<US>)

    override fun ofPartial(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") struct: TR): U? =
            if ((outer as Lens<TS, TR, S, out T?, *>).hasValue(struct))
                (nested as Lens<US, PartialStruct<US>, Struct<US>, out U, *>).ofPartial(
                        outer.ofPartial(struct) as PartialStruct<US>
                )
            else null

    override val size: Int
        get() = outer.size + nested.size

    override fun get(index: Int): NamedLens<*, *, *, *, *> {
        val outerSize = outer.size
        return if (index < outerSize) outer[index] else nested[index - outerSize]
    }

    // copy-paste of orderedHashCode + orderedEquals from AbstractList
    // note: this intentionally ignores [name] value

    override fun hashCode(): Int {
        var hashCode = 1
        for (i in 0 until size) {
            hashCode = 31 * hashCode + (this[i].hashCode())
        }
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Lens<*, *, *, *, *> || other.size != size) return false

        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun toString(): String = buildString {
        append(outer)
//      if (outer.type !is Schema<*>) append('?') — impossible without knowing type
        append('.').append(nested)
    }

}

// endregion telescope impl

// region special lenses

internal class PkLens<S : Schema<S>, ID : IdBound> constructor(
    private val table: Table<S, ID>,
    private val type: DataType.NotNull.Simple<ID>
) : BaseLens<S, Record<S, ID>, Record<S, ID>, ID, DataType.NotNull.Simple<ID>>(
    table.idColName
) {

    override fun hasValue(struct: Record<S, ID>): Boolean =
            true

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun type(mySchema: S): DataType.NotNull.Simple<ID> =
        type

    override fun hashCode(): Int =
        -1 // we're minus first field ;)

    override fun equals(other: Any?): Boolean =
            other is PkLens<*, *> && other.table === table

    override fun toString(): String =
        "${table.name}.${table.schema.run { name }} (PRIMARY KEY)"

    override fun ofPartial(@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") record: Record<S, ID>): ID =
            record.primaryKey

}

internal class FieldSetLens<S : Schema<S>>(
        name: CharSequence
) : BaseLens<S, PartialStruct<S>, Struct<S>, Long, DataType.NotNull.Simple<Long>>(name) {

    override fun hasValue(struct: PartialStruct<S>): Boolean =
            true

    override fun ofPartial(partial: PartialStruct<S>): Long? =
            partial.fields.bitSet

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun type(mySchema: S): DataType.NotNull.Simple<Long> =
        i64

    override fun hashCode(): Int =
        name!!.realHashCode()

    override fun equals(other: Any?): Boolean =
        other is FieldSetLens<*> && reallyEqual(name!!, other.name!!)

    override fun toString(): String =
        "${name!!} (field set)"

}

// ugly class for minimizing method count / vtable size
internal abstract class BaseLens<SCH : Schema<SCH>, PRT : PartialStruct<SCH>, STR : Struct<SCH>, T, DT : DataType<T>>(
    @JvmField protected val name: CharSequence?
) : NamedLens<SCH, PRT, STR, T, DT> {

    @Suppress("UNCHECKED_CAST")
    final override fun invoke(struct: STR): T =
        ofPartial(struct as PRT) as T

    override fun name(mySchema: SCH): CharSequence =
        name!!

    override val size: Int
        get() = 1 // true for FieldSetLens and PkLens, but overridden in Telescope

}

// endregion special lenses
