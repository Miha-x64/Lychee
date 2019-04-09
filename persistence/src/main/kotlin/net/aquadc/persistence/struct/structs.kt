@file:JvmName("Structs")
@file:Suppress("NOTHING_TO_INLINE")

package net.aquadc.persistence.struct

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


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

/**
 * Calls [block] inside a transaction to mutate [this].
 * Passes [SCH] as a receiver, so you can shorten `Schema.Field` as `Field`.
 * Passes [StructTransaction] as first parameter, so you can write `it[[FieldDef]] = newValue`.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
@UseExperimental(ExperimentalContracts::class)
inline fun <SCH : Schema<SCH>, R> TransactionalStruct<SCH>.transaction(block: SCH.(StructTransaction<SCH>) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
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
