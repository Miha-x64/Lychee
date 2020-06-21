package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.MutableField
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

/**
 * Responsible for fetching and updating data.
 * Fetching or storing a single value is trivial,
 * but handling a nested struct is a bit different.
 */
internal interface SqlPropertyDelegate<SCH : Schema<SCH>, ID : IdBound> {

    fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: FieldDef<SCH, T, *>, id: ID
    ): T

    fun <T, CUR> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T

    fun <T> update(
        lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: MutableField<SCH, T, *>, id: ID,
        previous: T, update: T
    )
}

internal class Simple<SCH : Schema<SCH>, ID : IdBound> : SqlPropertyDelegate<SCH, ID> {

    override fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: FieldDef<SCH, T, *>, id: ID
    ): T = table.schema.let { sch -> // the following cast seems to be unnecessary with new inference
        lowSession.fetchSingle(table, sch.run { field.name }, table.typeOf(field as FieldDef<SCH, T, DataType<T>>), id)
    }

    override fun <T, CUR> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            lowSession.cell<SCH, CUR, T>(cursor, table, field, bindBy)

    override fun <T> update(
        lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: MutableField<SCH, T, *>, id: ID,
        previous: T, update: T
    ): Unit = table.schema.let { sch ->
        lowSession.update(table, id, sch.run { field.name }, table.typeOf(field), update)
    }
}

internal class Embedded<SCH : Schema<SCH>, ID : IdBound>(
        private val recipe: Array<Table.Nesting>, // contains a single start-end pair with (flattened) nesting inside
        private val myOffset: Int,
        private val columnNames: Array<out CharSequence>,
        private val columnTypes: Array<Ilk<*, *>>
) : SqlPropertyDelegate<SCH, ID> {

    override fun <T> fetch(
            lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: FieldDef<SCH, T, *>, id: ID
    ): T =
            inflated(lowSession.fetch(table, columnNames, columnTypes, id))

    override fun <T, CUR> get(
            lowSession: Blocking<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            inflated(lowSession.row(cursor, myOffset, columnNames, columnTypes, bindBy))

    private fun <T> inflated(values: Array<Any?>): T {
        inflate(recipe, values, 0, 0, 0)
        return values[0] as T
    }

    override fun <T> update(
        lowSession: LowLevelSession<*, *>, table: Table<SCH, ID>, field: MutableField<SCH, T, *>, id: ID,
        previous: T, update: T
    ): Unit = lowSession.update(
            table, id, columnNames, columnTypes,
            // TODO don't allocate this array, bind args directly instead
            arrayOfNulls<Any>(columnNames.size).also { flatten(recipe, it, update, 0, 0) }
    )
}
