package net.aquadc.persistence.struct

import net.aquadc.persistence.New
import net.aquadc.persistence.type.DataType
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap

/**
 * Declares a struct (or DTO) schema /ˈskiː.mə/.
 * `struct`s in C, Rust, Swift, etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * @see Struct
 * @see FieldDef
 *
 * Note: may be moved to [DataType] and become its subtype soon.
 */
abstract class Schema<SELF : Schema<SELF>> : DataType.Partial<Struct<SELF>, SELF>() {

    override val schema: SELF
        get() = this as SELF

    /**
     * A temporary list of [FieldDef]s used while [Schema] is getting constructed.
     */
    @JvmField @JvmSynthetic internal var tmpFields: ArrayList<FieldDef<SELF, *>>? = ArrayList()

    /**
     * A list of fields of this struct.
     *
     * {@implNote
     *   on concurrent access, we might null out [tmpFields] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val fields: List<FieldDef<SELF, *>>
        get() = _fields.value
    private val _fields =
            lazy(LazyFields(0) as () -> List<FieldDef<SELF, *>>)

    val fieldsByName: Map<String, FieldDef<SELF, *>>
        get() = _byName.value
    private val _byName =
            lazy(LazyFields(1) as () -> Map<String, FieldDef<SELF, *>>)

    val mutableFields: List<FieldDef.Mutable<SELF, *>>
        get() = _mutableFields.value
    private val _mutableFields =
            lazy(LazyFields(2) as () -> List<FieldDef.Mutable<SELF, *>>)

    val immutableFields: List<FieldDef.Immutable<SELF, *>>
        get() = _immutableFields.value
    private val _immutableFields =
            lazy(LazyFields(3) as () -> List<FieldDef.Immutable<SELF, *>>)

    private var tmpMutableCount: Byte = 0

    /**
     * Gets called before this fully initialized structDef gets used for the first time.
     */
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
        val col = FieldDef.Mutable(this@Schema, this, dataType, fields.size.toByte(), default, tmpMutableCount++)
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
        val col = FieldDef.Immutable(this@Schema, this, dataType, fields.size.toByte(), (fields.size - tmpMutableCount).toByte())
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
                val fields = tmpFields()
                check(fields.isNotEmpty()) { "Struct must have at least one field." }
                // Schema.allFieldSet() relies on field count ∈ [1; 64]
                // and FieldDef constructor checks for ordinal ∈ [0; 63]

                val nameSet = HashSet<String>()
                for (i in fields.indices) {
                    val col = fields[i]
                    if (!nameSet.add(col.name)) {
                        throw IllegalStateException("duplicate column: `${this@Schema.javaClass.simpleName}`.`${col.name}`")
                    }
                }

                beforeFreeze(nameSet, fields)
                tmpFields = null
                fields
            }

            1 ->
                fields.associateByTo(New.map<String, FieldDef<SELF, *>>(fields.size), FieldDef<SELF, *>::name)

            2 ->
                fields.filterIsInstance<FieldDef.Mutable<SELF, *>>()

            3 ->
                fields.filterIsInstance<FieldDef.Immutable<SELF, *>>()

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
 * Implements [hashCode] and [equals]
 */
abstract class Lens<SCH : Schema<SCH>, T>(
        @JvmField val name: String,
        @JvmField val type: DataType<T>
) /*: (PartialStruct<SCH>) -> T*/ {

    abstract val size: Int
    abstract operator fun get(index: Int): FieldDef<*, *>

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
        if (other !is Lens<*, *> || other.size != size) return false

        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    override fun toString(): String = buildString {
        for (i in 0 until size)
            append(this@Lens[i]).append('.')
        setLength(length - 1) // it's safe since there's no empty lenses
    }

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
        name: String,
        type: DataType<T>,
        @JvmField val ordinal: Byte,
        default: T
) : Lens<SCH, T>(name, type), (PartialStruct<SCH>) -> T {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    private val _default = default

    val default: T
        get() = if (_default === Unset) throw NoSuchElementException("no default value for $this") else _default

    val hasDefault: Boolean
        @JvmName("hasDefault") get() = _default !== Unset

    override fun invoke(p1: PartialStruct<SCH>): T =
            p1[this]

    override val size: Int get() = 1

    override fun get(index: Int): FieldDef<*, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            ordinal.toInt()

    override fun equals(other: Any?): Boolean =
            this === other

    override fun toString(): String = schema.javaClass.simpleName + '.' + name + '@' + ordinal + " (" + when (this) {
        is Mutable -> "mutable#$mutableOrdinal"
        is Immutable -> "immutable#$immutableOrdinal"
    } + ')'

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
