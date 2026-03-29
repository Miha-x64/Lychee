package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableStruct
import net.aquadc.persistence.IteratorAndTransientStruct
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.SqlDatabase
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

@PublishedApi internal class FetchCellLazily<CUR, R>(
    private val rt: Ilk<out R, *>,
    private val orElse: () -> R
) : Fetch<CUR, Lazy<R>> {
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): Lazy<R> {
        val rt = rt; val orElse = orElse // don't capture `this`
        return lazy { from.cell(query, argumentTypes, receiverAndArguments, rt, orElse) }
    }
}

@PublishedApi internal class FetchColLazily<CUR, R>(
    private val rt: Ilk<R, *>
) : Fetch<CUR, CloseableIterator<R>> {
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<R> =
        from.column(query, argumentTypes, receiverAndArguments, rt)
}

@PublishedApi internal class FetchStructLazily<SCH : Schema<SCH>, CUR>(
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val orElse: () -> Struct<SCH>?,
) : Fetch<CUR, CloseableStruct<SCH>?>, CloseableStruct<SCH> {

    private var fallback: Struct<SCH>? = null
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableStruct<SCH> {
        val table = table; val bindBy = bindBy; val orElse = orElse // don't capture `this`
        val lazy = object : DbIter<CUR, SCH, CloseableStruct<SCH>>(table.schema, null) {
            override fun open(): CUR =
                from.select(query, argumentTypes, receiverAndArguments, table.managedColNames.size)
            override fun <T> cell(field: FieldDef<SCH, T, *>): T =
                table.let { it.delegateFor(field).get(from, it, field, cur, bindBy) }

            override fun moveToNext(): Boolean = from.next(cur)
            override fun onClose() = from.close(cur)
        }

        return if (lazy.hasNext() /* move to first */) lazy else this.also { fallback = orElse() }
    }

    override fun <T> get(field: FieldDef<SCH, T, *>): T = fallback!![field]
    override val schema: SCH get() = fallback!!.schema
    override fun close() { (fallback as? CloseableStruct)?.close() } // some dirty crap here, but damn, what can I do?
}

@PublishedApi internal class FetchStructListLazily<CUR, SCH : Schema<SCH>>(
        private val table: Table<SCH, *>,
        private val bindBy: BindBy,
        private val transient: Boolean
) : Fetch<CUR, CloseableIterator<Struct<SCH>>> {
    override fun fetch(
        from: SqlDatabase<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<Struct<SCH>> {
        val transient = transient; val table = table; val bindBy = bindBy // don't capture `this`
        return object : DbIter<CUR, SCH, Struct<SCH>>(table.schema, null) {
            override fun open(): CUR =
                from.select(query, argumentTypes, receiverAndArguments, table.managedColNames.size)
            override fun row(cur: CUR): Struct<SCH> =
                if (transient) this else StructSnapshot(this)
            override fun <T> cell(field: FieldDef<SCH, T, *>): T =
                table.let { it.delegateFor(field).get(from, it, field, cur, bindBy) }

            override fun moveToNext(): Boolean = from.next(cur)
            override fun onClose() = from.close(cur)
        }
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
