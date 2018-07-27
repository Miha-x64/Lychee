@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.concurrent.locks.ReentrantReadWriteLock

// TODO: support dialects
class JdbcSqliteSession(private val connection: Connection) : Session {

    init {
        connection.autoCommit = false
    }

    private val lock = ReentrantReadWriteLock()

    // transactional things, guarded by write-lock
    private var transaction: JdbcTransaction? = null
    private val insertStatements = HashMap<Pair<Table<*, *>, List<Col<*, *>>>, PreparedStatement>()

    private fun <REC : Record<REC, *>> insertStatementWLocked(table: Table<REC, *>, cols: Array<Col<REC, *>>): PreparedStatement {
        val cacheKey = Pair(table, cols.asList())
        val cached = insertStatements[cacheKey]
        if (cached != null) return cached

        val stmt = connection.prepareStatement(insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
        insertStatements[cacheKey] = stmt
        return stmt
    }

    override fun beginTransaction(): Transaction {
        val wLock = lock.writeLock()
        check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in transaction" }
        wLock.lock()
        val tr = JdbcTransaction()
        transaction = tr
        return tr
    }

    private fun onTransactionEnd(successful: Boolean) {
        checkNotNull(transaction)
        try {
            if (successful) connection.commit() else connection.rollback()
            transaction = null
        } finally {
            lock.writeLock().unlock()
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // using finalization guard
    inner class JdbcTransaction : java.lang.Object(),  Transaction {

        private val createdAt = Exception()
        private var thread: Thread? = Thread.currentThread()
        private var isSuccessful = false

        override val session: Session
            get() = this@JdbcSqliteSession

        override fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, vararg contentValues: ColValue<REC, *>): ID {
            checkOpenAndThread()

            val size = contentValues.size
            val vals = arrayOfNulls<Any>(size)
            val cols = arrayOfNulls<Col<REC, *>>(size)
            scatter(contentValues, colsToFill = cols, valsToFill = vals)
            cols as Array<Col<REC, *>> // non-nulls, I PROVE IT

            val statement = insertStatementWLocked(table, cols)
            vals.forEachIndexed { idx, x -> statement.setObject(idx + 1, x) }
            check(statement.executeUpdate() == 1)
            val keys = statement.generatedKeys
            check(keys.next())
            return keys.getObject(1) as ID
        }

        override fun setSuccessful() {
            isSuccessful = true
        }

        override fun close() {
            checkOpenAndThread()
            onTransactionEnd(isSuccessful)
            thread = null
        }


        private fun checkOpenAndThread() {
            check(thread == Thread.currentThread()) {
                if (thread == null) "this transaction was already closed" else "called from wrong thread"
            }
        }

        override fun finalize() {
            check(thread == null) {
                throw IllegalStateException("unclosed transaction being finalized, originally created at", createdAt)
            }
        }

    }

}

private fun <REC : Record<REC, *>> scatter(
        contentValues: Array<out ColValue<REC, *>>,
        colsToFill: Array<Col<REC, *>?>, valsToFill: Array<Any?>) {
    contentValues.forEachIndexed { i, pair ->
        colsToFill[i] = pair.col
        valsToFill[i] = pair.value
    }
}

private fun <REC : Record<REC, *>> insertQuery(table: Table<REC, *>, cols: Array<Col<REC, *>>): String {
    return StringBuilder("INSERT INTO ").appendName(table.name)
            .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
            .toString()
}

private fun StringBuilder.appendName(name: String) =
        append('"').append(name.replace("\"", "\"\"")).append('"')

private fun <REC : Record<REC, *>> StringBuilder.appendNames(cols: Array<Col<REC, *>>): StringBuilder {
    if (cols.isEmpty()) return this

    cols.forEach {  col ->
        appendName(col.name).append(", ")
    }
    setLength(length - 2) // trim comma

    return this
}

private fun StringBuilder.appendPlaceholders(count: Int): StringBuilder {
    if (count == 0) return this

    repeat(count) { append("?, ") }
    setLength(length - 2) // trim comma

    return this
}
