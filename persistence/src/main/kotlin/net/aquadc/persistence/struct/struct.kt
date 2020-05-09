package net.aquadc.persistence.struct

import java.io.Closeable

/**
 * Represents an instance of a struct /strʌkt/ —
 * a map with [FieldDef] keys (can be treated as [String] or [Byte])
 * and heterogeneous statically typed values, which has a value for each key.
 * @see Schema
 * @see build
 * @see FieldDef
 * @see TransactionalStruct
 * @see StructSnapshot — fully immutable implementation
 * @see net.aquadc.properties.persistence.ObservableStruct from `:properties` — observable implementation
 */
interface Struct<SCH : Schema<SCH>> : PartialStruct<SCH> {

    /**
     * Always returns a full set of fields declared in schema.
     */
    override val fields: FieldSet<SCH, FieldDef<SCH, *, *>>
        get() = schema.allFieldSet()

    /**
     * Returns the value of the requested field.
     */
    operator fun <T> get(field: FieldDef<SCH, T, *>): T

    override fun <T> getOrThrow(field: FieldDef<SCH, T, *>): T =
            get(field)

}


/**
 * A struct which can be mutated inside a transaction.
 */
interface TransactionalStruct<SCH : Schema<SCH>> : Struct<SCH> {

    /**
     * Creates and begins new transaction — an act of atomic, consistent, isolated mutation.
     */
    fun beginTransaction(): StructTransaction<SCH>
}

/**
 * A transaction on a single [Struct] instance.
 */
interface StructTransaction<SCH : Schema<SCH>> : Closeable {

    /**
     * Sets [field] value to [update].
     */
    operator fun <T> set(field: FieldDef.Mutable<SCH, T, *>, update: T)

    /**
     * Marks the whole transaction as successful.
     * Leads to actual data changes.
     * This instance becomes unusable then.
     */
    fun setSuccessful()
}

abstract class SimpleStructTransaction<SCH : Schema<SCH>> : StructTransaction<SCH>, Closeable {

    @JvmField protected var successful: Boolean? = false

    override fun setSuccessful() {
        successful = true
    }

}
