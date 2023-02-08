package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema


internal class RealTransaction<SRC>(
    override val mySession: Session<SRC>,
    @JvmField internal val lowSession: LowLevelSession<*, *>
) : Transaction<SRC>, Session<SRC> by mySession {

    private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
    private var isSuccessful = false

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(
        table: Table<SCH, ID>, data: PartialStruct<SCH>
    ): ID {
        checkOpenAndThread()
        return lowSession.insert(table, data)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>) {
        checkOpenAndThread()
        lowSession.update(table, id, patch)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID) {
        checkOpenAndThread()
        lowSession.delete(table, id)
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

    private fun checkOpenAndThread() {
        check(thread === Thread.currentThread()) {
            if (thread === null) "this transaction was already closed" else "called from wrong thread"
        }
    }
}
