@file:[
    JvmName("Schemas")
    Suppress(
        "NON_PUBLIC_PRIMARY_CONSTRUCTOR_OF_INLINE_CLASS",
        "RESERVED_MEMBER_INSIDE_INLINE_CLASS",
        "NOTHING_TO_INLINE"
    )
]
package net.aquadc.persistence.struct

import net.aquadc.persistence.eq
import net.aquadc.persistence.fieldValues
import net.aquadc.persistence.fill
import net.aquadc.persistence.newSet
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

    // if ArrayList, a temporary list of Field internals used while [Schema] subtype is being constructed;
    // if Array, a final list of 'em
    @JvmField @JvmSynthetic internal var tmpFields: Any = ArrayList<Any?>(16/*=round(name*type*def*avg.6fields)*/)

    // Written during subclass initialization, conforms FieldSet format
    @JvmField @JvmSynthetic internal var mutableFieldBits = 0L

    @Deprecated("names are now `CharSequence`s with undefined hashCode()/equals(). Use fieldByName() instead", level = DeprecationLevel.ERROR)
    val fieldsByName: Map<String, FieldDef<SELF, *, *>>
        get() = throw AssertionError()

    @Deprecated("use mutableFieldSet instead", level = DeprecationLevel.ERROR)
    val mutableFields: Nothing
        get() = throw AssertionError()

    @Deprecated("use immutableFieldSet instead", level = DeprecationLevel.ERROR)
    val immutableFields: Nothing
        get() = throw AssertionError()

    @Deprecated("use allFieldSet and fieldAt() instead")
    val fields: FieldSet<SELF, out FieldDef<SELF, *, *>>
        get() = allFieldSet

    /** A set of all fields of this [Schema]. */
    val allFieldSet: FieldSet<SELF, FieldDef<SELF, *, *>>
        @get:JvmName("allFieldSet") get() = FieldSet((namesTypesDefaults().size / 3).let { size ->
            // (1L shl size) - 1   :  1L shl 64  will overflow to 1
            // -1L ushr (64 - size): -1L ushr 64 will remain -1L
            // the last one is okay, assuming that zero-field structs are prohibited
            -1L ushr (64 - size)
        })

    fun fieldAt(ordinal: Int): FieldDef<SELF, *, *> {
        val count = namesTypesDefaults().size / 3
        if (ordinal !in 0 until count) throw IndexOutOfBoundsException("$ordinal !in 0..$count")
        val mutable = ((1L shl ordinal) and mutableFieldBits) != 0L
        val specialOrdinal = (if (mutable) mutableFieldBits else mutableFieldBits.inv()).indexOf(ordinal)
        return FieldDef<SELF, Any?, DataType<Any?>>(field(ordinal, specialOrdinal, mutable))
    }

    /** A set of all [MutableField]s of this [Schema]. */
    val mutableFieldSet: FieldSet<SELF, MutableField<SELF, *, *>>
        get() = FieldSet(namesTypesDefaults().let { mutableFieldBits })

    /** A set of all [ImmutableField]s of this [Schema]. */
    val immutableFieldSet: FieldSet<SELF, ImmutableField<SELF, *, *>>
        get() = FieldSet(namesTypesDefaults().let { (-1L ushr (64 - it.size/3)) and mutableFieldBits.inv() })

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
        val mfb = mutableFieldBits
        val ord = fields.size / 3
        val col = MutableField<SELF, T, DT>(field(ord, java.lang.Long.bitCount(mfb), true))
        mutableFieldBits = mfb or (1L shl ord)
        fields.add(this)
        fields.add(dataType)
        fields.add(default)
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
        val total = fields.size / 3
        val col = ImmutableField<SELF, T, DT>(field(total, total - java.lang.Long.bitCount(mutableFieldBits), false))
        fields.add(this)
        fields.add(dataType)
        fields.add(default)
        return col
    }

    // for Layout, see FieldDef: 0000 0000 0000 000m 00yy yyyy 00xx xxxx
    private fun field(ordinal: Int, specialOrdinal: Int, mutable: Boolean): Int {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
        check(specialOrdinal < 64)
        return ordinal or (specialOrdinal shl 8) or (if (mutable) 65536 else 0)
    }

    @Suppress("UNCHECKED_CAST") private fun tmpFields() =
        tmpFields as? ArrayList<Any?>
            ?: throw IllegalStateException("schema `${javaClass.simpleName}` is already initialized")

    @Synchronized private fun initialize() {
        val fieldList = tmpFields()
        check(fieldList.isNotEmpty()) { "Struct must have at least one field." }

        val nameSet = newSet<String>(fieldList.size / 3)
        for (i in fieldList.indices step 3) {
            val name = (fieldList[i] as CharSequence).toString()
            if (!nameSet.add(name)) {
                throw IllegalStateException("duplicate field: `${this@Schema.javaClass.simpleName}`.`${name}`")
            }
        }

        tmpFields = fieldList.toArray()
    }

    // todo: could be zero-copy
    override fun load(fields: FieldSet<SELF, *>, values: Any?): Struct<SELF> =
            schema { builder -> fill(builder, this, fields, values) }

    override fun fields(value: Struct<SELF>): FieldSet<SELF, FieldDef<SELF, *, *>> =
        schema.allFieldSet

    override fun store(value: Struct<SELF>): Any? =
            value.fieldValues()

    // names

    inline fun <R> fieldByName(
            name: CharSequence,
            ifFound: (FieldDef<SELF, *, *>) -> R,
            ifNot: () -> R
    ): R {
        val idx = indexByName(name)
        return if (idx >= 0) ifFound(fieldAt(idx)) else ifNot()
    }

    @PublishedApi internal fun indexByName(name: CharSequence): Int {
        val array = namesTypesDefaults()
        for (i in 0 .. array.size step 3)
            if ((array[i] as CharSequence).eq(name, false))
                return i/3
        return -1
    }

    // defaults

    inline fun <T> defaultOrElse(field: FieldDef<SELF, T, *>, orElse: () -> T): T =
        namesTypesDefaults()[3 * field.ordinal + 2].let { def ->
            @Suppress("UNCHECKED_CAST")
            if (def !== Unset) def as T else orElse()
        }

    // etc

    @PublishedApi internal fun namesTypesDefaults() =
        (tmpFields as? Array<out Any?>) ?: initialize().let { tmpFields as Array<out Any?> }

    inline val FieldDef<SELF, *, *>.name: CharSequence get() = nameAt(ordinal)
    inline val MutableField<SELF, *, *>.name: CharSequence get() = nameAt(ordinal)
    inline val ImmutableField<SELF, *, *>.name: CharSequence get() = nameAt(ordinal)
    inline val Named<SELF>.name: CharSequence get() = name(this as SELF)  // @implNote:
    // (Mutable|Immutable)Field(Def) gonna call our `nameAt()`, custom lenses just return a stored value

    @PublishedApi internal fun nameAt(ordinal: Int) =
        namesTypesDefaults()[3 * ordinal] as CharSequence

    inline val <T, DT : DataType<T>> FieldDef<SELF, T, DT>.type: DT get() = typeAt(ordinal)
    inline val <T, DT : DataType<T>> MutableField<SELF, T, DT>.type: DT get() = typeAt(ordinal)
    inline val <T, DT : DataType<T>> ImmutableField<SELF, T, DT>.type: DT get() = typeAt(ordinal)

    @Suppress("UNCHECKED_CAST") @PublishedApi internal fun <T, DT : DataType<T>> typeAt(ordinal: Int) =
        namesTypesDefaults()[3 * ordinal + 1] as DT

}


