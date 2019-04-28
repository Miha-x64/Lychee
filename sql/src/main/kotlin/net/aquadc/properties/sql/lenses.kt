package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullable


/**
 * A namespace where [div] is overloaded for creating new named lenses.
 */
abstract class NamingConvention {

    abstract fun concatNames(outer: String, nested: String): String

    @JvmName("0")
    operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U> NamedLens<TS, TR, T?>.div(
            nested: NamedLens<US, T, U>
    ): NamedLens<TS, TR, U?> =
            Telescope0(concatNames(this.name, nested.name), this, nested)

    @JvmName("1")
    operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U> NamedLens<TS, TR, T>.div(
            nested: NamedLens<US, T, U>
    ): NamedLens<TS, TR, U> =
            Telescope1(concatNames(this.name, nested.name), this, nested)

}

@JvmName("0")
operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U> Lens<TS, TR, T?>.div(
        nested: Lens<US, T, U>
): Lens<TS, TR, U?> =
        Telescope0(null, this, nested)

@JvmName("1")
operator fun <TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U> Lens<TS, TR, T>.div(
        nested: Lens<US, T, U>
): Lens<TS, TR, U> =
        Telescope1(null, this, nested)

@Suppress("UPPER_BOUND_VIOLATED")
internal fun <SCH : Schema<SCH>, STR : PartialStruct<SCH>> NamingConvention?.concatErased(
        dis: Lens<SCH, STR, *>, that: Lens<*, *, *>
): Lens<SCH, STR, *> =
        (if (this !== null && dis is NamedLens && that is NamedLens) concatNames(dis.name, that.name) else null).let { name ->
            if (dis.type is DataType.Nullable<*>) Telescope0<SCH, STR, Schema<*>, PartialStruct<Schema<*>>, PartialStruct<Schema<*>>>(
                    name, dis as Lens<SCH, STR, PartialStruct<Schema<*>>?>, that as Lens<Schema<*>, PartialStruct<Schema<*>>, PartialStruct<Schema<*>>>
            ) else Telescope1<SCH, STR, Schema<*>, PartialStruct<Schema<*>>, PartialStruct<Schema<*>>>(
                    name, dis as Lens<SCH, STR, PartialStruct<Schema<*>>>, that as Lens<Schema<*>, PartialStruct<Schema<*>>, PartialStruct<Schema<*>>>
            )
        }

/**
 * Generates lenses which names are concatenated using snake_case
 */
object SnakeCase : NamingConvention() {

    override fun concatNames(outer: String, nested: String): String =
            outer + '_' + nested

}

/**
 * Generates lenses which names are concatenated using camelCase
 */
object CamelCase : NamingConvention() {

    override fun concatNames(outer: String, nested: String): String = buildString {
        append(outer)
        if (nested.isNotEmpty()) {
            append(nested[0].toUpperCase())
            append(nested, 1, nested.length)
        }
    }

}

// region telescope impl

internal class Telescope0<TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U>(
        name: String?,
        outer: Lens<TS, TR, T?>, nested: Lens<US, T, U>
) : AbsTelescope<TS, TR, US, T?, T, U, U?>(
        name,
        (if (nested.type is DataType.Nullable<*>) nested.type else nullable(nested.type as DataType<Any>)) as DataType<U?>,
        outer, nested
) {

    override fun invoke(p1: TR): U? =
            outer(p1)?.let(nested)

    override fun toString(): String = buildString {
        for (i in 0 until size)
            append(this@Telescope0[i]).append("?.")
        setLength(length - 1) // it's safe since there's no empty lenses
    }

}

internal class Telescope1<TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T : PartialStruct<US>, U>(
        name: String?,
        outer: Lens<TS, TR, T>, nested: Lens<US, T, U>
) : AbsTelescope<TS, TR, US, T, T, U, U>(name, nested.type, outer, nested) {

    override fun invoke(p1: TR): U =
            nested(outer(p1))

    override fun toString(): String = buildString {
        for (i in 0 until size)
            append(this@Telescope1[i]).append('.')
        setLength(length - 1) // it's safe since there's no empty lenses
    }

}

internal abstract class AbsTelescope<TS : Schema<TS>, TR : PartialStruct<TS>, US : Schema<US>, T1 : PartialStruct<US>?, T2 : PartialStruct<US>, U1, U2>(
        name: String?,
        type: DataType<U2>,
        protected val outer: Lens<TS, TR, T1>,
        protected val nested: Lens<US, T2, U1>
) : BaseLens<TS, TR, U2>(name, type) {

    override val size: Int get() = outer.size + nested.size

    override fun get(index: Int): NamedLens<*, *, *> {
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
        if (other !is Lens<*, *, *> || other.javaClass !== javaClass || other.size != size) return false

        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

}

// endregion telescope impl

// region special lenses

internal class PkLens<S : Schema<S>, ID : IdBound>(
        private val table: Table<S, ID, out Record<S, ID>>
) : BaseLens<S, Record<S, ID>, ID>(
        table.idColName, table.idColType
) {

    override fun get(index: Int): NamedLens<*, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            64 // FieldDef.hashCode() is in [0; 63], be different!

    override fun equals(other: Any?): Boolean = // for tests
            other is PkLens<*, *> && other.table === table

    override fun toString(): String =
            "${table.name}.$name (PRIMARY KEY)"


    override fun invoke(p1: Record<S, ID>): ID =
            p1.primaryKey

}

internal class SyntheticColLens<S : Schema<S>, ST : PartialStruct<S>?, TS : Schema<TS>, TR : PartialStruct<TS>?>(
        private val table: Table<S, *, *>,
        name: String,
        private val path: Lens<S, ST, TR>,
        nullize: Boolean
) : BaseLens<S, ST, Long? /*= FieldSet<TS, *>?*/>(
        name,
        if (nullize || path.type is DataType.Nullable<*>) nullableLong else long as DataType<Long?>
) {

    override fun get(index: Int): NamedLens<*, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            if (type is DataType.Nullable<*>) 65 else 66 // FieldDef.hashCode() is in [0; 63], PK is 64, be different!

    // tests only

    override fun equals(other: Any?): Boolean =
            other is SyntheticColLens<*, *, *, *> &&
                    table === other.table && path == other.path && name == other.name && type == other.type

    override fun toString(): String = buildString {
        append(table.name).append('.').append(name).append(" (")
        val nullable = type is DataType.Nullable<*>
        val actual: DataType<*> = if (type is DataType.Nullable<*>) type.actualType else type
        val partial = actual !is Schema<*>
        if (nullable) append("nullability info")
        if (nullable && partial) append(", ")
        if (partial) append("fields set")
        append(')')
    }

    override fun invoke(p1: ST): Long? {
        val struct = path(p1)
        return if (struct === null) {
            check(type === nullableLong) // === check(path.type is DataType.Nullable<*>)
            null
        } else {
            struct.fields.bitmask
        }
    }

}

// ugly class for minimizing method count / vtable size
internal abstract class BaseLens<SCH : Schema<SCH>, STR : PartialStruct<SCH>?, T>(
        private val _name: String?,
        final override val type: DataType<T>
) : NamedLens<SCH, STR, T> {

    final override val name: String
        get() = _name!! // give this instance as Lens, not NamedLens, when _name is null

    override val size: Int
        get() = 1 // true for SyntheticColLens and PkLens, but overridden in AbsTelescope

}

// endregion special lenses
