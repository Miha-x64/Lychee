package net.aquadc.persistence.struct

import net.aquadc.persistence.New
import net.aquadc.persistence.fieldValues
import net.aquadc.persistence.fill
import net.aquadc.persistence.type.DataType

/**
 * Declares a struct (or DTO, VO, Entity) schema /ˈskiː.mə/.
 * `struct`s in C, Rust, Swift, etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * @see Struct
 * @see FieldDef
 */
abstract class Schema<SELF : Schema<SELF>> : DataType.Partial<Struct<SELF>, SELF>() {

    /**
     * A temporary list of [FieldDef]s used while [Schema] is getting constructed.
     */
    @JvmField @JvmSynthetic internal var tmpFields: ArrayList<FieldDef<SELF, *, *>>? = ArrayList()
    @JvmField @JvmSynthetic internal var mutableCount: Byte = 0

    /**
     * A list of fields of this struct.
     *
     * {@implNote
     *   on concurrent access, we might null out [tmpFields] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val fields: Array<out FieldDef<SELF, *, *>>
        get() = _fields.value
    private val _fields =
            lazy(LazyFields(0) as () -> Array<out FieldDef<SELF, *, *>>)

    val fieldsByName: Map<String, FieldDef<SELF, *, *>>
        get() = _byName.value
    private val _byName =
            lazy(LazyFields(1) as () -> Map<String, FieldDef<SELF, *, *>>)

    val mutableFields: Array<out FieldDef.Mutable<SELF, *, *>>
        get() = _mutableFields.value
    private val _mutableFields =
            lazy(LazyFields(2) as () -> Array<out FieldDef.Mutable<SELF, *, *>>)

    val immutableFields: Array<out FieldDef.Immutable<SELF, *, *>>
        get() = _immutableFields.value
    private val _immutableFields =
            lazy(LazyFields(3) as () -> Array<out FieldDef.Immutable<SELF, *, *>>)

    /**
     * Gets called before this fully initialized struct gets used for the first time.
     */
    @Deprecated("looks useless")
    protected open fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<SELF, *, *>>) { }

    @JvmSynthetic internal fun tmpFields() =
            tmpFields ?: throw IllegalStateException("schema `${javaClass.simpleName}` is already initialized")

    /**
     * Creates, remembers, and returns a new mutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    @Suppress("UNCHECKED_CAST")
    protected infix fun <T, DT : DataType<T>> String.mut(type: DT): FieldDef.Mutable<SELF, T, DT> =
            this.mut(type, Unset as T)

    /**
     * Creates, remembers, and returns a new mutable field definition with a default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T, DT : DataType<T>> String.mut(dataType: DT, default: T): FieldDef.Mutable<SELF, T, DT> {
        val fields = tmpFields()
        val col = FieldDef.Mutable(schema, this, dataType, fields.size.toByte(), default, mutableCount++)
        fields.add(col)
        return col
    }

    /**
     * Creates, remembers, and returns a new immutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    @Suppress("UNCHECKED_CAST")
    protected infix fun <T, DT : DataType<T>> String.let(dataType: DT): FieldDef.Immutable<SELF, T, DT> =
            this.let(dataType, Unset as T)

    /**
     * Creates, remembers, and returns a new immutable field definition with a default value.
     * Default value for immutable column looks useless when building instances directly,
     * but it is useful for maintaining compatibility of data transport format.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T, DT : DataType<T>> String.let(dataType: DT, default: T): FieldDef.Immutable<SELF, T, DT> {
        val fields = tmpFields()
        val col = FieldDef.Immutable(schema, this, dataType, fields.size.toByte(), default, (fields.size - mutableCount).toByte())
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
                val fields = arrayOfNulls<FieldDef<SELF, *, *>>(fieldList.size)
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
                fields.associateByTo(New.map<String, FieldDef<SELF, *, *>>(fields.size), FieldDef<SELF, *, *>::name)

            2 ->
                arrayOfNulls<FieldDef.Mutable<SELF, *, *>>(mutableCount.toInt()).also { mut ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is FieldDef.Mutable) mut[i++] = field
                    }
                }

            3 ->
                arrayOfNulls<FieldDef.Immutable<SELF, *, *>>(fields.size - mutableCount.toInt()).also { mut ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is FieldDef.Immutable) mut[i++] = field
                    }
                }

            else ->
                throw AssertionError()
        }

    }

    // todo: may be zero-copy
    override fun load(fields: FieldSet<SELF, FieldDef<SELF, *, *>>, values: Any?): Struct<SELF> =
            schema.build { builder -> fill(builder, this, fields, values) }

    override fun fields(value: Struct<SELF>): FieldSet<SELF, FieldDef<SELF, *, *>> =
            schema.allFieldSet()

    override fun store(value: Struct<SELF>): Any? =
            value.fieldValues()

}

interface Named {
    val name: String
}

/**
 * A field on a struct (`someStruct\[Field]`), potentially nested (`someStruct\[F1]\[F2]\[F3]`).
 * Nesting exists in stored representation
 * (i. e. `storedLens.dropLast(1).map(StoredLens::type).all { it is Partial || (it is Nullable && it.actualType is Partial) }`)
 * but not guaranteed to exist in runtime representation (for this, see [Lens]).
 */
interface StoredLens<SCH : Schema<SCH>, T, DT : DataType<T>> {

    /**
     * Type of values stored within a field/column represented by this lens.
     */
    val type: DataType<T>

