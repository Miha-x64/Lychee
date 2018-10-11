package net.aquadc.persistence.struct

import net.aquadc.persistence.type.DataType
import java.util.*
import java.util.Collections.unmodifiableList

/**
 * Declares a struct (or DTO).
 * `struct`s in C, Rust, Swift, etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * @see Struct
 * @see FieldDef
 */
abstract class StructDef<SELF : StructDef<SELF>>(
        val name: String
) {

    /**
     * A temporary list of [FieldDef]s used while [StructDef] is getting constructed.
     */
    private var tmpFields: ArrayList<FieldDef<SELF, *>>? = ArrayList()

    /**
     * A list of fields of this struct.
     *
     * {@implNote
     *   on concurrent access, we might null out [tmpFields] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val fields: List<FieldDef<SELF, *>> by lazy {
        val fields = tmpFields()
        check(fields.isNotEmpty()) { "Struct must have at least one field." }
        //          ^ note: isManaged also relies on the fact that any struct has at least one field.

        val nameSet = HashSet<String>()
        for (i in fields.indices) {
            val col = fields[i]
            if (!nameSet.add(col.name)) {
                throw IllegalStateException("duplicate column: `$name`.`${col.name}`")
            }
        }

        val frozen = unmodifiableList(fields)
        beforeFreeze(nameSet, frozen)
        tmpFields = null
        frozen
    }

    /**
     * Gets called before this fully initialized structDef gets used for the first time.
     */
    protected open fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<SELF, *>>) { }

    private fun tmpFields() =
            tmpFields ?: throw IllegalStateException("table `$name` is already initialized")

    /**
     * Creates, remembers and returns a new mutable field definition without default value.
     */
    @Suppress("UNCHECKED_CAST")
    protected infix fun <T> DataType<T>.mutable(name: String): FieldDef.Mutable<SELF, T> =
            mutable(name, Unset as T)

    /**
     * Creates, remembers and returns a new mutable field definition with default value.
     */
    protected fun <T> DataType<T>.mutable(name: String, default: T): FieldDef.Mutable<SELF, T> {
        val fields = tmpFields()
        val converter = this@mutable
        val col = FieldDef.Mutable(this@StructDef, name, converter, fields.size.toByte(), default)
        fields.add(col)
        return col
    }

    /**
     * Creates, remembers and returns a new immutable field definition.
     */
    protected infix fun <T> DataType<T>.immutable(name: String): FieldDef.Immutable<SELF, T> {
        val fields = tmpFields()
        val converter = this@immutable
        val col = FieldDef.Immutable(this@StructDef, name, converter, fields.size.toByte())
        fields.add(col)
        return col
    }

}

/**
 * Represents an instance of a struct —
 * a heterogeneous statically typed map with [String] keys.
 * @see StructDef
 * @see FieldDef
 */
interface Struct<DEF : StructDef<DEF>> {

    /**
     * Represents the type of this struct.
     */
    val type: StructDef<DEF>

    /**
     * Returns the value of the requested field.
     * fixme: rename to operator get
     */
    fun <T> getValue(field: FieldDef<DEF, T>): T

}

/**
 * Struct field is a single key-value mapping. FieldDef represents a key with name and type.
 * Note: constructors are internal to guarantee correct [ordinal] values.
 * @see StructDef
 * @see Struct
 * @see Mutable
 * @see Immutable
 * TODO: replace with inline-class wrapping Byte
 */
sealed class FieldDef<DEF : StructDef<DEF>, T>(
        val structDef: StructDef<DEF>,
        val name: String,
        val type: DataType<T>,
        val ordinal: Byte,
        default: T
) {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    private val _default = default

    val default: T
        get() = if (_default === Unset) throw NoSuchElementException() else _default

    override fun toString(): String = structDef.name + '.' + name

    /**
     * Represents a mutable field of a [Struct]: its value can be changed.
     */
    class Mutable<DEF : StructDef<DEF>, T> internal constructor(
            structDef: StructDef<DEF>,
            name: String,
            converter: DataType<T>,
            ordinal: Byte,
            default: T
    ) : FieldDef<DEF, T>(structDef, name, converter, ordinal, default)

    /**
     * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
     */
    @Suppress("UNCHECKED_CAST")
    class Immutable<DEF : StructDef<DEF>, T> internal constructor(
            structDef: StructDef<DEF>,
            name: String,
            converter: DataType<T>,
            ordinal: Byte
    ) : FieldDef<DEF, T>(structDef, name, converter, ordinal, Unset as T)

}

private val Unset = Any()
