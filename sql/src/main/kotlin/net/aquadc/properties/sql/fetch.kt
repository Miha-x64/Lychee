package net.aquadc.properties.sql

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema


internal interface FetchStrategy {
    fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, condition: WhereCondition<out SCH>
    ): T
}

internal object FetchPrimitive : FetchStrategy {
    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, condition: WhereCondition<out SCH>
    ): T =
            lowSession.fetchSingle(table, path.name, path.type, condition)
}

internal object FetchEmbedded : FetchStrategy {
    override fun <SCH : Schema<SCH>, ID : IdBound, T> fetch(
            lowSession: LowLevelSession, table: Table<SCH, ID, *>, path: NamedLens<SCH, Record<SCH, ID>, T>, condition: WhereCondition<out SCH>
    ): T =
            TODO()
}
