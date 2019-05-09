package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
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
            lowSession.fetchSingle(table, path.name, path.type, lowSession.reusableCond(table, table.idColName, id))

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
        private val columns: Array<NamedLens<SCH, REC, *>>
) : SqlPropertyDelegate {

    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T =
            Record(session, table, schema, id, lenses as Array<NamedLens<*, Record<*, ID>, *>>) as T
    //      ^^^^^^ will be visible outside as Struct, using Record for laziness; fixme: superfluous ManagedProperty instances

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession<*>, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, previous: T, update: T, into: Array<Any?>
    ) {
        val prev = StructSnapshot(previous as Struct<TSCH>)
        val vals = (update as Struct<TSCH>).valuesOf(columns, path.size)

        lowSession.update(table, id, columns, vals)

        val columnIndices = table.columnIndices
        for (i in columns.indices) {
            columnIndices[columns[i] as NamedLens<SCH, Nothing, out Any?>]!!.let { idx ->
                into[idx] = vals[i]
            }
        }

        val oldVals = into.last() as Array<Any?>?
                ?: Array<Any?>(table.schema.fields.size) { _ -> Unset }.also { into[into.lastIndex] = it }

        check(path.size == 1) // preserve a copy of previous struct, 'cause we gonna forget its values:
        oldVals[(path as FieldDef.Mutable).ordinal.toInt()] = prev
    }

}
