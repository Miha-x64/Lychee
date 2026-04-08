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
internal sealed class SqlPropertyDelegate<SCH : Schema<SCH>, ID : IdBound>(
    @JvmField val colCount: Int
) {
    abstract fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence
    abstract fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *>

    abstract fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T)
}

internal class Simple<SCH : Schema<SCH>, ID : IdBound> : SqlPropertyDelegate<SCH, ID>(1) {

    override fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence =
        if (index == 0) table.schema.run { field.name }
        else throw IndexOutOfBoundsException()
    override fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *> =
        if (index == 0) table.typeOf(field as FieldDef<SCH, Any?, DataType<Any?>>)
        else throw IndexOutOfBoundsException()

    override fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T) {
        out[0] = value
    }
}

internal class Embedded<SCH : Schema<SCH>, ID : IdBound>(
    @JvmField val recipe: Array<Table.StructStart?>, // contains a single start-end pair with (flattened) nesting inside
    @JvmField val myOffset: Int,
    @JvmField val columnNames: Array<out CharSequence>,
    @JvmField val columnTypes: Array<out Ilk<*, *>>
) : SqlPropertyDelegate<SCH, ID>(recipe.first()!!.colCount) {

    override fun nameAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): CharSequence =
        columnNames[index]
    override fun typeAt(table: Table<SCH, *>, field: FieldDef<SCH, *, *>, index: Int): Ilk<*, *>  =
        columnTypes[index]

    override fun <T> flattenTo(out: Array<Any?>, table: Table<SCH, *>, field: FieldDef<SCH, T, *>, value: T) {
        flatten(recipe, out, value, 0, 0)
    }
}
