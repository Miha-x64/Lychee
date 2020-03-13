package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.Blocking
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

    fun <T, CUR : AutoCloseable> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
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

    override fun <T, CUR : AutoCloseable> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            lowSession.cell<SCH, CUR, T>(cursor, table, field, bindBy)

    override fun <T> update(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID,
            previous: T, update: T
    ): Unit =
            lowSession.update(table, id, field, update)
}

internal class Embedded<SCH : Schema<SCH>, ID : IdBound>(
        private val columns: Array<StoredNamedLens<SCH, *, *>>,
        private val recipe: Array<Table.Nesting> // contains a single start-end pair with (flattened) nesting inside
      , private val myOffset: Int
) : SqlPropertyDelegate<SCH, ID> {

    override fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID
    ): T =
            inflated(lowSession.fetch(table, columns, id))

    override fun <T, CUR : AutoCloseable> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            inflated(lowSession.row(cursor, myOffset, columns, bindBy))

    private fun <T> inflated(values: Array<Any?>): T {
        inflate(recipe, values, 0, 0, 0)
        return values[0] as T
    }

    override fun <T> update(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID, *>, field: FieldDef<SCH, T, *>, id: ID,
            previous: T, update: T
    ): Unit = lowSession.update(
            table, id, columns,
            // TODO don't allocate this array, bind args directly instead
            arrayOfNulls<Any>(columns.size).also { flatten(recipe, it, update, 0, 0) }
    )
}
