package net.aquadc.properties.persistence

import net.aquadc.persistence.struct.*
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.function.identity
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

    /**
     * Constructs a new observable struct filled with default values.
     */
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
    }

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
     * Updates all [fields] with values from [source].
     */
    fun setFrom(source: Struct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *>>) {
        schema.forEach(fields) {
            mutateFrom(source, it) // capture type
        }
    }
    private inline fun <T> mutateFrom(source: Struct<SCH>, field: FieldDef.Mutable<SCH, T>) {
        this[field] = source[field]
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> prop(field: FieldDef.Mutable<SCH, T>) =
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

    private val values = arrayOfNulls<TransactionalProperty<StructTransaction<SCH>, *>>(observable.schema.fields.size)

    override fun <T> get(field: FieldDef<SCH, T>): T =
            observable[field]

    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T> {
        val index = field.ordinal.toInt()
        val prop = values[index] ?: (observable prop field).transactional<SCH, T>().also { values[index] = it }
        return (prop as TransactionalProperty<StructTransaction<SCH>, T>)
    }

    override fun beginTransaction(): StructTransaction<SCH> = object : PropStructTransaction<SCH>(this) {

        override fun close() {
            when (successful) {
                true -> Unit // nothing to do here
                false -> error("Oops... rollback is not supported") // TODO
                null -> error("attempting to close an already closed transaction")
            }
            successful = null
        }

    }

    private fun <SCH : Schema<SCH>, T> MutableProperty<T>
            .transactional(): TransactionalProperty<StructTransaction<SCH>, T> =
            object : `Mapped-`<T, T>(this@transactional, identity(), InPlaceWorker), TransactionalProperty<StructTransaction<SCH>, T> {
                override fun setValue(transaction: StructTransaction<SCH>, value: T) {
                    this@transactional.value = value
                }
            }

}
