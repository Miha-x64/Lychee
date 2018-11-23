package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.persistence.TransactionalPropertyStruct


@PublishedApi internal class RecordTransactionalAdapter<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val record: REC
) : TransactionalPropertyStruct<SCH> {

    override val schema: SCH
        get() = record.schema

    override fun <T> get(field: FieldDef<SCH, T>): T =
            record[field]

    // places for immutable fields remain nulls,
    // places for mutable are occupied by our wrappers
    // TODO: smaller array size :)
    private val props = arrayOfNulls<TransactionalProperty<StructTransaction<SCH>, *>>(record.schema.fields.size)

    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T> {
        val index = field.ordinal.toInt()
        return (props[index] as? ManagedProperty<SCH, StructTransaction<SCH>, T>)
                ?: ManagedProperty(manager, field, Unset as T).also { props[index] = it }
    }

    private val manager = object : Manager<SCH, StructTransaction<SCH>>() {

        override fun <T> getClean(field: FieldDef.Mutable<SCH, T>, id: Long): T =
                record[field]

        override fun <T> set(transaction: StructTransaction<SCH>, field: FieldDef.Mutable<SCH, T>, id: Long, update: T) {
            transaction.set(field, update)
        }

    }

    override fun beginTransaction(): StructTransaction<SCH> = object : StructTransaction<SCH> {

        private val transaction = record.session.beginTransaction()

        override fun <T> set(field: FieldDef.Mutable<SCH, T>, update: T) =
                record.prop(field).setValue(transaction, update)

        override fun setSuccessful() {
            transaction.setSuccessful()
        }

        override fun close() {
            transaction.close()
        }

    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>
        REC.transactional(): TransactionalPropertyStruct<SCH> =
        RecordTransactionalAdapter(this)
