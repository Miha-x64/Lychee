@file:JvmName("Structs")

package net.aquadc.persistence.struct

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Updates field values from [source].
 * @return a set of updated fields
 *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
 */
fun <SCH : Schema<SCH>> StructTransaction<SCH>.setFrom(
        source: PartialStruct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *, *>>
        /* default value for [fields] may be mutableFieldSet(), but StructBuilder's default is different */
): FieldSet<SCH, FieldDef.Mutable<SCH, *, *>> =
        source.fields.intersect(fields).also { intersect ->
            source.schema.forEach(intersect) { field ->
                mutateFrom(source, field) // capture type
            }
        }
@Suppress("NOTHING_TO_INLINE")
private inline fun <SCH : Schema<SCH>, T> StructTransaction<SCH>.mutateFrom(
        source: PartialStruct<SCH>, field: FieldDef.Mutable<SCH, T, *>
) {
    this[field] = source.getOrThrow(field)
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

@PublishedApi
internal class Getter<SCH : Schema<SCH>, T>(
    private val struct: Struct<SCH>,
    private val field: FieldDef<SCH, T, *>
) : () -> T {

    override fun invoke(): T =
        struct[field]

}

/**
 * Creates a getter applied to [this] [SCH],
 * i. e. a function which returns a value of a pre-set [field] of a pre-set (struct)[this].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, T> Struct<SCH>.getterOf(field: FieldDef<SCH, T, *>): () -> T =
    Getter(this, field)
