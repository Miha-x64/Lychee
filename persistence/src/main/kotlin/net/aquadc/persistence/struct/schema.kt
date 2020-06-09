@file:JvmName("Schemas")
package net.aquadc.persistence.struct

import net.aquadc.persistence.eq
import net.aquadc.persistence.fieldValues
import net.aquadc.persistence.fill
import net.aquadc.persistence.newMap
import net.aquadc.persistence.type.DataType

/**
 * Declares a struct (or DTO, VO, Entity) schema /ˈskiː.mə/.
 * `struct`s in C, Rust, Swift etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * A Schema enumerates fields, their names, types, mutability,
 * and memory layout of [Struct]s, which are instantiations of Schema.
 * @see Struct
 * @see FieldDef
 */
abstract class Schema<SELF : Schema<SELF>> : DataType.NotNull.Partial<Struct<SELF>, SELF>() {

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

    @Deprecated("names are now `CharSequence`s with undefined hashCode()/equals(). Use fieldByName() instead")
    val fieldsByName: Map<String, FieldDef<SELF, *, *>>
        get() = _byName.value
    private val _byName =
            lazy(LazyFields(1) as () -> Map<String, FieldDef<SELF, *, *>>)

    @Deprecated("use mutableFieldSet instead")
    val mutableFields: Array<out MutableField<SELF, *, *>>
        get() = _mutableFields.value
    private val _mutableFields =
            lazy(LazyFields(2) as () -> Array<out MutableField<SELF, *, *>>)

    @Deprecated("use immutableFieldSet instead")
    val immutableFields: Array<out ImmutableField<SELF, *, *>>
        get() = _immutableFields.value
    private val _immutableFields =
            lazy(LazyFields(3) as () -> Array<out ImmutableField<SELF, *, *>>)

    @JvmSynthetic internal fun tmpFields() =
            tmpFields ?: throw IllegalStateException("schema `${javaClass.simpleName}` is already initialized")

    private var mutableFieldBits = 0L

    /** A set of all fields of [this] [Schema]. */
    val allFieldSet: FieldSet<SELF, FieldDef<SELF, *, *>>
        get() = FieldSet(fields.size.let { size ->
            // (1L shl size) - 1   :  1L shl 64  will overflow to 1
            // -1L ushr (64 - size): -1L ushr 64 will remain -1L
            // the last one is okay, assuming that zero-field structs are prohibited
            -1L ushr (64 - size)
        })

    /** A set of all [MutableField]s of [this] [Schema]. */
    val mutableFieldSet: FieldSet<SELF, MutableField<SELF, *, *>>
        get() = FieldSet(fields.let { mutableFieldBits })

    /** A set of all [ImmutableField]s of [this] [Schema]. */
    val immutableFieldSet: FieldSet<SELF, ImmutableField<SELF, *, *>>
        get() = FieldSet(fields.let { (-1L ushr (64 - it.size)) and mutableFieldBits.inv() })

