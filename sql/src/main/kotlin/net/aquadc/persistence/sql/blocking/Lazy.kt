package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableStruct
import net.aquadc.persistence.IteratorAndTransientStruct
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.FreeSource
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
        from: FreeSource<CUR>, query: String,
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
        from: FreeSource<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<R> {
        val rt = rt // don't capture `this`
        return object : CurIterator<CUR, NullSchema, R>(from, query, argumentTypes, receiverAndArguments, null, BindBy.Name/*whatever*/, NullSchema) {
            override fun row(cur: CUR): R = from.cellAt(cur, 0, rt)
        }
    }
}

@PublishedApi internal class FetchStructLazily<SCH : Schema<SCH>, CUR>(
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val orElse: () -> Struct<SCH>?,
) : Fetch<CUR, CloseableStruct<SCH>?>, CloseableStruct<SCH> {

    private var fallback: Struct<SCH>? = null
    override fun fetch(
        from: FreeSource<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableStruct<SCH> {
        val lazy = CurIterator<CUR, SCH, CloseableStruct<SCH>>(from, query, argumentTypes, receiverAndArguments, table, bindBy, table.schema)

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
        from: FreeSource<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): CloseableIterator<Struct<SCH>> {
        val transient = transient // don't capture this
        return object : CurIterator<CUR, SCH, Struct<SCH>>(
            from, query, argumentTypes, receiverAndArguments, table, bindBy, table.schema
        ) {
            override fun row(cur: CUR): Struct<SCH> =
                if (transient) this else StructSnapshot(this)
        }
    }
}

private open class CurIterator<CUR, SCH : Schema<SCH>, R>(
    protected val from: FreeSource<CUR>,
    private val query: String,
    private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
    private val sessionAndArguments: Array<out Any>,

    private val table: Table<SCH, *>?,
    private val bindBy: BindBy,
    schema: SCH
) : IteratorAndTransientStruct<SCH, R>(schema) {

    private var _cur: CUR? = null
    private val cur get() = _cur ?: run {
        check(state == 0) { "Iterator is closed." }
        from.select(query, argumentTypes, sessionAndArguments, table?.managedColNames?.size ?: 1).also { _cur = it }
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
