package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.persistence.TransactionalPropertyStruct


@PublishedApi internal class RecordTransactionalAdapter<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>>(
        private val record: REC
) : TransactionalPropertyStruct<TBL> {

    override val type: TBL
        get() = record.type

    override fun <T> get(field: FieldDef<TBL, T>): T =
            record[field]

    // places for immutable fields remain nulls,
    // places for mutable are occupied by our wrappers
    // TODO: smaller array size :)
    private val props = arrayOfNulls<TransactionalProperty<StructTransaction<TBL>, *>>(record.type.fields.size)

    override fun <T> prop(field: FieldDef.Mutable<TBL, T>): TransactionalProperty<StructTransaction<TBL>, T> {
        val index = field.ordinal.toInt()
        return (props[index] as? ManagedProperty<TBL, StructTransaction<TBL>, T>)
                ?: ManagedProperty(manager, field, Unset as T).also { props[index] = it }
    }

    private val manager = object : Manager<TBL, StructTransaction<TBL>>() {

        override fun <T> getClean(field: FieldDef.Mutable<TBL, T>, id: Long): T =
                record[field]

        override fun <T> set(transaction: StructTransaction<TBL>, field: FieldDef.Mutable<TBL, T>, id: Long, update: T) {
            transaction.set(field, update)
        }

    }

    override fun beginTransaction(): StructTransaction<TBL> = object : StructTransaction<TBL> {

        private val transaction = record.session.beginTransaction()

        override fun <T> set(field: FieldDef.Mutable<TBL, T>, update: T) =
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
inline fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>>
        REC.transactional(): TransactionalPropertyStruct<TBL> =
        RecordTransactionalAdapter(this)