    /**
     * Creates, remembers, and returns a new mutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    @Suppress("UNCHECKED_CAST")
    // wannabe inline but https://youtrack.jetbrains.com/issue/KT-38827
    protected infix fun <T, DT : DataType<T>> CharSequence.mut(type: DT): MutableField<SELF, T, DT> =
            this.mut(type, Unset as T)

    /**
     * Creates, remembers, and returns a new mutable field definition with a default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T, DT : DataType<T>> CharSequence.mut(dataType: DT, default: T): MutableField<SELF, T, DT> {
        val fields = tmpFields()
        val col = MutableField(schema, this, dataType, fields.size.toByte(), default, mutableCount++)
        fields.add(col)
        return col
    }

    /**
     * Creates, remembers, and returns a new immutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    @Suppress("UNCHECKED_CAST")
    // wannabe inline but https://youtrack.jetbrains.com/issue/KT-38827
    protected infix fun <T, DT : DataType<T>> CharSequence.let(dataType: DT): ImmutableField<SELF, T, DT> =
            this.let(dataType, Unset as T)

    /**
     * Creates, remembers, and returns a new immutable field definition with a default value.
     * Default value for immutable column looks useless when building instances directly,
     * but it is useful for maintaining compatibility of data transport format.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T, DT : DataType<T>> CharSequence.let(dataType: DT, default: T): ImmutableField<SELF, T, DT> {
        val fields = tmpFields()
        val col = ImmutableField(schema, this, dataType, fields.size.toByte(), default, (fields.size - mutableCount).toByte())
        fields.add(col)
        return col
    }

    private inner class LazyFields(
            private val mode: Int
    ) : () -> Any? {

        override fun invoke(): Any? = when (mode) {
            0 -> {
                val fieldList = tmpFields()
                check(fieldList.isNotEmpty()) { "Struct must have at least one field." }
                // Schema.allFieldSet() relies on field count ∈ [1; 64]
                // and FieldDef constructor checks for ordinal ∈ [0; 63]
                val fields = arrayOfNulls<FieldDef<SELF, *, *>>(fieldList.size)
                val namesTypesDefaults = arrayOfNulls<Any>(3 * fieldList.size)

                val nameSet = HashSet<String>()
                var mutBits = 0L
                for (i in fieldList.indices) {
                    val field = fieldList[i]
                    val name = field.name// name(schema) is not computed yet!
                    if (!nameSet.add(name)) {
                        throw IllegalStateException("duplicate column: `${this@Schema.javaClass.simpleName}`.`${name}`")
                    }
                    fields[i] = field
                    namesTypesDefaults[3*i] = name
                    namesTypesDefaults[3*i+1] = field.type
                    namesTypesDefaults[3*i+2] = field._default
                    field.foldOrdinal(
                        ifMutable = { mutBits = mutBits or (1L shl field.ordinal.toInt()) },
                        ifImmutable = { }
                    )
                }

                tmpFields = null
                _fieldNamesTypesDefaults = namesTypesDefaults
                mutableFieldBits = mutBits
                fields
            }

            1 ->
                fields.associateByTo(newMap<String, FieldDef<SELF, *, *>>(fields.size), { it.name(schema).toString() })

            2 ->
                arrayOfNulls<MutableField<SELF, *, *>>(mutableCount.toInt()).also { mut ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is MutableField) mut[i++] = field
                    }
                }

            3 ->
                arrayOfNulls<ImmutableField<SELF, *, *>>(fields.size - mutableCount.toInt()).also { imm ->
                    var i = 0
                    fields.forEach { field ->
                        if (field is ImmutableField) imm[i++] = field
                    }
                }

            else ->
                throw AssertionError()
        }

    }

    // todo: may be zero-copy
    override fun load(fields: FieldSet<SELF, FieldDef<SELF, *, *>>, values: Any?): Struct<SELF> =
            schema { builder -> fill(builder, this, fields, values) }

    override fun fields(value: Struct<SELF>): FieldSet<SELF, FieldDef<SELF, *, *>> =
        schema.allFieldSet

    override fun store(value: Struct<SELF>): Any? =
            value.fieldValues()

    // names

    fun nameOf(field: FieldDef<SELF, *, *>): CharSequence =
            namesTypesDefaults()[3 * field.ordinal.toInt()] as CharSequence

    inline fun <R> fieldByName(
            name: CharSequence,
            ifFound: (FieldDef<SELF, *, *>) -> R,
            ifNot: () -> R
    ): R {
        val idx = indexByName(name)
        return if (idx >= 0) ifFound(fields[idx]) else ifNot()
    }

    @PublishedApi internal fun indexByName(name: CharSequence): Int {
        val array = namesTypesDefaults()
        for (i in 0 .. array.size step 3)
            if ((array[i] as CharSequence).eq(name, false))
                return i/3
        return -1
    }

    // types

    inline fun <T, DT : DataType<T>> typeOf(field: FieldDef<SELF, T, DT>): DT =
            namesTypesDefaults()[3 * field.ordinal.toInt() + 1] as DT

    // defaults

    inline fun <T> defaultOrElse(field: FieldDef<SELF, T, *>, orElse: () -> T): T =
            namesTypesDefaults()[3 * field.ordinal.toInt() + 2].let { def ->
                if (def !== Unset) def as T else orElse()
            }

    // etc

    @JvmSynthetic internal var _fieldNamesTypesDefaults: Array<out Any?>? = null
    @PublishedApi internal fun namesTypesDefaults() =
            _fieldNamesTypesDefaults ?: fields.let { _fieldNamesTypesDefaults!! }

    // the following extensions will be un-shadowed after removal of members
    inline val FieldDef<SELF, *, *>.name: CharSequence get() = nameOf(this)
    inline val <T, DT : DataType<T>> FieldDef<SELF, T, DT>.type: DT get() = typeOf(this)

}


interface Named<SCH : Schema<SCH>> {
    @Deprecated("Lenses are becoming dumb. Keep your Schema with you", ReplaceWith("this.name(schema)"))
    val name: String
    fun name(mySchema: SCH): CharSequence
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
    @Deprecated("Lenses are becoming dumb. Keep your Schema with you", ReplaceWith("this.type(schema)"))
    val type: DT
    fun type(mySchema: SCH): DT

    val size: Int
    operator fun get(index: Int): NamedLens<*, *, *, *, *> // any lens consists of small lenses, which are always named

}

/**
 * A field on a struct (`someStruct\[Field]`), potentially nested (`someStruct\[F1]\[F2]\[F3]`).
 */
interface Lens<SCH : Schema<SCH>,
        in PRT : PartialStruct<SCH>, in STR : Struct<SCH>,
        T, DT : DataType<T>
        > : StoredLens<SCH, T, DT>, (STR) -> T {

    /**
     * Checks whether [struct] has a value which can be returned by [ofPartial].
     * `true` means that [ofPartial] will return [T], not `T?`
     * always `true` for [Struct]s
     */
    fun hasValue(struct: PRT): Boolean

    /**
     * Get value of this lens on the given [partial], or null, if absent.
     */
    fun ofPartial(partial: PRT): T?

    /**
     * Get value of this lens on the given [struct].
     */
    override fun invoke(struct: STR): T
    // re-abstracted for KDoc

}

/**
 * Returns a function which is a special case of this [Lens] for non-partial [Struct]s
 * which implies non-nullable [T] as a return type.
 */
@Deprecated("not needed anymore", ReplaceWith("this"), DeprecationLevel.ERROR)
fun <SCH : Schema<SCH>, T> Lens<SCH, PartialStruct<SCH>, Struct<SCH>, T, *>.ofStruct(): Nothing =
        throw AssertionError()


// Damn, dear Kotlin, I just want to return an intersection-type
interface StoredNamedLens<SCH : Schema<SCH>, T, DT : DataType<T>>
    : StoredLens<SCH, T, DT>, Named<SCH>

interface NamedLens<SCH : Schema<SCH>, in PRT : PartialStruct<SCH>, in STR : Struct<SCH>, T, DT : DataType<T>>
    : StoredNamedLens<SCH, T, DT>, Lens<SCH, PRT, STR, T, DT>

/**
 * Struct field is a single key-value mapping. FieldDef represents a key with name and type.
 * When treated as a function, returns value of this [FieldDef] on a given [Struct].
 * Note: constructors are internal to guarantee correct [ordinal] values.
 * @param SCH host schema
 * @param T runtime type of values
 * @param DT stored type of values
 * @see Schema
 * @see Struct
 * @see MutableField
 * @see ImmutableField
 */
sealed class FieldDef<SCH : Schema<SCH>, T, DT : DataType<T>>(

        /**
         * A schema this field belongs to.
         */
        @Deprecated("Lenses are becoming dumb. Keep your Schema with you")
        @JvmField val schema: SCH,

        /**
         * A name unique among the [schema].
         * Required for string-ish serialization like JSON and XML and useful for debugging.
         */
        @Deprecated("Lenses are becoming dumb. Keep your Schema with you", ReplaceWith("this.name(schema)"))
        override val name: String,

        /**
         * Describes type of values it can store and underlying serialization techniques.
         * This property is a part of serialization ABI.
         */
        @Deprecated("Lenses are becoming dumb. Keep your Schema with you", ReplaceWith("this.type(schema)"))
        override val type: DT,

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

        default: T // todo maybe deprecate it?
) : NamedLens<SCH, PartialStruct<SCH>, Struct<SCH>, T, DT> {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    override fun name(mySchema: SCH): CharSequence = // this should take self name from Schema in future
            mySchema.nameOf(this).also { check(it === name) }

    override fun type(mySchema: SCH): DT =
            mySchema.typeOf(this).also { check(it === type) }

    @JvmSynthetic @JvmField internal val _default = default

    @Deprecated("Lenses are becoming dumb. Keep your Schema with you")
    val default: T
        get() = if (_default === Unset) throw NoSuchElementException("no default value for $this") else _default

    @Deprecated("Lenses are becoming dumb. Keep your Schema with you", ReplaceWith("this.type(schema)"))
    val hasDefault: Boolean
        @JvmName("hasDefault") get() = _default !== Unset

    override fun hasValue(struct: PartialStruct<SCH>): Boolean =
            this in struct.fields

    override fun ofPartial(partial: PartialStruct<SCH>): T? =
            if (this in partial.fields) partial.getOrThrow(this)
            else null

    override fun invoke(struct: Struct<SCH>): T =
            struct[this]

    override val size: Int get() = 1

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
            if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
            ordinal.toInt()

    override fun equals(other: Any?): Boolean {
        if (other !is FieldDef<*, *, *> || other.ordinal != ordinal) return false
        check(other.schema == schema)
        check(other.name == name)
        check(other.type == type)
        check(other._default == _default)
        return true
    }

    override fun toString(): String = StringBuilder()
        .append("field(#").append(ordinal).append(' ')
        .run { foldOrdinal(
                ifMutable = { append("mut").append('#').append(it) },
                ifImmutable = { append("let").append('#').append(it) }
        ) }
        .append(')')
        .toString()

    /**
     * Represents a mutable field of a [Struct]: its value can be changed.
     */
    @Deprecated("moved", ReplaceWith("MutableField"))
    open class Mutable<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
            schema: SCH, name: CharSequence, type: DT, ordinal: Byte, default: T, @JvmField val mutableOrdinal: Byte
    ) : FieldDef<SCH, T, DT>(schema, name.toString(), type, ordinal, default)

