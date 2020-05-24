package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableStruct
import net.aquadc.persistence.IteratorAndTransientStruct
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import java.io.FilterInputStream
import java.io.InputStream
import java.sql.ResultSet
import java.sql.SQLFeatureNotSupportedException

@PublishedApi internal class FetchCellLazily<CUR, R>(
    private val rt: Ilk<R, *>,
    private val orElse: () -> R
) : Fetch<Blocking<CUR>, Lazy<R>> {
    override fun fetch(
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.NotNull.Simple<*>>, arguments: Array<out Any>
    ): Lazy<R> {
        val rt = rt; val orElse = orElse // don't capture `this`
        return lazy { from.cell(query, argumentTypes, arguments, rt, orElse) }
    }
}

@PublishedApi internal class FetchColLazily<CUR, R>(
    private val rt: Ilk<R, *>
) : Fetch<Blocking<CUR>, CloseableIterator<R>> {
    override fun fetch(
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.NotNull.Simple<*>>, arguments: Array<out Any>
    ): CloseableIterator<R> {
        val rt = rt // don't capture `this`
        return object : CurIterator<CUR, NullSchema, R>(from, query, argumentTypes, arguments, null, BindBy.Name/*whatever*/, NullSchema) {
            override fun row(cur: CUR): R = from.cellAt(cur, 0, rt)
        }
    }
}

@PublishedApi internal class FetchStructLazily<SCH : Schema<SCH>, CUR>(
        private val table: Table<SCH, *>,
        private val bindBy: BindBy,
        private val orElse: () -> Struct<SCH>
) : Fetch<Blocking<CUR>, CloseableStruct<SCH>>, CloseableStruct<SCH> {

    private var fallback: Struct<SCH>? = null
    override fun fetch(
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.NotNull.Simple<*>>, arguments: Array<out Any>
    ): CloseableStruct<SCH> {
        val lazy = CurIterator<CUR, SCH, CloseableStruct<SCH>>(from, query, argumentTypes, arguments, table, bindBy, table.schema)

        return if (lazy.hasNext() /* move to first */) lazy else this.also { fallback = orElse() }
    }

    override fun <T> get(field: FieldDef<SCH, T, *>): T = fallback!![field]
    override val schema: SCH get() = fallback!!.schema
    override fun close() { /* nothing to do here */ }
}

@PublishedApi internal class FetchStructListLazily<CUR, SCH : Schema<SCH>>(
        private val table: Table<SCH, *>,
        private val bindBy: BindBy,
        private val transient: Boolean
) : Fetch<Blocking<CUR>, CloseableIterator<Struct<SCH>>> {
    override fun fetch(
        from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.NotNull.Simple<*>>, arguments: Array<out Any>
    ): CloseableIterator<Struct<SCH>> {
        val transient = transient // don't capture this
        return object : CurIterator<CUR, SCH, Struct<SCH>>(
            from, query, argumentTypes, arguments, table, bindBy, table.schema
        ) {
            override fun row(cur: CUR): Struct<SCH> =
                if (transient) this else StructSnapshot(this)
        }
    }
}

@PublishedApi internal object InputStreamFromResultSet : Fetch<Blocking<ResultSet>, InputStream> {

    override fun fetch(
        from: Blocking<ResultSet>, query: String, argumentTypes: Array<out DataType.NotNull.Simple<*>>, arguments: Array<out Any>
    ): InputStream =
            from.select(query, argumentTypes, arguments, 1).let { rs ->
                check(rs.next()) { rs.close(); "ResultSet is empty." }

                val stream = try { rs.getBlob(0).binaryStream } // Postgres-JDBC supports this, SQLite-JDBC doesn't
                catch (e: SQLFeatureNotSupportedException) { rs.getBinaryStream(0) } // this is typically just in-memory :'(

                object : FilterInputStream(stream) {
                    override fun close() {
                        super.close()
                        rs.close()
                    }
                }
            }
}

@Deprecated("moved") typealias CloseableIterator<T> = CloseableIterator<T>
@Deprecated("moved") typealias CloseableStruct<SCH> = CloseableStruct<SCH>

private open class CurIterator<CUR, SCH : Schema<SCH>, R>(
    protected val from: Blocking<CUR>,
    private val query: String,
    private val argumentTypes: Array<out DataType.NotNull.Simple<*>>,
    private val arguments: Array<out Any>,

    private val table: Table<SCH, *>?,
    private val bindBy: BindBy,
    schema: SCH
) : IteratorAndTransientStruct<SCH, R>(schema) {

    private var _cur: CUR? = null
    private val cur get() = _cur ?: run {
        check(state == 0) { "Iterator is closed." }
        from.select(query, argumentTypes, arguments, table?.managedColNames?.size ?: 1).also { _cur = it }
    }

    var state = 0

    final override fun next(): R = cur.let { cur ->
        when (state) {
            0 -> if (!move(cur, toState = 0)) throw NoSuchElementException()
            1 -> state = 0
            2 -> throw NoSuchElementException()
            else -> throw AssertionError()
        }
        row(cur)
    }
    final override fun hasNext(): Boolean =
            when (state) {
                0 -> move(cur, 1)
                1 -> true
                2 -> false
                else -> throw AssertionError()
            }
    final override fun close() {
        _cur?.let { from.close(it); _cur = null }
        state = 2
    }

    private fun move(cur: CUR, toState: Int): Boolean = from.next(cur).also {
        if (it) state = toState
        else close()
    }

    protected open fun row(cur: CUR): R =
            throw UnsupportedOperationException()

    final override fun <T> get(field: FieldDef<SCH, T, *>): T = when (state) {
        0,
        1 -> table!!.let { it.delegateFor(field).get(from, it, field, cur, bindBy) }
        2 -> throw UnsupportedOperationException()
        else -> throw AssertionError()
    }

}
