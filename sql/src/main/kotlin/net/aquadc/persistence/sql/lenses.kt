@file:[JvmName("Lenses") Suppress("NOTHING_TO_INLINE")]
package net.aquadc.persistence.sql

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
    fun concatNames(outer: CharSequence, nested: CharSequence): String
}


@JvmName("structLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<Struct<US>, US>,
        V, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, Struct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V, VD> =
        Telescope(null, nested.type, this, nested)

@JvmName("partialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.type), this, nested)

@JvmName("partialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UD : DataType.Partial<PartialStruct<US>, US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, PartialStruct<US>, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, VD> =
        Telescope(null, nested.type, this, nested)

@JvmName("nullablePartialToNonNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.Partial<UR, US>>,
        V : Any, VD : DataType<V>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.type), this, nested)

@JvmName("nullablePartialToNullableLens") inline operator fun <
        TS : Schema<TS>,
        US : Schema<US>, UR : PartialStruct<US>, UD : DataType.Nullable<UR, out DataType.Partial<UR, US>>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        Lens<TS, PartialStruct<TS>, Struct<TS>, UR?, UD>.div(
        nested: Lens<US, PartialStruct<US>, Struct<US>, V?, VD>
): Lens<TS, PartialStruct<TS>, Struct<TS>, V?, VD> =
        Telescope(null, nested.type, this, nested)


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
        V : Any, VD : DataType<V>
        >
        StoredLens<TS, T, out DataType.Partial<T, US>>.rem(
        nested: StoredLens<US, V, VD>
): StoredLens<TS, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.type), this, nested)

@JvmName("partialToNullableLens") inline operator fun <
        TS : Schema<TS>, T,
        US : Schema<US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        StoredLens<TS, T, out DataType.Partial<T, US>>.rem(
        nested: StoredLens<US, V?, VD>
): StoredLens<TS, V?, VD> =
        Telescope(null, nested.type, this, nested)

@JvmName("nullablePartialToNonNullableLens") inline operator fun <
        TS : Schema<TS>, T : Any,
        US : Schema<US>,
        V : Any, VD : DataType<V>
        >
        StoredLens<TS, T?, out DataType.Nullable<T, out DataType.Partial<T, US>>>.rem(
        nested: StoredLens<US, V, VD>
): StoredLens<TS, V?, DataType.Nullable<V, VD>> =
        Telescope(null, nullable(nested.type), this, nested)

@JvmName("nullablePartialToNullableLens") inline operator fun <
        TS : Schema<TS>, T : Any,
        US : Schema<US>,
        V : Any, VD : DataType.Nullable<V, *>
        >
        StoredLens<TS, T?, out DataType.Nullable<T, out DataType.Partial<T, US>>>.rem(
        nested: StoredLens<US, V?, VD>
): StoredLens<TS, V?, VD> =
        Telescope(null, nested.type, this, nested)


@Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
internal fun <SCH : Schema<SCH>, PRT : PartialStruct<SCH>, STR : Struct<SCH>> NamingConvention?.concatErased(
        thisSchema: SCH, thatSchema: Schema<*>,
        dis: StoredLens<SCH, *, *>, that: StoredLens<*, *, *>
): Lens<SCH, PRT, STR, *, *> =
        Telescope<SCH, PRT, STR, Schema<*>, PartialStruct<Schema<*>>, Any?, DataType<*>>(
                if (this !== null && dis is Named<*> && that is Named<*>)
                    concatNames((dis as Named<SCH>).name(thisSchema), (that as Named<Any?>).name(thatSchema))
                else null,
                dis as Lens<SCH, PRT, STR, out PartialStruct<Schema<*>>?, *>,
                that as Lens<Schema<*>, PartialStruct<Schema<*>>, Struct<Schema<*>>, out Any?, *>
        )

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
            // Note: this capitalizer is intended for use with identifiers which are mostly ASCII.
            // It will also work for most of non-ASCII scripts. However, it won't capitalize letters of
            // some rare scripts, see https://stackoverflow.com/a/57908029/3050249 for fully-featured capitalizer
            append(nested[0].toUpperCase())
            append(nested, 1, nested.length)
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
        name: String?,
        exactType: UD,
        private val outer: StoredLens<TS, out T?, *>,
        private val nested: StoredLens<US, out U, *>
) : BaseLens<TS, TR, S, U, UD>(name, exactType) {

    @PublishedApi internal constructor(
            name: String?,
            exactType: UD,
            outer: Lens<TS, TR, S, out T?, *>,
            nested: Lens<US, PartialStruct<US>, Struct<US>, out U, *>
    ) : this(name, exactType, outer as StoredLens<TS, out T?, *>, nested as StoredLens<US, out U, *>)

    @PublishedApi
    internal constructor(name: String?, outer: Lens<TS, TR, S, out T?, *>, nested: Lens<US, PartialStruct<US>, Struct<US>, out U, *>) : this(
            name,
            Unit.run {
                if (outer.type is Schema<*> || nested.type is DataType.Nullable<*, *>) nested.type
                //  ^^^^^^leave as is^^^^^^ or ^^^^^^^ it is already nullable ^^^^^^^
                else nullable(nested.type as DataType<Any>)
            } as UD,
            outer,
            nested
    )

    override fun hasValue(struct: TR): Boolean =
            (outer as Lens<TS, TR, S, out T?, *>).hasValue(struct) &&
                    (nested as Lens<US, PartialStruct<US>, Struct<US>, out U, *>).hasValue(outer.ofPartial(struct) as PartialStruct<US>)

    override fun ofPartial(struct: TR): U? =
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
        if (outer.type !is Schema<*>) append('?')
        append('.').append(nested)
    }

}

// endregion telescope impl

// region special lenses

internal class PkLens<S : Schema<S>, ID : IdBound> constructor(
        private val table: Table<S, ID>
) : BaseLens<S, Record<S, ID>, Record<S, ID>, ID, DataType.Simple<ID>>(
        table.idColName, table.idColType
) {

    override fun hasValue(struct: Record<S, ID>): Boolean =
            true

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            64 // FieldDef.hashCode() is in [0; 63], be different!

    override fun equals(other: Any?): Boolean = // for tests
            other is PkLens<*, *> && other.table === table

    override fun toString(): String =
            "${table.name}.${name(table.schema)} (PRIMARY KEY)"


    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE") // can't be fixed, overrides both invoke(struct) and invoke(partial)
    override fun ofPartial(record: Record<S, ID>): ID =
            record.primaryKey

}

internal class FieldSetLens<S : Schema<S>>(
        name: String
) : BaseLens<S, PartialStruct<S>,  Struct<S>, Long, DataType.Simple<Long>>(name, i64) {

    override fun hasValue(struct: PartialStruct<S>): Boolean =
            true

    override fun ofPartial(partial: PartialStruct<S>): Long? =
            partial.fields.bitSet

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            if (type is DataType.Nullable<*, *>) 65 else 66 // FieldDef.hashCode() is in [0; 63], PK is 64, be different!

    // tests only

    override fun equals(other: Any?): Boolean =
            other is FieldSetLens<*> && name == other.name && type == other.type

    override fun toString(): String = buildString {
        append(name).append(" (")
        val nullable = type is DataType.Nullable<*, *>
        val actual: DataType<*> = if (type is DataType.Nullable<*, *>) type.actualType else type
        val partial = actual !is Schema<*>
        if (nullable) append("nullability info")
        if (nullable && partial) append(", ")
        if (partial) append("fields set")
        append(')')
    }

}

// ugly class for minimizing method count / vtable size
internal abstract class BaseLens<SCH : Schema<SCH>, PRT : PartialStruct<SCH>, STR : Struct<SCH>, T, DT : DataType<T>>(
        private val _name: CharSequence?,
        final override val type: DT
) : NamedLens<SCH, PRT, STR, T, DT> {

    @Suppress("UNCHECKED_CAST")
    final override fun invoke(struct: STR): T =
            ofPartial(struct as PRT) as T

    final override val name: String
        get() = _name!!.toString() // give this instance as Lens, not NamedLens, when _name is null

    override fun name(mySchema: SCH): CharSequence =
            _name!!

    override fun type(mySchema: SCH): DT =
            type

    override val size: Int
        get() = 1 // true for FieldSetLens and PkLens, but overridden in Telescope

}

// endregion special lenses