    /**
     * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
     */
    @Deprecated("moved", ReplaceWith("ImmutableField"))
    open class Immutable<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
            schema: SCH, name: CharSequence, type: DT, ordinal: Byte, default: T, @JvmField val immutableOrdinal: Byte
    ) : FieldDef<SCH, T, DT>(schema, name.toString(), type, ordinal, default)

}


/**
 * Represents a mutable field of a [Struct]: its value can be changed.
 */
/*wannabe inline*/ class MutableField<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
    schema: SCH, name: CharSequence, type: DT, ordinal: Byte, default: T, mutableOrdinal: Byte
) : FieldDef.Mutable<SCH, T, DT>(schema, name, type, ordinal, default, mutableOrdinal)

/**
 * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
 */
/*wannabe inline*/ class ImmutableField<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
    schema: SCH, name: CharSequence, type: DT, ordinal: Byte, default: T, immutableOrdinal: Byte
) : FieldDef.Immutable<SCH, T, DT>(schema, name, type, ordinal, default, immutableOrdinal)

inline fun <R> FieldDef<*, *, *>.foldOrdinal(
    ifMutable: (mutableOrdinal: Int) -> R,
    ifImmutable: (immutableOrdinal: Int) -> R
): R = when (this) {
    is FieldDef.Mutable -> ifMutable(mutableOrdinal.toInt())
    is FieldDef.Immutable -> ifImmutable(immutableOrdinal.toInt())
}
inline fun <SCH : Schema<SCH>, T, DT : DataType<T>, R> FieldDef<SCH, T, DT>.foldField(
    ifMutable: (MutableField<SCH, T, DT>) -> R,
    ifImmutable: (ImmutableField<SCH, T, DT>) -> R
): R = when (this) {
    is FieldDef.Mutable -> ifMutable(this as MutableField<SCH, T, DT>)
    is FieldDef.Immutable -> ifImmutable(this as ImmutableField<SCH, T, DT>)
}

@JvmField @JvmSynthetic @PublishedApi internal val Unset = Any()
