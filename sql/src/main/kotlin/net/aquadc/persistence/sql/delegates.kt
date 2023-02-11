package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

/**
 * Responsible for fetching and updating data.
 * Fetching or storing a single value is trivial,
 * but handling a nested struct is a bit different.
 */
internal abstract class SqlPropertyDelegate<SCH : Schema<SCH>, ID : IdBound>(
    @JvmField val colCount: Int
) {
    abstract fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence
    abstract fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *>

    abstract fun <T, CUR> get(
        lowSession: FreeSource<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR, bindBy: BindBy
    ): T

    abstract fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T)
}

internal class Simple<SCH : Schema<SCH>, ID : IdBound> : SqlPropertyDelegate<SCH, ID>(1) {

    override fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence =
        if (index == 0) table.schema.run { field.name }
        else throw IndexOutOfBoundsException()
    override fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *> =
        if (index == 0) table.typeOf(field as FieldDef<SCH, Any?, DataType<Any?>>)
        else throw IndexOutOfBoundsException()

    override fun <T, CUR> get(
            lowSession: FreeSource<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            lowSession.cell<SCH, CUR, T>(cursor, table, field, bindBy)

    override fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T) {
        out[0] = value
    }
}

internal class Embedded<SCH : Schema<SCH>, ID : IdBound>(
    private val recipe: Array<Table.StructStart?>, // contains a single start-end pair with (flattened) nesting inside
    private val myOffset: Int,
    private val columnNames: Array<out CharSequence>,
    private val columnTypes: Array<Ilk<*, *>>
) : SqlPropertyDelegate<SCH, ID>(recipe.first()!!.colCount) {

    override fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence =
        columnNames[index]
    override fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *>  =
        columnTypes[index]

    override fun <T, CUR> get(
            lowSession: FreeSource<CUR>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, cursor: CUR,
            bindBy: BindBy
    ): T =
            inflated(lowSession.row(cursor, myOffset, columnNames, columnTypes, bindBy))

    private fun <T> inflated(values: Array<Any?>): T {
        inflate(recipe, values, 0, 0, 0)
        return values[0] as T
    }

    override fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T) {
        flatten(recipe, out, value, 0, 0)
    }
}
