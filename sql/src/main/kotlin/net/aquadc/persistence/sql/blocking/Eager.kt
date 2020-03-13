package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.inflate
import net.aquadc.persistence.sql.row
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType

@PublishedApi internal class FetchCellEagerly<CUR : AutoCloseable, R>(
        private val rt: DataType<R>
) : Fetch<Blocking<CUR>, R> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): R =
            from.cell(query, argumentTypes, arguments, rt)
}

@PublishedApi internal class FetchColEagerly<CUR : AutoCloseable, R>(
        private val rt: DataType<R>
) : Fetch<Blocking<CUR>, List<R>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<R> =
            fetchCol(rt, from, query, argumentTypes, arguments)
}

@PublishedApi internal class FetchStructEagerly<SCH : Schema<SCH>, CUR : AutoCloseable>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, StructSnapshot<SCH>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): StructSnapshot<SCH> =
            fetchStruct(table, bindBy, from, query, argumentTypes, arguments)
}

@PublishedApi internal class FetchStructListEagerly<CUR : AutoCloseable, SCH : Schema<SCH>>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<StructSnapshot<SCH>> =
            fetchStructList(table, bindBy, from, query, argumentTypes, arguments)
}

internal fun <CUR : AutoCloseable, R> fetchCol(
        rt: DataType<R>,
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
): List<R> {
    val cur = from.select(query, argumentTypes, arguments, 1)
    try {
        return if (from.next(cur)) {
            val first = from.cellAt(cur, 0, rt)
            if (from.next(cur)) {
                ArrayList<R>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                    it.add(first)
                    do it.add(from.cellAt(cur, 0, rt)) while (from.next(cur))
                }
            } else listOf(first)
        } else emptyList()
    } finally {
        cur.close()
    }
}

internal fun <SCH : Schema<SCH>, CUR : AutoCloseable> fetchStruct(
        table: Table<SCH, *, *>, bindBy: BindBy,
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
): StructSnapshot<SCH> {
    val cur = from.select(query, argumentTypes, arguments, table.columns.size)
    try {
        check(from.next(cur))
        val value = from.mapRow(bindBy, cur, table.columnsMappedToFields, table.recipe)
        check(!from.next(cur)) // single row expected
        return value
    } finally {
        cur.close()
    }
}

internal fun <CUR : AutoCloseable, SCH : Schema<SCH>> fetchStructList(
        table: Table<SCH, *, *>, bindBy: BindBy,
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
): List<StructSnapshot<SCH>> {
    val cols = table.columnsMappedToFields
    val recipe = table.recipe

    val cur = from.select(query, argumentTypes, arguments, cols.size)
    try {
        return if (from.next(cur)) {
            val first = from.mapRow(bindBy, cur, cols, recipe)
            if (from.next(cur)) {
                ArrayList<StructSnapshot<SCH>>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                    it.add(first)
                    do it.add(from.mapRow(bindBy, cur, cols, recipe)) while (from.next(cur))
                }
            } else listOf<StructSnapshot<SCH>>(first)
        } else emptyList()
    } finally {
        cur.close()
    }
}

private fun <CUR : AutoCloseable, SCH : Schema<SCH>> Blocking<CUR>.mapRow(
        bindBy: BindBy,
        cur: CUR, cols: Array<out StoredNamedLens<SCH, *, *>>, recipe: Array<out Table.Nesting>
): StructSnapshot<SCH> {
    val firstValues = row(cur, 0, cols, bindBy)
    inflate(recipe, firstValues, 0, 0, 0)
    @Suppress("UNCHECKED_CAST")
    return firstValues[0] as StructSnapshot<SCH>
}
