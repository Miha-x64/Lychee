package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.valuesOf
import net.aquadc.properties.internal.Unset

/**
 * Responsible for fetching and updating data.
 * Fetching or storing a single value is trivial,
 * but handling a nested struct is a bit different.
 */
internal interface SqlPropertyDelegate {

    fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T

    fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, previous: T, update: T, into: Array<Any?>
    )
}

internal object Simple : SqlPropertyDelegate {

    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T =
            lowSession.fetchSingle(table, path, id)

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, previous: T, update: T, into: Array<Any?>
    ) {
        lowSession.update(table, id, path, update)
        into[table.columnIndices[path]!!] = update
    }

}

internal class Embedded<SCH : Schema<SCH>, TSCH : Schema<TSCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val schema: TSCH,
        private val lenses: Array<NamedLens<SCH, REC, *>>,
        private val columns: Array<NamedLens<SCH, REC, *>>,
        private val fieldSetColumn: NamedLens<SCH, REC, out Long?>?
) : SqlPropertyDelegate {

    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T {
        val fieldSet = if (fieldSetColumn != null) {
            lowSession.fetchSingle(table, fieldSetColumn as NamedLens<SCH, *, Long?>, id)
                    ?.let { FieldSet<TSCH, FieldDef<TSCH, *>>(it) }
        } else {
            schema.allFieldSet()
        }

        return when (fieldSet) {
            null -> null
            schema.allFieldSet() -> Record(session, table, schema, id, lenses as Array<NamedLens<*, Record<*, ID>, *>>)
                                 // ^^^^^^ will be visible outside as (Partial)Struct(?), using Record for laziness; fixme: superfluous ManagedProperty instances
            else -> PartialRecord(session, table, schema, id, lenses as Array<NamedLens<*, Record<*, ID>, *>>, fieldSet)
        } as T
    }

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, previous: T, update: T, into: Array<Any?>
    ) {
        val type = path.type
        val actualType = if (type is DataType.Nullable<*>) type.actualType else type
        val prev = when {
            type is DataType.Nullable<*> && previous === null -> null
            actualType is Schema<*> -> StructSnapshot(previous as Struct<TSCH>)
            actualType is DataType.Partial<*, *> -> (actualType as DataType.Partial<PartialStruct<TSCH>, TSCH>)
                    .load((previous as PartialStruct<TSCH>).fields, previous.packedValues())
            else -> throw AssertionError()
        }
        val vals = (update as PartialStruct<TSCH>?)?.valuesOf(columns, path.size)

        lowSession.update(table, id, columns, vals)

        val columnIndices = table.columnIndices
        for (i in columns.indices) {
            columnIndices[columns[i] as NamedLens<SCH, Nothing, out Any?>]!!.let { idx ->
                into[idx] = vals?.get(i)
            }
        }

        val oldVals = into.last() as Array<Any?>?
                ?: Array<Any?>(table.schema.fields.size) { _ -> Unset }.also { into[into.lastIndex] = it }

        check(path.size == 1) // preserve a copy of previous struct, 'cause we gonna forget its values:
        oldVals[(path as FieldDef.Mutable).ordinal.toInt()] = prev
    }

}

private fun <SCH : Schema<SCH>> PartialStruct<SCH>.packedValues(): Array<Any?> {
    val values = arrayOfNulls<Any>(fields.size.toInt())
    schema.forEachIndexed(fields) { idx, field ->
        values[idx] = getOrThrow(field)
    }
    return values
}
