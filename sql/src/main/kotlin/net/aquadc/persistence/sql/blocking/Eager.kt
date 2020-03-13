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
        private val et: DataType<R>
) : Fetch<Blocking<CUR>, List<R>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<R> {
        val cur = from.select(query, argumentTypes, arguments, 1)
        try {
            return if (from.next(cur)) {
                val first = from.cellAt(cur, 0, et)
                if (from.next(cur)) {
                    ArrayList<R>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.cellAt(cur, 0, et)) while (from.next(cur))
                    }
                } else listOf(first)
            } else emptyList()
        } finally {
            cur.close()
        }
    }
}

@PublishedApi internal class FetchStructEagerly<SCH : Schema<SCH>, CUR : AutoCloseable>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, StructSnapshot<SCH>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): StructSnapshot<SCH> {
        val cur = from.select(query, argumentTypes, arguments, table.columns.size)
        try {
            check(from.next(cur))
            val values = from.row(cur, table.columnsMappedToFields, bindBy)
            check(!from.next(cur)) // single row expected
            inflate(table.recipe, values, 0, 0, 0)
            @Suppress("UNCHECKED_CAST")
            return values[0] as StructSnapshot<SCH>
        } finally {
            cur.close()
        }
    }
}

@PublishedApi internal class FetchStructListEagerly<CUR : AutoCloseable, SCH : Schema<SCH>>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<StructSnapshot<SCH>> {
        val cols = table.columnsMappedToFields
        val recipe = table.recipe

        val cur = from.select(query, argumentTypes, arguments, cols.size)
        try {
            return if (from.next(cur)) {
                val first = from.mapRow(cur, cols, recipe)
                if (from.next(cur)) {
                    ArrayList<StructSnapshot<SCH>>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.mapRow(cur, cols, recipe)) while (from.next(cur))
                    }
                } else listOf(first)
            } else emptyList()
        } finally {
            cur.close()
        }
    }

    private fun Blocking<CUR>.mapRow(cur: CUR, cols: Array<out StoredNamedLens<SCH, *, *>>, recipe: Array<out Table.Nesting>): StructSnapshot<SCH> {
        val firstValues = row(cur, cols, bindBy); inflate(recipe, firstValues, 0, 0, 0)
        @Suppress("UNCHECKED_CAST")
        return firstValues[0] as StructSnapshot<SCH>
    }
}
