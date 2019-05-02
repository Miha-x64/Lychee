package net.aquadc.properties.sql

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct


internal interface FetchStrategy {
    fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, id: ID
    ): T
}

internal object FetchPrimitive : FetchStrategy {
    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, id: ID
    ): T =
            lowSession.fetchSingle(table, path.name, path.type, lowSession.reusableCond(table, table.idColName, id))
}

internal class FetchEmbedded<SCH : Schema<SCH>, TSCH : Schema<TSCH>>(
        private val schema: TSCH,
        private val columns: List<NamedLens<SCH, Struct<SCH>, *>>
) : FetchStrategy {
    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            session: Session, lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, id: ID
    ): T =
            Record(session, table, schema, id, columns as List<NamedLens<*, Record<*, ID>, *>>) as T
}
