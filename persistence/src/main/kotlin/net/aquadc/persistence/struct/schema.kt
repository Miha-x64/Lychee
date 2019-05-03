package net.aquadc.persistence.struct

import net.aquadc.persistence.New
import net.aquadc.persistence.type.DataType

/**
 * Declares a struct (or DTO) schema /ˈskiː.mə/.
 * `struct`s in C, Rust, Swift, etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * @see Struct
 * @see FieldDef
 */
abstract class Schema<SELF : Schema<SELF>> : DataType.Partial<Struct<SELF>, SELF>() {

    override val schema: SELF
        get() = this as SELF

    /**
     * A temporary list of [FieldDef]s used while [Schema] is getting constructed.
     */
    @JvmField @JvmSynthetic internal var tmpFields: ArrayList<FieldDef<SELF, *>>? = ArrayList()
    private var mutableCount: Byte = 0

    /**
     * A list of fields of this struct.
     *
     * {@implNote
     *   on concurrent access, we might null out [tmpFields] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val fields: Array<out FieldDef<SELF, *>>
        get() = _fields.value
    private val _fields =
            lazy(LazyFields(0) as () -> Array<out FieldDef<SELF, *>>)

    val fieldsByName: Map<String, FieldDef<SELF, *>>
        get() = _byName.value
    private val _byName =
            lazy(LazyFields(1) as () -> Map<String, FieldDef<SELF, *>>)

    val mutableFields: Array<out FieldDef.Mutable<SELF, *>>
        get() = _mutableFields.value
    private val _mutableFields =
            lazy(LazyFields(2) as () -> Array<out FieldDef.Mutable<SELF, *>>)

    val immutableFields: Array<out FieldDef.Immutable<SELF, *>>
        get() = _immutableFields.value
    private val _immutableFields =
            lazy(LazyFields(3) as () -> Array<out FieldDef.Immutable<SELF, *>>)

    /**
     * Gets called before this fully initialized structDef gets used for the first time.
     */
    @Deprecated("looks useless")
    protected open fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<SELF, *>>) { }

    @JvmSynthetic internal fun tmpFields() =
            tmpFields ?: throw IllegalStateException("schema `${javaClass.simpleName}` is already initialized")

    /**
     * Creates, remembers and returns a new mutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    @Suppress("UNCHECKED_CAST")
    protected infix fun <T> String.mut(type: DataType<T>): FieldDef.Mutable<SELF, T> =
            this.mut(type, Unset as T)

    /**
     * Creates, remembers and returns a new mutable field definition with a default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T> String.mut(dataType: DataType<T>, default: T): FieldDef.Mutable<SELF, T> {
        val fields = tmpFields()
        val col = FieldDef.Mutable(this@Schema, this, dataType, fields.size.toByte(), default, mutableCount++)
        fields.add(col)
        return col
    }

    /**
     * Creates, remembers and returns a new immutable field definition.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected infix fun <T> String.let(dataType: DataType<T>): FieldDef.Immutable<SELF, T> {
        val fields = tmpFields()
        val col = FieldDef.Immutable(this@Schema, this, dataType, fields.size.toByte(), (fields.size - mutableCount).toByte())
        fields.add(col)
        return col
    }

    override fun toString(): String =
            javaClass.simpleName

    private inner class LazyFields(
            private val mode: Int
    ) : () -> Any? {

        override fun invoke(): Any? = when (mode) {
            0 -> {
                val fieldList = tmpFields()
                val fields = arrayOfNulls<FieldDef<SELF, *>>(fieldList.size)
                check(fieldList.isNotEmpty()) { "Struct must have at least one field." }
                // Schema.allFieldSet() relies on field count ∈ [1; 64]
                // and FieldDef constructor checks for ordinal ∈ [0; 63]

                val nameSet = HashSet<String>()
                for (i in fieldList.indices) {
                    val field = fieldList[i]
                    if (!nameSet.add(field.name)) {
                        throw IllegalStateException("duplicate column: `${this@Schema.javaClass.simpleName}`.`${field.name}`")
                    }
                    fields[i] = field
                }

                beforeFreeze(nameSet, fieldList)
                tmpFields = null
                fields
            }

            1 ->
                fields.associateByTo(New.map<String, FieldDef<SELF, *>>(fields.size), FieldDef<SELF, *>::name)

            2 ->
                arrayOfNulls<FieldDef.Mutable<SELF, *>>(mutableCount.toInt()).also { mut ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is FieldDef.Mutable) mut[i++] = field
                    }
                }

            3 ->
                arrayOfNulls<FieldDef.Immutable<SELF, *>>(fields.size - mutableCount.toInt()).also { mut ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is FieldDef.Immutable) mut[i++] = field
                    }
                }

            else ->
                throw AssertionError()
        }

    }

    override fun load(fields: FieldSet<SELF, FieldDef<SELF, *>>, values: Array<Any?>?): Struct<SELF> =
            schema.build { b ->
                schema.forEach(fields) { field -> // todo: may be zero-copy
                    b[field as FieldDef<SELF, Any?>] = values!![field.ordinal.toInt()]
                }
            }

    override fun store(value: Struct<SELF>): PartialStruct<SELF> =
            value

}

/**
 * A field on a struct (`someStruct\[Field]`), potentially nested (`someStruct\[F1]\[F2]\[F3]`).
 */
