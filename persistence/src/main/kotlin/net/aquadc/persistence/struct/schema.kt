@file:[
    JvmName("Schemas")
    Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
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

    // region values changed during subclass initialization

    // if ArrayList, a temporary list of Field internals; if Array, a final list of 'em
    private var fieldInternals: Any = ArrayList<Any?>(16/*=round(name*type*def*avg.6fields)*/)

    // if ArrayList, a temporary list of Field instances; if Array, a final list of 'em
    private var fieldInstances: Any = ArrayList<FieldDef<SELF, *, *>>(8/*=round(avg.6fields)*/)

    // Written during subclass initialization, conforms FieldSet format
    private var mutableFieldBits = 0L

    // endregion values changed during subclass initialization


    // region initialization interface for subclasses

    /**
     * Creates, remembers, and returns a new mutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    // wannabe inline but https://youtrack.jetbrains.com/issue/KT-38827
    protected infix fun <T, DT : DataType<T>> CharSequence.mut(type: DT): MutableField<SELF, T, DT> =
            this.mut(type, Unset as T)

    /**
     * Creates, remembers, and returns a new mutable field definition with a default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
    protected fun <T, DT : DataType<T>> CharSequence.mut(dataType: DT, default: T): MutableField<SELF, T, DT> {
        val internals = fieldInternals()
        val fields = fieldInstances as ArrayList<FieldDef<SELF, *, *>>

        val mfb = mutableFieldBits
        val ord = fields.size
        val field = MutableField<SELF, T, DT>(ord, java.lang.Long.bitCount(mfb))
        mutableFieldBits = mfb or (1L shl ord)

        internals.add(this)
        internals.add(dataType)
        internals.add(default)
        fields.add(field)
        return field
    }

    /**
     * Creates, remembers, and returns a new immutable field definition without default value.
     * Don't call this conditionally,
     * otherwise [Struct]s with different instances of this [Schema] will become incompatible.
     */
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
        val internals = fieldInternals()
        val fields = fieldInstances as ArrayList<FieldDef<SELF, *, *>>

        val total = fields.size
        val field = ImmutableField<SELF, T, DT>(total, total - java.lang.Long.bitCount(mutableFieldBits))

        internals.add(this)
        internals.add(dataType)
        internals.add(default)
        fields.add(field)
        return field
    }

    private fun fieldInternals() =
        fieldInternals as? ArrayList<Any?>
            ?: throw IllegalStateException("schema `${javaClass.simpleName}` is already initialized")

    // endregion initialization interface for subclasses


    // region interface accessible during initialization

    fun fieldAt(ordinal: Int): FieldDef<SELF, *, *> = fieldInstances.arrOrAlAt(ordinal) as FieldDef<SELF, *, *>

    inline val FieldDef<SELF, *, *>.name: CharSequence get() = nameAt(ordinal)
    inline val Named<SELF>.name: CharSequence get() = name(this as SELF)  // @implNote:
    // (Mutable|Immutable)Field(Def) gonna call our `nameAt()`, custom lenses just return a stored value
    @PublishedApi internal fun nameAt(ordinal: Byte) = fieldInternals.arrOrAlAt(3 * ordinal) as CharSequence

    inline val <T, DT : DataType<T>> FieldDef<SELF, T, DT>.type: DT get() = typeAt(ordinal)
    @PublishedApi internal fun <T, DT : DataType<T>> typeAt(ordinal: Byte) =
        fieldInternals.arrOrAlAt(3 * ordinal + 1) as DT

    inline fun <T> defaultOrElse(field: FieldDef<SELF, T, *>, orElse: () -> T): T =
        defaultAt(field.ordinal.toInt()).let { def -> if (def !== Unset) def as T else orElse() }
    @PublishedApi internal fun defaultAt(ordinal: Int): Any? = fieldInternals.arrOrAlAt(3 * ordinal + 2)

    private fun Any.arrOrAlAt(index: Int): Any? {
        (this as? ArrayList<*>)?.let { return it[index] }
        return (this as Array<*>)[index]
    }

    // endregion interface accessible during initialization


    // region interface which ends initialization

    /**
     * A list of fields of this struct.
     */
    @Deprecated("use allFieldSet and fieldAt instead", level = DeprecationLevel.ERROR)
    val fields: Array<out FieldDef<SELF, *, *>>
        get() = throw AssertionError()

    /** A set of all fields of this [Schema]. */
    val allFieldSet: FieldSet<SELF, FieldDef<SELF, *, *>>
        get() = FieldSet(fields().size.let { size ->
            // (1L shl size) - 1   :  1L shl 64  will overflow to 1
            // -1L ushr (64 - size): -1L ushr 64 will remain -1L
            // the last one is okay, assuming that zero-field structs are prohibited
            -1L ushr (64 - size)
        })

    /** A set of all [MutableField]s of this [Schema]. */
    val mutableFieldSet: FieldSet<SELF, MutableField<SELF, *, *>>
        get() = FieldSet(fields().let { mutableFieldBits })

    /** A set of all [ImmutableField]s of this [Schema]. */
    val immutableFieldSet: FieldSet<SELF, ImmutableField<SELF, *, *>>
        get() = FieldSet(fields().let { (-1L ushr (64 - it.size)) and mutableFieldBits.inv() })

    inline fun <R> fieldByName(
            name: CharSequence,
            ifFound: (FieldDef<SELF, *, *>) -> R,
            ifNot: () -> R
    ): R {
        val idx = indexByName(name)
        return if (idx >= 0) ifFound(fieldAt(idx)) else ifNot()
    }
    @PublishedApi internal fun indexByName(name: CharSequence): Int {
        val array = (fieldInternals as? Array<out Any?>) ?: freeze().let { fieldInternals as Array<out Any?> }

        for (i in 0 .. array.size step 3)
            if ((array[i] as CharSequence).eq(name, false))
                return i/3
        return -1
    }

    private fun fields() = ((fieldInstances as? Array<FieldDef<SELF, *, *>>)
        ?: freeze().let { fieldInstances as Array<FieldDef<SELF, *, *>> })
    @Synchronized private fun freeze() {
        if (fieldInternals is Array<*>) return // initialized concurrently

        val fieldList = fieldInternals as ArrayList<Any?>
        check(fieldList.isNotEmpty()) { "Struct must have at least one field." }

        val fieldCount = fieldList.size / 3
        val nameSet = newSet<String>(fieldCount)
        for (i in fieldList.indices step 3) {
            val name = (fieldList[i] as CharSequence).toString()
            if (!nameSet.add(name)) {
                throw IllegalStateException("duplicate field: `${this@Schema.javaClass.simpleName}`.`${name}`")
            }
        }

        fieldInternals = fieldList.toArray()
        fieldInstances = (fieldInstances as ArrayList<FieldDef<SELF, *, *>>).toTypedArray()
    }

    // endregion interface which ends initialization


    // region Partial implementation

    // todo: could be zero-copy
    @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("load") // avoid having both `load-<hash>()` and `bridge load()`
    override fun load(fields: FieldSet<SELF, FieldDef<SELF, *, *>>, values: Any?): Struct<SELF> =
        schema { builder -> fill(builder, this, fields, values) }

    override fun fields(value: Struct<SELF>): FieldSet<SELF, FieldDef<SELF, *, *>> =
        schema.allFieldSet

    override fun store(value: Struct<SELF>): Any? =
        value.fieldValues()

    // endregion Partial implementation

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
sealed class FieldDef<SCH : Schema<SCH>, T, DT : DataType<T>>(
    ordinal: Int,
    specialOrdinal: Int,
    mutable: Boolean
) : NamedLens<SCH, PartialStruct<SCH>, Struct<SCH>, T, DT> {

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
    @JvmField @JvmSynthetic @PublishedApi internal val value: Int
    init {
        check(ordinal < 64) { "Ordinal must be in [0; 63], $ordinal given" }
        check(specialOrdinal < 64)
        value = ordinal or (specialOrdinal shl 8) or (if (mutable) 65536 else 0)
    }

    override fun name(mySchema: SCH): CharSequence =
        mySchema.nameAt(ordinal)

    override fun type(mySchema: SCH): DT =
        mySchema.typeAt(ordinal)

    override fun hasValue(struct: PartialStruct<SCH>): Boolean =
        this in struct.fields

    override fun ofPartial(partial: PartialStruct<SCH>): T? =
        if (this in partial.fields) partial.getOrThrow(this) else null

    override fun invoke(struct: Struct<SCH>): T =
        struct[this]

    override val size: Int
        get() = 1

    override fun get(index: Int): NamedLens<*, *, *, *, *> =
        if (index == 0) this else throw IndexOutOfBoundsException(index.toString())

    override fun hashCode(): Int =
        value

    override fun equals(other: Any?): Boolean =
        javaClass === other?.javaClass && value == (other as FieldDef<*, *, *>).value

    override fun toString(): String = StringBuilder("field(#").append(value and 63).append(' ')
        .append(if ((value and 65536) == 0) "let" else "mut").append('#').append((value shr 8) and 63)
        .append(')')
        .toString()
}

