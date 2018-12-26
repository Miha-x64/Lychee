package net.aquadc.properties.sql

import net.aquadc.properties.*
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.properties.function.Arrayz
import net.aquadc.properties.internal.IdManagedProperty
import net.aquadc.properties.internal.`Mapped-`
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// TODO: evicting stale records, counts, and selections
internal class RealDao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val session: Session,
        private val lowSession: LowLevelSession,
        private val table: Table<SCH, ID, REC>,
        private val dialect: Dialect
) : Dao<SCH, ID, REC>, Manager<SCH, Transaction>() {

    private val records = ConcurrentHashMap<Long, REC>()

    // SELECT COUNT(*) WHERE ...
    private val counts = ConcurrentHashMap<WhereCondition<out SCH>, `Mapped-`<WhereCondition<out SCH>, Long>>()

    // SELECT _id WHERE ...
    private val selections = Vector<MutableProperty<WhereCondition<out SCH>>>() // coding like in 1995, yay! TODO: deduplication

    private val orderedSelections = ConcurrentHashMap<FieldDef<SCH, *>, Vector<MutableProperty<WhereCondition<out SCH>>>>()

    @Suppress("UNCHECKED_CAST")
    internal fun <T> commitValue(localId: Long, column: FieldDef.Mutable<SCH, T>, value: T) {
        (records[localId]?.values?.get(column.ordinal.toInt()) as ManagedProperty<SCH, Transaction, T>?)?.commit(value)
    }

    internal fun dropManagement(localId: Long) {
        records.remove(localId)?.let { record ->
            val defs = record.table.schema.fields
            val fields = record.values
            for (i in defs.indices) {
                when (defs[i]) {
                    is FieldDef.Mutable -> (fields[i] as ManagedProperty<*, *, *>).dropManagement()
                    is FieldDef.Immutable -> { /* no-op */ }
                }.also {  }
            }
            record.isManaged = false
        }
    }

    internal fun onStructuralChange() {
        // TODO: trigger this only if something has changed

        selections.forEach {
            it.value = it.value
        }
        counts.values.forEach { it ->
            (it.original as MutableProperty).let {
                it.value = it.value
            }
        }
    }

    internal fun onOrderChange(affectedCols: List<Pair<Table<SCH, *, *>, FieldDef<SCH, *>>>) {
        affectedCols.forEach { (_, col) ->
            orderedSelections[col]?.forEach {
                it.value = it.value
            }
        }
    }

    fun dump(prefix: String, sb: StringBuilder) {
        sb.append(prefix).append("records\n")
        records.forEach { (localId, rec) ->
            sb.append(prefix).append(" ").append(localId).append(" : ").append(rec).append("\n")
        }

        sb.append(prefix).append("counts\n")
        counts.keys.forEach { cond ->
            sb.append(prefix).append(" ").let { cond.appendSqlTo(dialect, it) }.append("\n")
        }

        sb.append(prefix).append("selections\n")
        selections.forEach { sel ->
            sb.append(prefix).append(" ").let { sel.value.appendSqlTo(dialect, it) }.append("\n")
        }

    }

    // region Dao implementation

    override fun find(id: ID): REC? {
        if (!lowSession.exists(table, id)) return null
        val localId = lowSession.localId(table, id)
        return records.getOrPut<Long, REC>(localId) { table.newRecord(session, id) }
    }

    override fun select(condition: WhereCondition<out SCH>, vararg order: Order<SCH>): Property<List<REC>> {
        val prop = concurrentPropertyOf(condition)
        selections.add(prop)
        order.forEach { orderedSelections.getOrPut(it.col, ::Vector).add(prop) }
        return prop
                .map(PrimaryKeys(table, lowSession, order))
                .distinct(Arrayz.Equal)
                .map(Query(this, table, lowSession, condition, order))
    }

    override fun count(condition: WhereCondition<out SCH>): Property<Long> =
            counts.getOrPut(condition) {
                concurrentPropertyOf(condition).map(Count(table, lowSession)) as `Mapped-`<WhereCondition<out SCH>, Long>
            }

    // endregion Dao implementation

    // region low-level Dao implementation

    override fun <T> createFieldOf(col: FieldDef.Mutable<SCH, T>, id: ID): ManagedProperty<SCH, Transaction, T> {
        val localId = lowSession.localId(table, id)
        return IdManagedProperty(this, col, localId, unset())
    }

    override fun <T> getValueOf(col: FieldDef<SCH, T>, id: ID): T =
            getValueInternal(col, id)

    // endregion low-level Dao implementation

    // region Manager implementation

    override fun <T> getDirty(field: FieldDef.Mutable<SCH, T>, id: Long): T {
        val transaction = lowSession.transaction ?: return unset()

        val thisCol = transaction.updated?.getFor(table, field) ?: return unset()
        // we've created this column, we can cast it to original type

        val localId = id
        return if (thisCol.containsKey(localId)) thisCol[localId] as T else unset()
        // 'as T' is safe since thisCol won't contain non-T value
    }

    @Suppress("UPPER_BOUND_VIOLATED")
    override fun <T> getClean(field: FieldDef.Mutable<SCH, T>, id: Long): T {
        val primaryKey = lowSession.primaryKey(table, id)
        return getValueInternal(field, primaryKey)
    }

    private fun <T> getValueInternal(field: FieldDef<SCH, T>, primaryKey: ID): T {
        val condition = lowSession.reusableCond(table, table.idColName, primaryKey)
        return lowSession.fetchSingle(field, table, condition)
    }

    override fun <T> set(transaction: Transaction, field: FieldDef.Mutable<SCH, T>, id: Long, update: T) {
        val ourTransact = lowSession.transaction
        if (transaction !== ourTransact) {
            if (ourTransact === null)
                throw IllegalStateException("This can be performed only within a transaction")
            else
                throw IllegalStateException("Wrong transaction: requested $transaction, but session's is $ourTransact")
        }
        ourTransact.checkOpenAndThread()

        val dirty = getDirty(field, id)
        val cleanEquals = dirty === Unset && getClean(field, id) === update
        if (dirty === update || cleanEquals) {
            return
        }

        transaction.update(table, lowSession.primaryKey(table, id), field, update)
    }

    // endregion Manager implementation

    override fun toString(): String = "Dao(for $table in $session)"

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> unset(): T =
            Unset as T

}
