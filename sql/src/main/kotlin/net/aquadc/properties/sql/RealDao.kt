package net.aquadc.properties.sql

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.properties.*
import net.aquadc.properties.function.Arrayz
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.`Distinct-`
import net.aquadc.properties.internal.`Mapped-`
import net.aquadc.properties.sql.dialect.Dialect
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


internal class RealDao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val session: Session,
        private val lowSession: LowLevelSession,
        private val table: Table<SCH, ID, REC>,
        private val dialect: Dialect
) : Dao<SCH, ID, REC> {

    // there's no ReferenceQueue, just evict when reference gets nulled out

    private val recordRefs = ConcurrentHashMap<ID, WeakReference<REC>>()

    // SELECT COUNT(*) WHERE ...
    private val counts = ConcurrentHashMap<WhereCondition<out SCH>, WeakReference<`Mapped-`<WhereCondition<out SCH>, Long>>>()

    // SELECT _id WHERE ...
    private val selections = ConcurrentHashMap<ConditionAndOrder<SCH>, WeakReference<Property<List<REC>>>>()
    // same items here, but in different format
    private val selectionsByOrder = ConcurrentHashMap<@ParameterName("columnName") String, CopyOnWriteArraySet<WeakReference<Property<List<REC>>>>>()

    @Suppress("UNCHECKED_CAST")
    internal fun <T> commitValue(id: ID, colName: String, value: T) {
        recordRefs.getWeakOrRemove(id)?.let { rec ->
            val ord = table.schema.fieldsByName[colName]!!.ordinal.toInt() // TODO
            (rec.values[ord] as ManagedProperty<SCH, Transaction, T, ID>).commit(value)
        }
    }

    internal fun forget(id: ID): WeakReference<REC>? =
            recordRefs.remove(id)?.let(::forgetInternal)

    internal fun truncate(): List<WeakReference<REC>> {
        val clone = recordRefs.values.mapNotNull(::forgetInternal)
        recordRefs.clear()
        return clone
    }

    private fun forgetInternal(ref: WeakReference<REC>): WeakReference<REC>? {
        val rec = ref.get() ?: return null
        rec.isManaged = false
        return ref
    }

    internal fun onStructuralChange() {
        // TODO: trigger this only if something has changed

        selections.values.iterateWeakOrRemove(::updateSelection)

        counts.values.iterateWeakOrRemove {
            (it.original as MutableProperty).let { it.value = it.value } // trigger update
        }
    }

    internal fun onOrderChange(affectedCols: List<Pair<Table<SCH, *, *>, String>>) {
        affectedCols.forEach { (_, col) ->
            selectionsByOrder[col]?.iterateWeakOrRemove(::updateSelection)
        }
    }

    private fun updateSelection(selection: Property<List<REC>>) {
        val mapped = selection as `Mapped-`<*, *>
        val distinct = mapped.original as `Distinct-`
        val anotherMapped = distinct.original as `Mapped-`<*, *>
        val mutable = anotherMapped.original as MutableProperty<WhereCondition<out SCH>> // lol, some bad code right here
        mutable.value = mutable.value
    }

    fun dump(prefix: String, sb: StringBuilder) {
        sb.append(prefix).append("records\n")
        recordRefs.forEach { (id, recRef) ->
            recRef.get()?.let { rec ->
                sb.append(prefix).append(" ").append(id).append(" : ").append(rec).append("\n")
            }
        }

        sb.append(prefix).append("counts\n")
        counts.entries.forEach { (cond, ref) ->
            if (ref.get() != null)
                sb.append(prefix).append(" ").let { cond.appendSqlTo(dialect, it) }.append("\n")
        }

        sb.append(prefix).append("selections\n")
        selections.forEach { (cor, ref) ->
            if (ref.get() !== null) {
                sb.append(prefix).append(" ").let {
                    cor.condition.appendSqlTo(dialect, it)
                    if (!cor.order.isEmpty()) {
                        it.append(" ORDER BY ...")
                    }
                }
                sb.append("\n")
            }
        }

    }

    private fun <K, V : Any> ConcurrentMap<K, WeakReference<V>>.getWeakOrRemove(key: K): V? {
        val ref = get(key)
        return if (ref === null) {
            null
        } else {
            val value = ref.get()
            if (value === null) {
                remove(key, ref)
                null
            } else {
                value
            }
        }
    }

    private inline fun <E : Any> MutableIterable<WeakReference<E>>.iterateWeakOrRemove(func: (E) -> Unit) {
        val itr = iterator()
        while (itr.hasNext()) {
            val ref = itr.next()
            val el = ref.get()
            if (el === null) itr.remove()
            else func(el)
        }
    }

    // region Dao implementation

    override fun find(id: ID): REC? =
            when (lowSession.fetchCount(table, lowSession.reusableCond(table, table.idColName, id))) {
                0L -> null
                1L -> recordRefs.getOrPutWeak(id) { table.newRecord(session, id) }
                else -> throw AssertionError()
            }

    override fun select(condition: WhereCondition<out SCH>, vararg order: Order<SCH>): Property<List<REC>> {
        val cor = ConditionAndOrder(condition, order)
        val ref: WeakReference<Property<List<REC>>>
        val prop: Property<List<REC>>
        selections.getOrPutWeak(cor, {
            concurrentPropertyOf(cor)
                    .map(PrimaryKeys(table, lowSession))
                    .distinct(Arrayz.Equal)
                    .map(Query(this))
        }) { r, p ->
            // ugly one ;)
            ref = r
            prop = p
        }
        order.forEach { selectionsByOrder.getOrPut(/* TODO: */ it.col.name, ::CopyOnWriteArraySet).add(ref) }
        return prop
    }

    override fun count(condition: WhereCondition<out SCH>): Property<Long> =
            counts.getOrPutWeak(condition) {
                concurrentPropertyOf(condition).map(Count(table, lowSession)) as `Mapped-`<WhereCondition<out SCH>, Long>
            }

    // endregion Dao implementation

    // region Manager implementation

    override fun <T> getDirty(column: NamedLens<SCH, Struct<SCH>, T>, id: ID): T {
        val transaction = lowSession.transaction ?: return unset()

        val thisCol = transaction.updated?.getFor(table, column.name) ?: return unset()
        // we've created this column, we can cast it to original type

        return if (thisCol.containsKey(id)) thisCol[id] as T else unset()
        // 'as T' is safe since thisCol won't contain non-T value
    }

    override fun <T> getClean(column: NamedLens<SCH, Struct<SCH>, T>, id: ID): T =
            table.delegateFor(column).fetch(session, lowSession, table, column, id)

    override fun <T> set(transaction: Transaction, column: NamedLens<SCH, Struct<SCH>, T>, id: ID, update: T) {
        val ourTransact = lowSession.transaction
        if (transaction !== ourTransact)
            throw IllegalStateException(
                    if (ourTransact === null) "This can be performed only within a transaction"
                    else "Wrong transaction: requested $transaction, but session's is $ourTransact"
            )
        ourTransact.checkOpenAndThread()

        val dirty = getDirty(column, id)
        val cleanEquals = dirty === Unset && getClean(column, id) === update
        if (dirty === update || cleanEquals) {
            return
        }

        transaction.update(table, id, column, update)
    }

    // endregion Manager implementation

    override fun toString(): String = "Dao(for $table in $session)"

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> unset(): T =
            Unset as T

}

private inline fun <K, V : Any> ConcurrentMap<K, WeakReference<V>>.getOrPutWeak(key: K, create: () -> V): V =
        getOrPutWeak(key, create) { _, v -> v }

@UseExperimental(ExperimentalContracts::class)
private inline fun <K, V : Any, R> ConcurrentMap<K, WeakReference<V>>.getOrPutWeak(key: K, create: () -> V, success: (WeakReference<V>, V) -> R): R {
    contract {
        callsInPlace(success, InvocationKind.EXACTLY_ONCE)
    }

    while (true) {
        val ref = getOrPut(key) {
            // putIfAbsent here may return either newly created or concurrently inserted value
            WeakReference(create())
        }
        val value = ref.get()
        if (value === null) remove(key, ref)
        else return success(ref, value)
    }
}
