package net.aquadc.properties.sql

import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.executor.InPlaceWorker
import net.aquadc.properties.function.identity
import net.aquadc.properties.internal.`Mapped-`
import net.aquadc.properties.persistence.TransactionalPropertyStruct


@PublishedApi internal class RecordTransactionalAdapter<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        @JvmField @JvmSynthetic internal val record: REC
) : BaseStruct<SCH>(record.schema), TransactionalPropertyStruct<SCH> {

    override fun <T> get(field: FieldDef<SCH, T, *>): T =
            record[field]

    private val props = arrayOfNulls<TransactionalProperty<StructTransaction<SCH>, *>>(record.schema.mutableFields.size)

    override fun <T> prop(field: FieldDef.Mutable<SCH, T, *>): TransactionalProperty<StructTransaction<SCH>, T> {
        val index = field.mutableOrdinal.toInt()
        return (props[index] as TransactionalProperty<StructTransaction<SCH>, T>?)
                ?: record.prop(field).transactional(field).also { props[index] = it }
    }

    override fun beginTransaction(): StructTransaction<SCH> = object : StructTransaction<SCH> {

        private val transaction = record._session.beginTransaction()

        override fun <T> set(field: FieldDef.Mutable<SCH, T, *>, update: T) =
                record.prop(field).setValue(transaction, update)

        override fun setSuccessful() {
            transaction.setSuccessful()
        }

        override fun close() {
            transaction.close()
        }

    }

    private fun <SCH : Schema<SCH>, T> SqlProperty<T>
            .transactional(field: FieldDef.Mutable<SCH, T, *>): TransactionalProperty<StructTransaction<SCH>, T> =
            object : `Mapped-`<T, T>(this@transactional, identity(), InPlaceWorker), TransactionalProperty<StructTransaction<SCH>, T> {
                override fun setValue(transaction: StructTransaction<SCH>, value: T) {
                    transaction.set<T>(field, value)
                }
            }

}

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>
        REC.transactional(): TransactionalPropertyStruct<SCH> =
        RecordTransactionalAdapter(this)