interface Lens<SCH : Schema<SCH>, in STR : PartialStruct<SCH>?, T> : (STR) -> T {

    /**
     * Type of values stored within a field/column represented by this lens.
     */
    val type: DataType<T>

    /**
     * A default/initial/fallback value for a field/column represented by this lens.
     * @throws RuntimeException if no such value
     */
    val default: T

    val size: Int
    operator fun get(index: Int): NamedLens<*, *, *> // any lens consists of small lenses, which are always named

}

interface NamedLens<SCH : Schema<SCH>, in STR : PartialStruct<SCH>?, T> : Lens<SCH, STR, T> {
    val name: String
}

/**
 * Struct field is a single key-value mapping. FieldDef represents a key with name and type.
 * When treated as a function, returns value of this [FieldDef] on a given [Struct].
 * Note: constructors are internal to guarantee correct [ordinal] values.
 * @see Schema
 * @see Struct
 * @see Mutable
 * @see Immutable
 */
sealed class FieldDef<SCH : Schema<SCH>, T>(
        @JvmField val schema: Schema<SCH>,
        override val name: String,
        override val type: DataType<T>,
        @JvmField val ordinal: Byte,
        default: T
) : NamedLens<SCH, PartialStruct<SCH>, T> {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    private val _default = default

    override val default: T
        get() = if (_default === Unset) throw NoSuchElementException("no default value for $this") else _default

    val hasDefault: Boolean
        @JvmName("hasDefault") get() = _default !== Unset

    override fun invoke(p1: PartialStruct<SCH>): T =
            p1[this]

    override val size: Int get() = 1

    override fun get(index: Int): NamedLens<*, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            ordinal.toInt()

    // equals() is default ­— identity

    override fun toString(): String = schema.javaClass.simpleName + '[' + name + " #" + ordinal + ' ' + when (this) {
        is Mutable -> "mut#$mutableOrdinal"
        is Immutable -> "let#$immutableOrdinal"
    } + ']'

    /**
     * Represents a mutable field of a [Struct]: its value can be changed.
     */
    class Mutable<SCH : Schema<SCH>, T> internal constructor(
            schema: Schema<SCH>,
            name: String,
            type: DataType<T>,
            ordinal: Byte,
            default: T,
            @JvmField val mutableOrdinal: Byte
    ) : FieldDef<SCH, T>(schema, name, type, ordinal, default)

    /**
     * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
     */
    @Suppress("UNCHECKED_CAST")
    class Immutable<SCH : Schema<SCH>, T> internal constructor(
            schema: Schema<SCH>,
            name: String,
            type: DataType<T>,
            ordinal: Byte,
            @JvmField val immutableOrdinal: Byte
    ) : FieldDef<SCH, T>(schema, name, type, ordinal, Unset as T)

}

@JvmField @JvmSynthetic internal val Unset = Any()
