package net.aquadc.persistence.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.concurrentPropertyOf
import net.aquadc.properties.distinct
import net.aquadc.properties.function.Arrayz
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.`Distinct-`
import net.aquadc.properties.internal.`Mapped-`
import net.aquadc.properties.map
import net.aquadc.persistence.sql.dialect.Dialect
import java.lang.ref.WeakReference
import java.util.BitSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArraySet


internal class RealDao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>, STMT>(
        private val session: Session<*>,
        private val lowSession: LowLevelSession<STMT, *>,
        private val table: Table<SCH, ID, REC>,
        private val dialect: Dialect
) : Dao<SCH, ID, REC> {

    // these three are guarded by RW lock
    internal var insertStatement: STMT? = null
    internal val updateStatements = New.map<Any, STMT>()
    internal var deleteStatement: STMT? = null


    // there's no ReferenceQueue, just evict when reference gets nulled out
    private val recordRefs = ConcurrentHashMap<ID, WeakReference<REC>>()

    // SELECT COUNT(*) WHERE ...
    private val counts = ConcurrentHashMap<WhereCondition<SCH>, WeakReference<`Mapped-`<WhereCondition<SCH>, Long>>>()

    // SELECT _id WHERE ...
    private val selections = ConcurrentHashMap<ConditionAndOrder<SCH>, WeakReference<Property<List<REC>>>>()
    // same items here, but in different format
    private val selectionsByOrder = Array(table.columns.size) { _ ->
        CopyOnWriteArraySet<WeakReference<Property<List<REC>>>>()
    }

    internal fun getCached(id: ID): REC? =
            recordRefs.getWeakOrRemove(id)

    internal fun forget(id: ID): WeakReference<REC>? =
            recordRefs.remove(id)?.let(::forgetInternal)

    internal fun truncateLocked(removedRefsTo: ArrayList<in WeakReference<out REC>>) {
        recordRefs.values.mapNotNullTo(removedRefsTo, ::forgetInternal)
        recordRefs.clear()
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

    internal fun onOrderChange(affectedCols: BitSet) {
        for (i in 0 until affectedCols.length()) {
            if (affectedCols[i]) {
                selectionsByOrder[i].iterateWeakOrRemove(::updateSelection)
            }
        }
    }

    private fun updateSelection(selection: Property<List<REC>>) {
        val mapped = selection as `Mapped-`<*, *>
        val distinct = mapped.original as `Distinct-`
        val anotherMapped = distinct.original as `Mapped-`<*, *>
        val mutable = anotherMapped.original as MutableProperty<WhereCondition<SCH>> // lol, some bad code right here
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
                sb.append(prefix).append(" ").let { cond.appendSqlTo(table, dialect, it) }.append("\n")
        }

        sb.append(prefix).append("selections\n")
        selections.forEach { (cor, ref) ->
            if (ref.get() !== null) {
                sb.append(prefix).append(" ").let {
                    cor.condition.appendSqlTo(table, dialect, it)
                    if (cor.order.isNotEmpty()) {
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

    private inline fun <E : Any> MutableCollection<WeakReference<E>>.iterateWeakOrRemove(func: (E) -> Unit) {
        if (isNotEmpty()) {
            // every damn Java collection creates new iterator even if it is empty and may just return emptyIterator()
            val itr = iterator()
            while (itr.hasNext()) {
                val ref = itr.next()
                val el = ref.get()
                if (el === null) itr.remove()
                else func(el)
            }
        }
    }

    // region Dao implementation

    override fun find(id: ID): REC? =
            when (lowSession.fetchCount(table, lowSession.pkCond(table, id))) {
                0L -> null
                1L -> recordRefs.getOrPutWeak(id) { table.newRecord(session, id) }
                else -> throw AssertionError()
            }

    override fun select(condition: WhereCondition<SCH>, vararg order: Order<SCH>): Property<List<REC>> {
        val cor = ConditionAndOrder(condition, order)
        val ref: WeakReference<Property<List<REC>>>
        val prop: Property<List<REC>>
        selections.getOrPutWeak(cor, {
            concurrentPropertyOf(cor)
                    .map(PrimaryKeys(table, lowSession))
                    .distinct(Arrayz.Equal)
                    .map { primaryKeys -> ListSelection(this, primaryKeys) }
        }) { r, p -> // ugly one ;)
            ref = r
            prop = p
        }
        order.forEach { ord ->
            selectionsByOrder[table.columnIndices[ord.col]!!].add(ref)
        }
        return prop
    }

    override fun count(condition: WhereCondition<SCH>): Property<Long> =
            counts.getOrPutWeak(condition) {
                concurrentPropertyOf(condition).map(Count(table, lowSession)) as `Mapped-`<WhereCondition<SCH>, Long>
            }

    // endregion Dao implementation

    // region Manager implementation

    override fun <T> getDirty(field: FieldDef.Mutable<SCH, T, *>, id: ID): T {
        val thisRec = lowSession.transaction?.updated?.get(table)?.get(id) ?: return unset()
        return thisRec[field.mutableOrdinal.toInt()] as T
    }

    override fun <T> getClean(field: FieldDef<SCH, T, *>, id: ID): T =
            table.delegateFor(field).fetch(session, lowSession, table, field, id)

    override fun <T> set(transaction: Transaction, field: FieldDef.Mutable<SCH, T, *>, id: ID, previous: T, update: T) {
        val ourTransact = lowSession.transaction
        if (transaction !== ourTransact)
            error("Wrong transaction: requested $transaction, but session's is $ourTransact")
        ourTransact.checkOpenAndThread()

        transaction.update(table, id, field, previous, update)
    }

    // endregion Manager implementation

    override fun toString(): String = "Dao(for $table in $session)"

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> unset(): T =
            Unset as T

}
