package net.aquadc.properties.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import java.lang.ref.WeakReference

@Suppress(
        "PLATFORM_CLASS_MAPPED_TO_KOTLIN", // using finalization guard
        "ReplacePutWithAssignment" // shut up, I want to write my code in cute columns
)
internal class RealTransaction(
        private val session: Session,
        private val lowSession: LowLevelSession
) : java.lang.Object(), Transaction {

    private val createdAt = Exception()
    private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
    private var isSuccessful = false

    // table : IDs
    private var inserted: MutableMap<Table<*, *, *>, ArrayList<IdBound>>? = null

    // column : ID : value
    internal var updated: UpdatesMap? = null

    // table : IDs
    private var deleted: MutableMap<Table<*, *, *>, Any>? = null // MutableMap<Table, ArrayList<IdBound> | Unit>; fixme: without RWLock this will be wrong

    // TODO: use special collections for Longs

    override fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound> replace(
            table: Table<SCH, ID, REC>, data: Struct<SCH>
    ): REC {
        checkOpenAndThread()

        val id = lowSession.replace(table, data)

        // remember we've added a record
        (inserted ?: New.map<Table<*, *, *>, ArrayList<IdBound>>().also { inserted = it })
                .getOrPut(table, ::ArrayList)
                .add(id)

        // write all insertion fields as updates
        val updated = updated ?: UpdatesMap().also { updated = it }

        val fields = table.schema.fields
        for (i in fields.indices) {
            val field = fields[i]
            when (field) {
                is FieldDef.Mutable -> updated.put(table, /* todo */ field.name, data[field], id)
                is FieldDef.Immutable -> { }
            }.also { }
        }

        return session[table].require(id)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound, T> update(
            table: Table<SCH, ID, *>, id: ID, column: FieldDef.Mutable<SCH, T>, columnName: String, value: T
    ) {
        checkOpenAndThread()

        lowSession.update(table, id, column, columnName, value)

        (updated ?: UpdatesMap().also { updated = it })
                .getOrPut(table to columnName, New::map)
                .put(id, value)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>) {
        checkOpenAndThread()
        check(session === record._session)

        val table = record.table
        val id = record.primaryKey
        lowSession.delete(table, id)

        val del = deletedMap()
        if (del[table] != Unit) {
            (del as MutableMap<Table<*, *, *>, ArrayList<IdBound>>).getOrPut(table, ::ArrayList).add(id)
        }
    }

    override fun truncate(table: Table<*, *, *>) {
        checkOpenAndThread()
        lowSession.truncate(table)
        deletedMap()[table] = Unit
    }

    private fun deletedMap() =
            deleted ?: New.map<Table<*, *, *>, Any>().also { deleted = it }

    override fun setSuccessful() {
        checkOpenAndThread()
        isSuccessful = true
    }

    override fun close() {
        checkOpenAndThread()
        lowSession.onTransactionEnd(isSuccessful)
        thread = null
    }

    @Suppress("UNCHECKED_CAST")
    internal fun deliverChanges() {
        val del = deleted

        // Deletions first! Now we're not going to disturb souls of dead records & properties during this notification.
        var unmanage: MutableMap<Table<*, *, *>, List<WeakReference<out Record<*, *>>>>? = null
        if (del != null) {
            for ((table, ids) in del) { // forEach here will be unable to smart-case `unmanage` var
                val man = lowSession.daos[table.erased] as RealDao<*, IdBound, *>?
                if (man != null) {
                    val removed: List<WeakReference<out Record<*, *>>> = if (ids is Unit) {
                        man.truncate()
                    } else {
                        ids as ArrayList<IdBound>
                        ids.mapNotNull(man::forget)
                    }
                    if (removed.isNotEmpty()) {
                        if (unmanage === null) unmanage = New.map()
                        unmanage[table] = removed
                    }
                }
            }
        }

        // value changes
        val upd = updated
        upd?.forEach { (tblToCol, idToVal) ->
            val (table, colName) = tblToCol
            idToVal.forEach { (id, value) ->
                lowSession.daos[table]?.erased?.commitValue(id, colName, value)
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
                        @Suppress("UPPER_BOUND_VIOLATED")
                        dao.erased.onOrderChange(updatedInTable as List<Pair<Table<Any, *, *>, String>>)
                    }
                }

            }
        }

        // commit 'unmanaged' status for all the properties which lost their management.
        unmanage?.forEach { (table, refs) ->
            val man = (lowSession.daos[table.erased] as RealDao<*, IdBound, Record<*, IdBound>>)
            refs.forEach { ref ->
                ref.get()?.let { rec ->
                    man.dropRecordManagement(rec as Record<*, IdBound>)
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

    @Suppress("UPPER_BOUND_VIOLATED")
    private inline val RealDao<*, *, *>.erased
        get() = this as RealDao<Any, IdBound, Record<Any, IdBound>>

}
