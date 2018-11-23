package net.aquadc.properties.persistence

import net.aquadc.persistence.struct.*
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.propertyOf

/**
 * Implements a mutable, observable, in-memory [Struct].
 * This is a reference implementation:
 * * every struct should have
 *   * a constructor copying from [Struct]
 * * every mutable observable [Struct] implementation should have
 *   * a mutator [set]
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

    private val manager = object : Manager<SCH, StructTransaction<SCH>>() {

        override fun <T> getClean(field: FieldDef.Mutable<SCH, T>, id: Long): T =
                get(field)

        override fun <T> set(transaction: StructTransaction<SCH>, field: FieldDef.Mutable<SCH, T>, id: Long, update: T) {
            (observable prop field).value = update
        }

    }

    private val values = observable.schema.fields.map {
        when (it) {
            is FieldDef.Mutable -> ManagedProperty(manager, it as FieldDef.Mutable<SCH, Any?>, net.aquadc.properties.internal.Unset)
            is FieldDef.Immutable -> observable[it]
        }
    }

    override fun <T> get(field: FieldDef<SCH, T>): T {
        val index = field.ordinal.toInt()
        return when (field) {
            is FieldDef.Mutable -> (values[index] as Property<T>).value
            is FieldDef.Immutable -> values[index] as T
        }
    }

    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T> =
            (values[field.ordinal.toInt()] as TransactionalProperty<StructTransaction<SCH>, T>)

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

}
