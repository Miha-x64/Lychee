package net.aquadc.properties.sql

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.map
import net.aquadc.properties.propertyOf
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.struct.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.getOrSet

// TODO: evicting stale records, counts, and selections
internal class RealDao<REC : Record<REC, ID>, ID : IdBound>(
        private val session: Session,
        private val lowSession: LowLevelSession,
        private val table: Table<REC, ID>,
        private val dialect: Dialect
) : Dao<REC, ID>, Manager<Transaction> {

    private val records = ConcurrentHashMap<Long, REC>()

    // SELECT COUNT(*) WHERE ...
    private val counts = ConcurrentHashMap<WhereCondition<out REC>, MutableProperty<WhereCondition<out REC>>>()

    // SELECT _id WHERE ...
    private val selections = Vector<MutableProperty<WhereCondition<out REC>>>() // coding like in 1995, yay! TODO: deduplication

    internal fun <T> commitValue(localId: Long, column: Col<REC, T>, value: T) {
        (records[localId]?.fields?.get(column.ordinal) as ManagedProperty<Transaction, T>?)?.commit(value)
    }

    internal fun dropManagement(localId: Long) {
        records.remove(localId)?.fields?.forEach(ManagedProperty<*, *>::dropManagement)
    }

    internal fun onStructuralChange() {
        // TODO: trigger this only if something has changed

        selections.forEach {
            it.value = it.value
        }
        counts.forEach { (_, it) ->
            it.value = it.value
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
        return records.getOrPut<Long, REC>(localId) { table.create(session, id) }
    }

    override fun select(condition: WhereCondition<out REC>): Property<List<REC>> =
            propertyOf(condition)
                    .also { selections.add(it) }
                    .map(Query(this, table, lowSession))

    override fun count(condition: WhereCondition<out REC>): Property<Long> =
            counts.getOrPut(condition) {
                propertyOf(condition)
            }.map(Count(table, lowSession)) // todo cache, too

    // endregion Dao implementation

    // region low-level Dao implementation

    override fun <T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<Transaction, T> {
        val localId = lowSession.localId(table, id)
        return ManagedProperty(this, col, localId)
    }

    // endregion low-level Dao implementation

    // region Manager implementation

    override fun <T> getDirty(column: Field<*, T, *>, id: Long): T {
        val transaction = lowSession.transaction ?: return unset()

        val thisCol = transaction.updated?.getFor(column as Col<REC, T>) ?: return unset()
        // we've created this column, we can cast it to original type

        val localId = id
        return if (thisCol.containsKey(localId)) thisCol[localId] as T else unset()
        // 'as T' is safe since thisCol won't contain non-T value
    }

    @Suppress("UPPER_BOUND_VIOLATED")
    override fun <T> getClean(column: Field<*, T, *>, id: Long): T {
        val col = column as Col<REC, T>
        val primaryKey = lowSession.primaryKey(table, id)
        val condition = lowSession.reusableCond(table, table.idColName, primaryKey)
        return lowSession.fetchSingle(col, table, condition)
    }

    override fun <T> set(transaction: Transaction, column: Field<*, T, *>, id: Long, update: T) {
        column as Col<REC, T>
        val ourTransact = lowSession.transaction
        if (transaction !== ourTransact) {
            if (ourTransact === null)
                throw IllegalStateException("This can be performed only within a transaction")
            else
                throw IllegalStateException("Wrong transaction: requested $transaction, but session's is $ourTransact")
        }
        ourTransact.checkOpenAndThread()

        val dirty = getDirty(column, id)
        val cleanEquals = dirty === Unset && getClean(column, id) === update
        if (dirty === update || cleanEquals) {
            return
        }

        transaction.update<REC, ID, T>(table, lowSession.primaryKey(table, id), column, update)
    }

    // endregion Manager implementation

    override fun toString(): String = "Dao(for $table in $session)"

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> unset(): T =
            Unset as T

}
