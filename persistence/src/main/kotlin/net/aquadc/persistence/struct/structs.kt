@file:[JvmName("Structs") Suppress("NOTHING_TO_INLINE")]
package net.aquadc.persistence.struct

import net.aquadc.persistence.type.DataType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Updates field values from [source].
 * @return a set of updated fields
 *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
 */
fun <SCH : Schema<SCH>> StructTransaction<SCH>.setFrom(
        source: PartialStruct<SCH>, fields: FieldSet<SCH, MutableField<SCH, *, *>>
/* default value for [fields] could be mutableFieldSet() but StructBuilder's default is different, so don't disappoint */
): FieldSet<SCH, MutableField<SCH, *, *>> =
        source.fields.intersect(fields).also { intersect ->
            source.schema.forEach_(intersect) { field ->
                mutateFrom(source, field) // capture type
            }
        }
private inline fun <SCH : Schema<SCH>, T> StructTransaction<SCH>.mutateFrom(
        source: PartialStruct<SCH>, field: MutableField<SCH, T, *>
) {
    this[field] = source.getOrThrow(field)
}

/**
 * Calls [block] inside a transaction to mutate [this].
 * Passes [SCH] as a receiver, so you can shorten `Schema.Field` as `Field`.
 * Passes [StructTransaction] as first parameter, so you can write `it[[FieldDef]] = newValue`.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
@OptIn(ExperimentalContracts::class)
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
inline fun <SCH : Schema<SCH>, T> Struct<SCH>.getterOf(field: FieldDef<SCH, T, *>): () -> T =
    Getter(this, field)

// pass-through adapters for (im)mutable fields

inline operator fun <SCH : Schema<SCH>, T> Struct<SCH>.get(field: MutableField<SCH, T, *>): T =
    get((field as MutableField<SCH, T, out DataType<T>>).upcast())

inline operator fun <SCH : Schema<SCH>, T> Struct<SCH>.get(field: ImmutableField<SCH, T, *>): T =
    get((field as ImmutableField<SCH, T, out DataType<T>>).upcast())

inline fun <SCH : Schema<SCH>, T> PartialStruct<SCH>.getOrThrow(field: MutableField<SCH, T, *>): T =
    getOrThrow((field as MutableField<SCH, T, out DataType<T>>).upcast())

inline fun <SCH : Schema<SCH>, T> PartialStruct<SCH>.getOrThrow(field: ImmutableField<SCH, T, *>): T =
    getOrThrow((field as ImmutableField<SCH, T, out DataType<T>>).upcast())
