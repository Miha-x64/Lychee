package net.aquadc.persistence.sql

import net.aquadc.persistence.New
import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.properties.internal.Unset
import java.lang.ref.WeakReference
import java.util.BitSet


@Suppress(
        "ReplacePutWithAssignment", "ReplaceGetOrSet" // shut up, I want to write my code in cute columns
)
internal class RealTransaction(
        private val session: Session<*>,
        private val lowSession: LowLevelSession<*, *>
) : Transaction {

    private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
    private var isSuccessful = false

    // table : IDs
    private var inserted: MutableMap<Table<*, *, *>, ArrayList<IdBound>>? = null

    // column : ID : value
    internal var updated: UpdatesMap? = null

    // table : IDs
    private var deleted: MutableMap<Table<*, *, *>, Any>? = null // MutableMap<Table, ArrayList<IdBound> | Unit>; fixme: without RWLock this will be wrong

    // TODO: use special collections for Longs

    override fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound> insert(
            table: Table<SCH, ID, REC>, data: Struct<SCH>
    ): REC {
        checkOpenAndThread()

        val id = lowSession.insert(table, data)

        // remember we've added a record
        (inserted ?: New.map<Table<*, *, *>, ArrayList<IdBound>>().also { inserted = it })
                .getOrPut(table, ::ArrayList)
                .add(id)

        // write all insertion fields as updates
        /* hmm, looks like we don't need it, uncommitted values will be read from DB if ever requested TODO consider
        val updated = updated ?: UpdatesMap().also { updated = it }

        val fields = table.schema.fields
        for (i in fields.indices) {
            val field = fields[i]
            when (field) {
                is FieldDef.Mutable -> updated.put(table, field.name hey, what about nested ones?, data[field], id)
                is FieldDef.Immutable -> { }
            }.also { }
        }*/

        return session[table].require(id)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>, T> update(
            table: Table<SCH, ID, REC>, id: ID, field: FieldDef.Mutable<SCH, T, *>, previous: T, value: T
    ) {
        checkOpenAndThread()
        val updates = (updated ?: UpdatesMap().also { updated = it })
                .getOrPut(table, New::map)
                .getOrPut(id) { Array<Any?>(table.schema.mutableFields.size) { Unset } }

        table.delegateFor(field).update(session, lowSession, table, field, id, previous, value)
        updates[field.mutableOrdinal.toInt()] = value
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>) {
        checkOpenAndThread()
        check(session === record._session)

        val table = record.table
        val id = record.primaryKey
        lowSession.delete(table as Table<SCH, ID, Record<SCH, ID>>, id)

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
        var unmanage: ArrayList<WeakReference<out Record<*, *>>>? = null
        if (del != null) {
            for ((table, ids) in del) { // forEach here will be unable to smart-cast `unmanage` var
                val man = lowSession.daos[table.erased] as RealDao<*, IdBound, *, *>?
                if (man != null) {
                    if (unmanage == null) unmanage = ArrayList()
                    if (ids is Unit) man.truncateLocked(removedRefsTo = unmanage)
                    else (ids as ArrayList<IdBound>).mapNotNullTo(unmanage, man::forget)
                }
            }
        }

        // value changes
        val upd = updated?.onEach { (table, idToRec) ->
            idToRec.forEach { (id, upd) ->
                lowSession.daos[table]?.erased?.getCached(id)?.let { rec ->
                    table.erased.commitValues(rec, upd)
                }
            }
        }

        // structure changes
        val ins = inserted
        if (ins != null || del != null || upd != null) {
            val changedTables = (ins?.keys ?: emptySet<Table<*, *, *>>()) + (del?.keys ?: emptySet())

            var updatedCols: BitSet? = null
            for ((table, dao) in lowSession.daos) {
                if (table in changedTables) {
                    dao.onStructuralChange()
                } else if (upd != null) {
                    if (updatedCols === null) updatedCols = BitSet(table.columns.size)

                    upd[table]?.values?.forEach { values ->
                        for (i in table.schema.mutableFields.indices) {
                            if (values[i] !== Unset) updatedCols.set(i)
                        }
                    }
                    if (!updatedCols.isEmpty) {
                        dao.onOrderChange(updatedCols)
                        updatedCols.clear()
                    }
                }
            }
        }

        // commit 'unmanaged' status for all the properties which lost their management.
        unmanage?.forEach { ref ->
            ref.get()?.let(Record<*, *>::dropManagement)
        }
    }


    internal fun checkOpenAndThread() {
        check(thread === Thread.currentThread()) {
            if (thread === null) "this transaction was already closed" else "called from wrong thread"
        }
    }

    @Suppress("UPPER_BOUND_VIOLATED")
    private inline val RealDao<*, *, *, *>.erased
        get() = this as RealDao<Any, IdBound, Record<Any, IdBound>, *>

}
