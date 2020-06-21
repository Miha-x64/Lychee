package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.MutableField
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.foldField
import net.aquadc.persistence.struct.foldOrdinal
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.mapIndexed
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.persistence.PropertyStruct


/**
 * Represents an active record — a container with some values and properties backed by an RDBMS row.
 */
open class Record<SCH : Schema<SCH>, ID : IdBound>
/**
 * Creates new record.
 * Note that such a record is managed and alive (will receive updates) only if created by [Dao].
 */
internal constructor(
        internal val table: Table<SCH, ID>,
        private val session: Session<*>,
        val primaryKey: ID
) : BaseStruct<SCH>(table.schema), PropertyStruct<SCH> {

    internal val _session get() = session

    @Suppress("UNCHECKED_CAST")
    internal val dao: Dao<SCH, ID>
        get() = session[table as Table<SCH, ID>]

    internal fun copyValues(): Array<Any?> =
        schema.mapIndexed(schema.allFieldSet) { _, f -> get(f) }

    override fun <T> get(field: FieldDef<SCH, T, *>): T = (field as FieldDef<SCH, T, DataType<T>>).foldField(
        ifMutable = { prop(it).value },
        ifImmutable = {
            val index = field.ordinal
            val value = values[index]

            if (value === Unset) {
                @Suppress("UNCHECKED_CAST")
                val freshValue = dao.getClean(field, primaryKey)
                values[index] = freshValue
                freshValue
            } else value as T
        }
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T> prop(field: MutableField<SCH, T, *>): SqlProperty<T> =
            values[field.ordinal.toInt()] as SqlProperty<T>

    @JvmField @JvmSynthetic @Suppress("UNCHECKED_CAST")
    internal val values: Array<Any?/* = ManagedProperty<Transaction, T> | T */> =
            session[table as Table<SCH, ID>].let { dao ->
                schema.mapIndexed(fields) { i, field ->
                    field.foldOrdinal(
                        ifMutable = { ManagedProperty(dao, field as FieldDef<SCH, Any?, DataType<Any?>>, primaryKey, Unset) },
                        ifImmutable = { Unset }
                    )
                }
            }

    var isManaged: Boolean = true
        @JvmSynthetic internal set // cleared **before** real property unmanagement occurs

    @JvmSynthetic internal fun dropManagement() {
        val vals = values
        schema.forEachIndexed(fields) { i, field ->
            field.foldOrdinal(
                ifMutable = { (vals[i] as ManagedProperty<*, *, *, *>).dropManagement() },
                ifImmutable = { /* no-op */ }
            )
        }
    }

    override fun toString(): String =
            if (isManaged) super.toString()
            else buildString {
                append(this@Record.javaClass.simpleName).append(':')
                        .append(schema.javaClass.simpleName).append("(isManaged=false)")
            }

}
