package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.IteratorAndTransientStruct
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.SqlDatabase
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

@PublishedApi internal class FetchColLazily<CUR, R>(
    private val rt: Ilk<R, *>
) : Fetch<CUR, CloseableIterator<R>> {
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<R> =
        from.column(query, argumentTypes, receiverAndArguments, rt)
}

@PublishedApi internal class FetchStructsLazily<CUR, SCH : Schema<SCH>>(
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val transient: Boolean,
) : Fetch<CUR, CloseableIterator<Struct<SCH>>> {
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<Struct<SCH>> =
        from.rows(query, argumentTypes, receiverAndArguments, table, bindBy, transient)
}

internal fun <CUR, R> Fetch<CUR, R>.lazy(): Fetch<CUR, Lazy<R>> =
    object : Fetch<CUR, Lazy<R>> {
        override fun fetch(
            from: SqlDatabase<CUR>,
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            receiverAndArguments: Array<out Any>
        ): Lazy<R> =
            lazy {
                this@lazy.fetch(from, query, argumentTypes, receiverAndArguments)
            }
    }

internal abstract class DbIter<CUR, SCH : Schema<SCH>, R>(
    schema: SCH,
    initialCur: CUR?,
) : IteratorAndTransientStruct<SCH, R>(schema) {

    private var state = 0 // 0: no element; 1: hasNext() returned true; 2: closed

    private var _cur: CUR? = initialCur
    protected val cur get() = _cur ?: run {
        check(state == 0) { "Iterator is closed." }
        open().also { _cur = it }
    }

    protected open fun open(): CUR =
        throw UnsupportedOperationException()

    final override fun next(): R = cur.let { cur ->
        when (state) {
            0 -> if (!move(toState = 0)) throw NoSuchElementException()
            1 -> state = 0
            2 -> throw NoSuchElementException()
            else -> throw AssertionError()
        }
        row(cur)
    }
    final override fun hasNext(): Boolean =
        when (state) {
            0 -> move(toState = 1)
            1 -> true
            2 -> false
            else -> throw AssertionError()
        }
    final override fun close() {
        _cur?.let { onClose(); _cur = null }
        state = 2
    }

    private fun move(toState: Int): Boolean =
        moveToNext()
            .also { if (it) state = toState else close() }

    protected abstract fun moveToNext(): Boolean

    protected abstract fun onClose()

    protected open fun row(cur: CUR): R =
        throw UnsupportedOperationException()

    final override fun <T> get(field: FieldDef<SCH, T, *>): T = when (state) {
        0,
        1 -> cell(field)
        2 -> throw IllegalStateException()
        else -> throw AssertionError()
    }

    protected open fun <T> cell(field: FieldDef<SCH, T, *>): T =
        throw UnsupportedOperationException()

}