inline val FieldDef<*, *, *>.ordinal: Byte
    get() = (value and 63).toByte()

@PublishedApi internal inline val FieldDef<*, *, *>.specialOrdinal: Byte
    get() = ((value shr 8) and 63).toByte()


/**
 * Represents a mutable field of a [Struct]: its value can be changed.
 */
/*wannabe inline*/ class MutableField<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
    ordinal: Int, specialOrdinal: Int
) : FieldDef<SCH, T, DT>(ordinal, specialOrdinal, true)

inline val MutableField<*, *, *>.mutableOrdinal: Byte
    get() = ((value shr 8) and 63).toByte()

/**
 * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
 */
/*wannabe inline*/ class ImmutableField<SCH : Schema<SCH>, T, DT : DataType<T>> internal constructor(
    ordinal: Int, specialOrdinal: Int
) : FieldDef<SCH, T, DT>(ordinal, specialOrdinal, false)

inline val ImmutableField<*, *, *>.immutableOrdinal: Byte
    get() = ((value shr 8) and 63).toByte()

inline fun <R> FieldDef<*, *, *>.foldOrdinal(
    ifMutable: (mutableOrdinal: Int) -> R,
    ifImmutable: (immutableOrdinal: Int) -> R
): R =
    if ((value and 65536) != 0) ifMutable(specialOrdinal.toInt())
    else ifImmutable(specialOrdinal.toInt())

inline fun <SCH : Schema<SCH>, T, DT : DataType<T>, R> FieldDef<SCH, T, DT>.foldField(
    ifMutable: (MutableField<SCH, T, DT>) -> R,
    ifImmutable: (ImmutableField<SCH, T, DT>) -> R
): R = when (this) {
    is MutableField -> ifMutable(this)
    is ImmutableField -> ifImmutable(this)
}

@JvmField @JvmSynthetic @PublishedApi internal val Unset = Any()
