package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens

/**
 * Responsible for fetching and updating data.
 * Fetching or storing a single value is trivial,
 * but handling a nested struct is a bit different.
 */
internal interface SqlPropertyDelegate<SCH : Schema<SCH>, ID : IdBound> {
    fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID
    ): T
    fun <T> update(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID,
            previous: T, update: T
    )
}

internal class Simple<SCH : Schema<SCH>, ID : IdBound> : SqlPropertyDelegate<SCH, ID> {

    override fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID
    ): T =
            lowSession.fetchSingle(table, field, id)

    override fun <T> update(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID,
            previous: T, update: T
    ): Unit =
            lowSession.update(table, id, field, update)

}

internal class Embedded<SCH : Schema<SCH>, ID : IdBound>(
        private val columns: Array<StoredNamedLens<SCH, *, *>>,
        private val recipe: Array<Table.Nesting> // contains a single start-end pair with (flattened) nesting inside
) : SqlPropertyDelegate<SCH, ID> {

    override fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID
    ): T {
        val values = lowSession.fetch(table, columns, id)
        inflate(recipe, values, 0, 0, 0)
        return values[0] as T
    }

    override fun <T> update(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID, previous: T, update: T
    ): Unit = lowSession.update(
            table, id, columns,
            // TODO don't allocate this array, bind args directly instead
            arrayOfNulls<Any>(columns.size).also { flatten(recipe, it, update, 0, 0) }
    )

}
