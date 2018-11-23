package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema

@Suppress(
        "PLATFORM_CLASS_MAPPED_TO_KOTLIN", // using finalization guard
        "ReplacePutWithAssignment" // shut up, I want to write my code in cute columns
)
internal class RealTransaction(
        override val session: Session,
        private val lowSession: LowLevelSession
) : java.lang.Object(), Transaction {

    private val createdAt = Exception()
    private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
    private var isSuccessful = false

    // table : local IDs
    private var inserted: HashMap<Table<*, *, *>, ArrayList<Long>>? = null

    // column : localId : value
    internal var updated: UpdatesHashMap? = null

    // table : local IDs
    private var deleted: HashMap<Table<*, *, *>, ArrayList<Long>>? = null

    // TODO: use special collections for Longs

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(
            table: Table<SCH, ID, *>, vararg contentValues: ColValue<SCH, *>
    ): ID {
        checkOpenAndThread()

        val size = contentValues.size
        val cols = arrayOfNulls<FieldDef<SCH, *>>(size)
        val vals = arrayOfNulls<Any>(size)
        scatter(contentValues, colsToFill = cols, valsToFill = vals)
        cols as Array<FieldDef<SCH, *>>

        val id = lowSession.insert(table, cols, vals)
        val localId = lowSession.localId(table, id)

        // remember we've added a record
        (inserted ?: HashMap<Table<*, *, *>, ArrayList<Long>>().also { inserted = it })
                .getOrPut(table, ::ArrayList)
                .add(localId)

        // write all insertion fields as updates
        val updated = updated ?: UpdatesHashMap().also { updated = it }
        contentValues.forEach {
            when (it.col) {
                is FieldDef.Mutable -> updated.put(table, it, localId)
                is FieldDef.Immutable -> { }
            }.also { }
        }

        return id
    }

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            table: Table<SCH, ID, *>, id: ID, column: FieldDef.Mutable<SCH, T>, value: T
    ) {
        checkOpenAndThread()

        lowSession.update(table, id, column, value)

        (updated ?: HashMap<Pair<Table<*, *, *>, FieldDef.Mutable<*, *>>, HashMap<Long, Any?>>().also { updated = it })
                .getOrPut(table to column, ::HashMap)
                .put(lowSession.localId(table, id), value)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>) {
        checkOpenAndThread()
        check(session === record.session)

        val table = record.table
        val localId = lowSession.deleteAndGetLocalId(table, record.primaryKey)

        (deleted ?: HashMap<Table<*, *, *>, ArrayList<Long>>().also { deleted = it })
                .getOrPut(table, ::ArrayList)
                .add(localId)
    }

    override fun setSuccessful() {
        checkOpenAndThread()
        isSuccessful = true
    }

    override fun close() {
        checkOpenAndThread()
        lowSession.onTransactionEnd(isSuccessful)
        thread = null
    }


    internal fun deliverChanges() {
        val del = deleted
        del?.forEach { (table, localIDs) ->
            lowSession.daos[table.erased]?.let { man ->
                localIDs.forEach(man::dropManagement)
            }
        }
        // Deletions first! Now we're not going to disturb souls of dead records & properties.

        // value changes
        val upd = updated
        upd?.forEach { (tblToCol, localIdToVal) ->
            val (table, col) = tblToCol
            localIdToVal.forEach { (localId, value) ->
                lowSession.daos[table]?.erased?.commitValue(localId, col.erased, value)
                Unit
            }
        }

        // structure changes
        val ins = inserted
        if (ins != null || del != null || upd != null) {
            val changedTables = (ins?.keys ?: emptySet<Table<*, *, *>>()) + (del?.keys ?: emptySet())

            lowSession.daos.forEach { (table, dao) ->
                if (table in changedTables) {
                    dao.onStructuralChange()
                } else if (upd != null) {
                    val updatedInTable = upd.keys.filter { it.first == table }
                    if (updatedInTable.isNotEmpty()) {
                        @Suppress("UPPER_BOUND_VIOLATED", "UNCHECKED_CAST")
                        dao.erased.onOrderChange(updatedInTable as List<Pair<Table<Any, *, *>, FieldDef<Any, *>>>)
                    }
                }

            }
        }
    }


    internal fun checkOpenAndThread() {
        check(thread === Thread.currentThread()) {
            if (thread === null) "this transaction was already closed" else "called from wrong thread"
        }
    }

    override fun finalize() {
        if (thread !== null) {
            throw IllegalStateException("unclosed transaction being finalized, originally created at", createdAt)
        }
    }

    private fun <SCH : Schema<SCH>> scatter(
            contentValues: Array<out ColValue<SCH, *>>,
            colsToFill: Array<FieldDef<SCH, *>?>, valsToFill: Array<Any?>) {
        contentValues.forEachIndexed { i, pair ->
            colsToFill[i] = pair.col
            valsToFill[i] = pair.value
        }
    }

    @Suppress("UPPER_BOUND_VIOLATED")
    private inline val RealDao<*, *, *>.erased
        get() = this as RealDao<Any, IdBound, Record<Any, IdBound>>

}
