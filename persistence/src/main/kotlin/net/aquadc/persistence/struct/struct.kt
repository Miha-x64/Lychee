package net.aquadc.persistence.struct

import net.aquadc.persistence.converter.Converter
import java.util.*
import java.util.Collections.unmodifiableList

/**
 * Declares a struct (or DTO).
 * `struct`s in C, Rust, Swift, etc, or `Object`s in JS, are similar
 * to final classes with only public fields, no methods and no supertypes.
 * @see Struct
 * @see FieldDef
 */
abstract class StructDef<STRUCT : Struct<STRUCT>>(
        val name: String
) {

    /**
     * A temporary list of [FieldDef]s used while [StructDef] is getting constructed.
     */
    private var tmpFields: ArrayList<FieldDef<STRUCT, *>>? = ArrayList()

    /**
     * A list of fields of this struct.
     *
     * {@implNote
     *   on concurrent access, we might null out [tmpFields] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val fields: List<FieldDef<STRUCT, *>> by lazy {
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
    protected open fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<STRUCT, *>>) { }

    private fun tmpFields() =
            tmpFields ?: throw IllegalStateException("table `$name` is already initialized")

    /**
     * Creates, remembers and returns a new mutable field definition.
     */
    protected infix fun <T> Converter<T>.mutable(name: String): FieldDef.Mutable<STRUCT, T> {
        val fields = tmpFields()
        val converter = this@mutable
        val col = FieldDef.Mutable(this@StructDef, name, converter, fields.size.toByte())
        fields.add(col)
        return col
    }

    /**
     * Creates, remembers and returns a new immutable field definition.
     */
    protected infix fun <T> Converter<T>.immutable(name: String): FieldDef.Immutable<STRUCT, T> {
        val fields = tmpFields()
        val converter = this@immutable
        val col = FieldDef.Immutable(this@StructDef, name, converter, fields.size.toByte())
        fields.add(col)
        return col
    }

}

/**
 * Represents an instance of a struct.
 * @see StructDef
 * @see FieldDef
 */
interface Struct<THIS : Struct<THIS>> {

    /**
     * Returns the value of the requested field.
     */
    fun <T> getValue(field: FieldDef<THIS, T>): T

}

/**
 * Struct field is a single key-value mapping. FieldDef represents a key with name and type.
 * Note: constructors are internal to guarantee correct [ordinal] values.
 * @see StructDef
 * @see Struct
 * @see Mutable
 * @see Immutable
 */
sealed class FieldDef<STRUCT : Struct<STRUCT>, T>(
        val structDef: StructDef<STRUCT>,
        val name: String,
        val converter: Converter<T>,
        val ordinal: Byte
) {

    init {
        check(ordinal < 64) { "Ordinal must be in [0..63], $ordinal given" }
    }

    override fun toString(): String = structDef.name + '.' + name

    /**
     * Represents a mutable field of a [Struct]: its value can be changed.
     */
    class Mutable<STRUCT : Struct<STRUCT>, T> internal constructor(
            structDef: StructDef<STRUCT>,
            name: String,
            converter: Converter<T>,
            ordinal: Byte
    ) : FieldDef<STRUCT, T>(structDef, name, converter, ordinal)

    /**
     * Represents an immutable field of a [Struct]: its value must be set during construction and cannot be changed.
     */
    class Immutable<STRUCT : Struct<STRUCT>, T> internal constructor(
            structDef: StructDef<STRUCT>,
            name: String,
            converter: Converter<T>,
            ordinal: Byte
    ) : FieldDef<STRUCT, T>(structDef, name, converter, ordinal)

}

internal inline fun <reified T> t(): Class<T> = T::class.java