interface Named<SCH : Schema<SCH>> {
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
inline class FieldDef<SCH : Schema<SCH>, T, DT : DataType<T>> @PublishedApi internal constructor(
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
         *
         * {@implNote Layout: 0000 0000 0000 000m 00yy yyyy 00xx xxxx, where
         *   xxxxxx is ordinal,
         *   yyyyyy is special (mutable of immutable) ordinal, and
         *   m is mutability flag; m = 0x1_0000 = 65_536.
         * }
         */
        val value: Int
) : NamedLens<SCH, PartialStruct<SCH>, Struct<SCH>, T, DT> {
    override fun name(mySchema: SCH): CharSequence = mySchema.run { this@FieldDef.name }
    override fun type(mySchema: SCH): DT = mySchema.run { this@FieldDef.type }
    override fun hasValue(struct: PartialStruct<SCH>): Boolean = this in struct.fields
    override fun ofPartial(partial: PartialStruct<SCH>): T? = if (this in partial.fields) partial.getOrThrow(this) else null
    override fun invoke(struct: Struct<SCH>): T = struct[this]
    override val size: Int get() = 1
    override fun get(index: Int): NamedLens<*, *, *, *, *> = getThisAt(index)
    override fun hashCode(): Int = this.value
    override fun equals(other: Any?): Boolean = fieldsEq(value, other)
    override fun toString(): String = fieldToString(value)
}

inline val FieldDef<*, *, *>.ordinal: Int
    get() = value and 63

@PublishedApi internal inline val FieldDef<*, *, *>.specialOrdinal: Int
    get() = (value shr 8) and 63


/**
 * Represents a mutable field of a [Struct]: its value can be changed.
 */
inline class MutableField<SCH : Schema<SCH>, T, DT : DataType<T>> @PublishedApi internal constructor(
    val value: Int // same layout as FieldDef
) : NamedLens<SCH, PartialStruct<SCH>, Struct<SCH>, T, DT> {
    override fun name(mySchema: SCH): CharSequence = mySchema.run { this@MutableField.name }
    override fun type(mySchema: SCH): DT = mySchema.run { this@MutableField.type }
    override fun hasValue(struct: PartialStruct<SCH>): Boolean = this in struct.fields
    override fun ofPartial(partial: PartialStruct<SCH>): T? = if (this in partial.fields) partial.getOrThrow(this) else null
    override fun invoke(struct: Struct<SCH>): T = struct[this]
    override val size: Int get() = 1
    override fun get(index: Int): NamedLens<*, *, *, *, *> = getThisAt(index)
    override fun hashCode(): Int = this.value
    override fun equals(other: Any?): Boolean = fieldsEq(value, other)
    override fun toString(): String = fieldToString(value)
}

inline val MutableField<*, *, *>.ordinal: Int
    get() = value and 63

inline val MutableField<*, *, *>.mutableOrdinal: Int
    get() = (value shr 8) and 63

inline fun <SCH : Schema<SCH>, T, DT : DataType<T>> MutableField<SCH, T, DT>.upcast(): FieldDef<SCH, T, DT> =
    FieldDef(this.value)

/**
 * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
 */
inline class ImmutableField<SCH : Schema<SCH>, T, DT : DataType<T>> @PublishedApi internal constructor(
    val value: Int // same layout as FieldDef
) : NamedLens<SCH, PartialStruct<SCH>, Struct<SCH>, T, DT> {
    override fun name(mySchema: SCH): CharSequence = mySchema.run { this@ImmutableField.name }
    override fun type(mySchema: SCH): DT = mySchema.run { this@ImmutableField.type }
    override fun hasValue(struct: PartialStruct<SCH>): Boolean = this in struct.fields
    override fun ofPartial(partial: PartialStruct<SCH>): T? = if (this in partial.fields) partial.getOrThrow(this) else null
    override fun invoke(struct: Struct<SCH>): T = struct[this]
    override val size: Int get() = 1
    override fun get(index: Int): NamedLens<*, *, *, *, *> = getThisAt(index)
    override fun hashCode(): Int = this.value
    override fun equals(other: Any?): Boolean = fieldsEq(value, other)
    override fun toString(): String = fieldToString(value)
}

inline val ImmutableField<*, *, *>.ordinal: Int
    get() = value and 63

inline val ImmutableField<*, *, *>.immutableOrdinal: Int
    get() = (value shr 8) and 63

inline fun <SCH : Schema<SCH>, T, DT : DataType<T>> ImmutableField<SCH, T, DT>.upcast(): FieldDef<SCH, T, DT> =
    FieldDef(this.value)

inline fun <R> FieldDef<*, *, *>.foldOrdinal(
    ifMutable: (mutableOrdinal: Int) -> R,
    ifImmutable: (immutableOrdinal: Int) -> R
): R =
    if ((value and 65536) != 0) ifMutable(specialOrdinal)
    else ifImmutable(specialOrdinal)

inline fun <SCH : Schema<SCH>, T, DT : DataType<T>, R> FieldDef<SCH, T, DT>.foldField(
    ifMutable: (MutableField<SCH, T, DT>) -> R,
    ifImmutable: (ImmutableField<SCH, T, DT>) -> R
): R =
    if ((value and 65536) != 0) ifMutable(MutableField(value))
    else ifImmutable(ImmutableField(value))


@JvmSynthetic internal fun NamedLens<*, *, *, *, *>.getThisAt(index: Int): NamedLens<*, *, *, *, *> =
    if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

@JvmSynthetic internal fun fieldToString(value: Int): String {
    // value = ordinal or (specialOrdinal shl 8) or (if (mutable) 65536 else 0)
    val special = (value shr 8) and 63
    return StringBuilder("field(#").append(value and 63).append(' ')
        .append(if ((value and 65536) == 0) "let" else "mut").append('#').append(special)
        .append(')')
        .toString()
}
@JvmSynthetic internal fun fieldsEq(value: Int, other: Any?) =
    (other is FieldDef<*, *, *> && other.value == value)
        || (other is MutableField<*, *, *> && other.value == value)
        || (other is ImmutableField<*, *, *> && other.value == value)

@JvmField @JvmSynthetic @PublishedApi internal val Unset = Any()
