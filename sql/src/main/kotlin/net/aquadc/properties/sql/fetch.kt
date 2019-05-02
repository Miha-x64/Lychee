package net.aquadc.properties.sql

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct

/**
 * Responsible for fetching and updating data.
 * Fetching or storing a single value is trivial,
 * but handling a nested struct is a bit different.
 */
internal interface SqlPropertyDelegate {

    fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T

    /**
     * @return evicted record, if any
     */
    fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, update: T
    ): Record<*, *>?
}

internal object Simple : SqlPropertyDelegate {

    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T =
            lowSession.fetchSingle(table, path.name, path.type, lowSession.reusableCond(table, table.idColName, id))

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, update: T
    ): Record<*, *>? {
        lowSession.update(table, id, path, update)
        return null
    }

}

internal class Embedded<SCH : Schema<SCH>, TSCH : Schema<TSCH>>(
        private val schema: TSCH,
        private val columns: List<NamedLens<SCH, Struct<SCH>, *>>
) : SqlPropertyDelegate {

    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID
    ): T =
            Record(session, table, schema, id, columns as List<NamedLens<*, Record<*, ID>, *>>) as T

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Struct<SCH>, T>, id: ID, update: T
    ): Record<*, *>? =
            TODO()

}
