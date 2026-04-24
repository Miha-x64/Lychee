package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.SizedIterator
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Exec
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.MutableSqlDatabase
import net.aquadc.persistence.sql.SqlDatabase
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.nothing

@PublishedApi internal class FetchCellEagerly<R>(
        private val rt: Ilk<out R, *>,
        private val orElse: () -> R
) : Fetch<R> {
    override fun fetch(
        from: SqlDatabase,
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        arguments: Array<out Any>
    ): R =
        from.cell(query, argumentTypes, arguments, rt, orElse)
}

@PublishedApi internal class FetchStructEagerly<SCH : Schema<SCH>>(
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val orElse: () -> Any?,
) : Fetch<Any?> {
    override fun fetch(
        from: SqlDatabase, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, arguments: Array<out Any>
    ): Any? {
        val iter = from.rows(query, argumentTypes, arguments, table, bindBy, false)
        return try {
            if (iter.hasNext()) {
                iter.next().also {
                    require(!iter.hasNext()) { "The query has returned more than one row." }
                }
            } else {
                orElse()
            }
        } finally {
            iter.close()
        }
    }
}

internal fun <R> Fetch<Iterator<R>>.collect(): Fetch<List<R>> =
    object : Fetch<List<R>> {
        override fun fetch(
            from: SqlDatabase,
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            arguments: Array<out Any>
        ): List<R> =
            this@collect.fetch(from, query, argumentTypes, arguments).collect()
    }

private fun <R> Iterator<R>.collect(): List<R> {
    return if (hasNext()) {
        val first = next()
        if (hasNext()) {
            // TODO collect to primitive array if possible
            (if (this is SizedIterator<*>) ArrayList(this.size) else ArrayList<R>()).also { dst ->
                dst.add(first)
                do dst.add(next()) while (hasNext())
            }
        } else listOf(first)
    } else emptyList()
}

@Suppress("UNCHECKED_CAST") // `nothing` has special handling
@PublishedApi @JvmField internal val ExecuteForUnit = ExecuteEagerlyFor(nothing)
    as Exec<Unit>
@Suppress("UNCHECKED_CAST") // `null` has special handling
@PublishedApi @JvmField internal val ExecuteForRowCount = ExecuteEagerlyFor<Nothing>(null)
    as Exec<Int>

@PublishedApi internal class ExecuteEagerlyFor<ID>(
    private val retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
) : Exec<Any?> {
    override fun fetch(
        from: MutableSqlDatabase, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, arguments: Array<out Any>
    ): Any? {
        val ret = from.execute(query, argumentTypes, arguments, if (retKeyType === nothing) null else retKeyType)
        return if (retKeyType === nothing) Unit else ret // if (retKeyType == null) affected else insertedPrimaryKey
    }
}