    /**
     * Exact type of values stored within a field/column represented by this lens.
     * Note: `StoredLens<*, T, *>::exactType` is inferred to `DataType<*>`
     * while `StoredLens<*, T, *>::type` is useful `DataType<T>`.
     */
    val exactType: DT

    val size: Int
    operator fun get(index: Int): NamedLens<*, *, *, *> // any lens consists of small lenses, which are always named

}

/**
 * A field on a struct (`someStruct\[Field]`), potentially nested (`someStruct\[F1]\[F2]\[F3]`).
 * [invoke] function must return [T] if input is not `null` and contains the requested field,
 * i. e. it must be safe to cast `Lens<SCH, PartialStruct<SCH>?, T>` to `(Struct<SCH>) -> T`
 */
interface Lens<SCH : Schema<SCH>, in STR : PartialStruct<SCH>, T, DT : DataType<T>> : StoredLens<SCH, T, DT>, (STR) -> T? {

    /**
     * A default/initial/fallback value for a field/column represented by this lens.
     * @throws RuntimeException if no such value
     */
    @Deprecated("does not look very useful", ReplaceWith("(this[this.size-1] as? FieldDef<SCH, T, *> ?: throw NoSuchElementException()).default"))
    val default: T

}

/**
 * Returns a function which is a special case of this [Lens] for non-partial [Struct]s
 * which implies non-nullable [T] as a return type.
 */
fun <SCH : Schema<SCH>, T> Lens<SCH, PartialStruct<SCH>, T, *>.ofStruct(): (Struct<SCH>) -> T =
        this as (Struct<SCH>) -> T


// Damn, dear Kotlin, I just want to return an intersection-type
interface StoredNamedLens<SCH : Schema<SCH>, T, DT : DataType<T>> : StoredLens<SCH, T, DT>, Named
interface NamedLens<SCH : Schema<SCH>, in STR : PartialStruct<SCH>, T, DT : DataType<T>> : StoredNamedLens<SCH, T, DT>, Lens<SCH, STR, T, DT>

/**
 * Struct field is a single key-value mapping. FieldDef represents a key with name and type.
 * When treated as a function, returns value of this [FieldDef] on a given [Struct].
 * Note: constructors are internal to guarantee correct [ordinal] values.
 * @param SCH host schema
 * @param T runtime type of values
 * @param DT stored type of values
 * @see Schema
 * @see Struct
 * @see Mutable
 * @see Immutable
 */
sealed class FieldDef<SCH : Schema<SCH>, T, DT : DataType<T>>(

        /**
         * A schema this field belongs to.
         */
        @JvmField val schema: SCH,

        /**
         * A name unique among the [schema].
         * Required for string-ish serialization like JSON and XML and useful for debugging.
         */
        override val name: String,

        /**
         * Describes type of values it can store and underlying serialization techniques.
         * This property is a part of serialization ABI.
         */
        override val exactType: DT,

        /**
         * Zero-based ordinal number of this field.
         * Fields are getting their ordinals according to the program evaluation order.
         * This property is a part of serialization ABI, so changing field order is a breaking change.
         *
         * Given a Schema
         *     object SampleSchema : Schema<SampleSchema>() {
         *         val First = "first" let string
         *         val Second = "second" let string
         *         val Third = "third" let string
         *     }
         * Field 'removal' in compatible fashion will look like
         *     object SampleSchema : Schema<SampleSchema>() {
         *         val First = "first" let string
         *         init { "second" let string }
         *         val Third = "third" let string
         *     }
         * This will guarantee that property initializers will be executed in the same order
         * and ordinals will remain unchanged.
         */
        @JvmField val ordinal: Byte,

        default: T
) : NamedLens<SCH, PartialStruct<SCH>, T, DT> {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    /**
     * Describes type of values it can store and underlying serialization techniques.
     * This property is a part of serialization ABI.
     *
     * Note: `override val type: DT` is possible and could supersede both [exactType] and [type],
     * but `FieldDef<*, T, *>::exactType` is inferred to `DataType<*>`
     * while `FieldDef<*, T, *>::type` is useful `DataType<T>`.
     */
    override val type: DataType<T>
        get() = exactType

    private val _default = default

    override val default: T
        get() = if (_default === Unset) throw NoSuchElementException("no default value for $this") else _default

    val hasDefault: Boolean
        @JvmName("hasDefault") get() = _default !== Unset

    /**
     * Returns value of [this] field for the given [struct], or `null`, if it is absent.
     */
    override fun invoke(struct: PartialStruct<SCH>): T? =
            if (this in struct.fields) struct.getOrThrow(this)
            else null

    override val size: Int get() = 1

    override fun get(index: Int): NamedLens<*, *, *, *> =
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
    class Mutable<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
            schema: SCH,
            name: String,
            type: DT,
            ordinal: Byte,
            default: T,
            @JvmField val mutableOrdinal: Byte
    ) : FieldDef<SCH, T, DT>(schema, name, type, ordinal, default)

    /**
     * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
     */
    @Suppress("UNCHECKED_CAST")
    class Immutable<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
            schema: SCH,
            name: String,
            type: DT,
            ordinal: Byte,
            default: T,
            @JvmField val immutableOrdinal: Byte
    ) : FieldDef<SCH, T, DT>(schema, name, type, ordinal, default)

}

@JvmField @JvmSynthetic internal val Unset = Any()
