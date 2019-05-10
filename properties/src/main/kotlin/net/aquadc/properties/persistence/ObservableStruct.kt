package net.aquadc.properties.persistence

import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.SimpleStructTransaction
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.intersectMutable
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.function.identity
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.`Mapped-`
import net.aquadc.properties.propertyOf

/**
 * Implements a mutable, observable, in-memory [Struct].
 * This is a reference implementation:
 * * every struct should have
 *   * a constructor copying from [Struct]
 * * every mutable observable [Struct] implementation should have
 *   * [set], [setFrom] mutators
 *   * a property getter [prop]
 */
class ObservableStruct<SCH : Schema<SCH>> : BaseStruct<SCH>, PropertyStruct<SCH> {

    private val values: Array<Any?>

    constructor(source: Struct<SCH>, concurrent: Boolean) : super(source.schema) {
        val fields = schema.fields
        values = Array(fields.size) { i ->
            val field = fields[i]
            val value = source[field]
            when (field) {
                is FieldDef.Mutable<SCH, *> -> propertyOf(value, concurrent)
                is FieldDef.Immutable<SCH, *> -> value
            }
        }
    }

    /*
     * Constructs a new observable struct filled with default values.
     * unsafe
    constructor(type: SCH, concurrent: Boolean) : super(type) {
        val fields = type.fields
        values = Array(fields.size) { i ->
            val field = fields[i]
            val value = field.default
            when (field) {
                is FieldDef.Mutable<SCH, *> -> propertyOf(value, concurrent)
                is FieldDef.Immutable<SCH, *> -> value
            }
        }
    }*/

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<SCH, T>): T {
        val value = values[field.ordinal.toInt()]
        return when (field) {
            is FieldDef.Mutable<SCH, T> -> (value as MutableProperty<T>).value
            is FieldDef.Immutable<SCH, T> -> value as T
        }
    }

    operator fun <T> set(field: FieldDef.Mutable<SCH, T>, value: T) {
        prop(field).value = value
    }

    /**
     * Updates field values from [source].
     * @return a set of updated fields
     *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
     */
    fun setFrom(
            source: PartialStruct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *>>
    ): FieldSet<SCH, FieldDef.Mutable<SCH, *>> =
            source.fields.intersectMutable(fields).also { intersect ->
                schema.forEach(fields) { field ->
                    mutateFrom(source, field) // capture type
                }
            }
    @Suppress("NOTHING_TO_INLINE")
    private inline fun <T> mutateFrom(source: PartialStruct<SCH>, field: FieldDef.Mutable<SCH, T>) {
        this[field] = source.getOrThrow(field)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): MutableProperty<T> =
            (values[field.ordinal.toInt()] as MutableProperty<T>)

    /**
     * A bridge between [ObservableStruct] and [TransactionalPropertyStruct].
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun transactional(): TransactionalPropertyStruct<SCH> =
            ObservableTransactionalAdapter(this)

}

@PublishedApi internal class ObservableTransactionalAdapter<SCH : Schema<SCH>>(
        @JvmField @JvmSynthetic internal val observable: ObservableStruct<SCH>
) : BaseStruct<SCH>(observable.schema), TransactionalPropertyStruct<SCH> {

    private val props = arrayOfNulls<TransactionalProperty<StructTransaction<SCH>, *>>(observable.schema.mutableFields.size)

    override fun <T> get(field: FieldDef<SCH, T>): T =
            observable[field]

    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T> {
        val index = field.mutableOrdinal.toInt()
        val prop = props[index] ?: observable.transactional(field).also { props[index] = it }
        return (prop as TransactionalProperty<StructTransaction<SCH>, T>)
    }

    override fun beginTransaction(): StructTransaction<SCH> = object : SimpleStructTransaction<SCH>() {

        private val patch = Array<Any?>(schema.mutableFields.size) { Unset }

        override fun <T> set(field: FieldDef.Mutable<SCH, T>, update: T) {
            patch[field.mutableOrdinal.toInt()] = update
        }

        override fun close() {
            when (successful) {
                true -> {
                    val fields = schema.mutableFields
                    for (i in fields.indices) {
                        if (patch[i] !== Unset) {
                            (observable.prop(fields[i]) as MutableProperty<Any?>).value = patch[i]
                        }
                    }
                }
                false -> Unit // nothing to do here
                null -> error("attempting to close an already closed transaction")
            }
            successful = null
        }

    }

    private fun <T> ObservableStruct<SCH>
            .transactional(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T> =
            object : `Mapped-`<T, T>(this@transactional prop field, identity(), InPlaceWorker), TransactionalProperty<StructTransaction<SCH>, T> {
                override fun setValue(transaction: StructTransaction<SCH>, value: T) {
                    transaction[field] = value
                }
            }

}
