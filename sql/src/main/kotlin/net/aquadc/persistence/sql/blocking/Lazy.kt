package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Record
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.nothing
import java.io.Closeable
import java.io.FilterInputStream
import java.io.InputStream
import java.sql.ResultSet
import java.sql.SQLFeatureNotSupportedException

@PublishedApi internal class FetchCellLazily<CUR : AutoCloseable, R>(
        private val rt: DataType<R>
) : Fetch<Blocking<CUR>, Lazy<R>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): Lazy<R> =
            lazy { from.cell(query, argumentTypes, arguments, rt) }
}

@PublishedApi internal class FetchColLazily<CUR : AutoCloseable, R>(
        private val rt: DataType<R>
) : Fetch<Blocking<CUR>, CloseableIterator<R>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): CloseableIterator<R> =
            object : CurIterator<CUR, NullSchema, R>(from, query, argumentTypes, arguments, null, BindBy.Name/*whatever*/, NullSchema) {
                override fun row(cur: CUR): R = from.cellAt(cur, 0, rt)
            }
}

@PublishedApi internal class FetchStructLazily<SCH : Schema<SCH>, CUR : AutoCloseable>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, CloseableStruct<SCH>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): CloseableStruct<SCH> =
            CurIterator<CUR, SCH, CloseableStruct<SCH>>(from, query, argumentTypes, arguments, table, bindBy, table.schema)
                    .also { it.hasNext() /* move to first */ }
}

@PublishedApi internal class FetchStructListLazily<CUR : AutoCloseable, SCH : Schema<SCH>>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, CloseableIterator<TemporaryStruct<SCH>>> {
    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): CloseableIterator<TemporaryStruct<SCH>> =
            object : CurIterator<CUR, SCH, TemporaryStruct<SCH>>(
                    from, query, argumentTypes, arguments, table, bindBy, table.schema
            ) {
                override fun row(cur: CUR): TemporaryStruct<SCH> = this
            }
}

@PublishedApi internal object InputStreamFromResultSet : Fetch<Blocking<ResultSet>, InputStream> {

    override fun fetch(
            from: Blocking<ResultSet>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
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

interface CloseableIterator<out T> : Iterator<T>, Closeable
interface CloseableStruct<SCH : Schema<SCH>> : Struct<SCH>, Closeable

/**
 * A struct which will be invalid after mutating [Iterator] which owns it.
 * [get] could be blocking.
 *
 * @see android.util.MapCollections.MapIterator
 */
interface TemporaryStruct<SCH : Schema<SCH>> : Struct<SCH>

private open class CurIterator<CUR : AutoCloseable, SCH : Schema<SCH>, R>(
        protected val from: Blocking<CUR>,
        private val query: String,
        private val argumentTypes: Array<out DataType.Simple<*>>,
        private val arguments: Array<out Any>,

        private val table: Table<SCH, *, out Record<SCH, *>>?,
        private val bindBy: BindBy,
        schema: SCH
) : BaseStruct<SCH>(schema), CloseableIterator<R>, TemporaryStruct<SCH>, CloseableStruct<SCH> {
// he-he, like this weird iterator https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/util/MapCollections.java#74

    private var _cur: CUR? = null
    private val cur get() = _cur ?: run {
        check(state == 0) { "Iterator is closed." }
        from.select(query, argumentTypes, arguments, table?.managedColumns?.size ?: 1).also { _cur = it }
    }

    var state = 0

    override fun next(): R = cur.let { cur ->
        when (state) {
            0 -> if (!move(cur, toState = 0)) throw NoSuchElementException()
            1 -> state = 0
            2 -> throw NoSuchElementException()
            else -> throw AssertionError()
        }
        row(cur)
    }
    override fun hasNext(): Boolean =
            when (state) {
                0 -> move(cur, 1)
                1 -> true
                2 -> false
                else -> throw AssertionError()
            }
    override fun close() {
        _cur?.let { it.close(); _cur = null }
        state = 2
    }

    private fun move(cur: CUR, toState: Int): Boolean = from.next(cur).also {
        if (it) state = toState
        else close()
    }

    protected open fun row(cur: CUR): R =
            throw UnsupportedOperationException()

    override fun <T> get(field: FieldDef<SCH, T, *>): T = when (state) {
        0,
        1 -> table!!.let { it.delegateFor(field).get(from, it, field, cur, bindBy) }
        2 -> throw UnsupportedOperationException()
        else -> throw AssertionError()
    }

    override fun equals(other: Any?): Boolean =
            if (schema === NullSchema) this === other
            else super<BaseStruct>.equals(other)
    override fun hashCode(): Int =
            if (schema === NullSchema) System.identityHashCode(this)
            else super<BaseStruct>.hashCode()
    override fun toString(): String =
            if (schema === NullSchema) javaClass.getName() + "@" + Integer.toHexString(hashCode())
            else super<BaseStruct>.toString()
}

private object NullSchema : Schema<NullSchema>() {
    init { "" let nothing }
}
