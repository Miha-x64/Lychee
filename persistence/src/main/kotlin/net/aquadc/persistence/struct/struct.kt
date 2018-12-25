package net.aquadc.persistence.struct

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents an instance of a struct —
 * a map with keys of type [FieldDef] (can be treated as [String] or [Byte])
 * and heterogeneous statically typed values.
 * @see Schema
 * @see FieldDef
 * @see TransactionalStruct
 * @see StructSnapshot — fully immutable implementation
 * @see net.aquadc.properties.persistence.ObservableStruct from `:properties` — observable implementation
 */
interface Struct<SCH : Schema<SCH>> {

    /**
     * Represents the type of this struct.
     */
    val schema: SCH

    /**
     * Returns the value of the requested field.
     */
    operator fun <T> get(field: FieldDef<SCH, T>): T

}


/**
 * A struct which can be mutated inside a transaction.
 */
interface TransactionalStruct<SCH : Schema<SCH>> : Struct<SCH> {
    fun beginTransaction(): StructTransaction<SCH>
}

/**
 * A transaction on a single [Struct] instance.
 */
interface StructTransaction<SCH : Schema<SCH>> : AutoCloseable {
    operator fun <T> set(field: FieldDef.Mutable<SCH, T>, update: T)
    fun setSuccessful()
}

/**
 * Updates all [fields] with values from [source].
 */
fun <SCH : Schema<SCH>> StructTransaction<SCH>.setFrom(
        source: Struct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *>>
) {
    source.schema.forEach(fields) {
        mutateFrom(source, it) // capture type
    }
}
private inline fun <SCH : Schema<SCH>, T> StructTransaction<SCH>.mutateFrom(source: Struct<SCH>, field: FieldDef.Mutable<SCH, T>) {
    this[field] = source[field]
}

abstract class SimpleStructTransaction<SCH : Schema<SCH>> : StructTransaction<SCH>, AutoCloseable {

    @JvmField protected var successful: Boolean? = false

    override fun setSuccessful() {
        successful = true
    }

}

/**
 * Calls [block] inside a transaction to mutate [this].
 */
@UseExperimental(ExperimentalContracts::class)
inline fun <SCH : Schema<SCH>, R> TransactionalStruct<SCH>.transaction(block: SCH.(StructTransaction<SCH>) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val transaction = beginTransaction()
    try {
        val r = schema.block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}
