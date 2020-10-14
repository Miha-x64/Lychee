package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct


internal class RealTransaction(
    private val lowSession: LowLevelSession<*, *>
) : Transaction {

    private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
    private var isSuccessful = false

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(
            table: Table<SCH, ID>, data: Struct<SCH>
    ): ID {
        checkOpenAndThread()
        return lowSession.insert(table, data)
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID) {
        checkOpenAndThread()
        lowSession.delete(table, id)
    }

    override fun truncate(table: Table<*, *>) {
        checkOpenAndThread()
        lowSession.truncate(table)
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
