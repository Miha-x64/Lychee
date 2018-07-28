@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.newManagedProperty
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

// TODO: support dialects
class JdbcSqliteSession(private val connection: Connection) : Session {

    init {
        connection.autoCommit = false
    }

    private val lock = ReentrantReadWriteLock()
    private val recordManager = RecordManager()

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
    private inner class JdbcTransaction : java.lang.Object(),  Transaction {

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
            val id: ID
            try {
                check(keys.next())
                id = keys.getObject(1) as ID
            } finally {
                keys.close()
            }
            return id
            // TODO notify
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

    override fun <REC : Record<REC, ID>, ID : IdBound, T> fieldOf(col: Col<REC, T>, id: ID): MutableProperty<T> {
        val tableCols =
                recordManager.records.getOrPut(col, ::ConcurrentHashMap) as ConcurrentHashMap<Long, MutableProperty<T>>

        if (id !is Long) TODO("non-long keys support")
        val pk = id

        return tableCols.getOrPut(pk) {
            newManagedProperty(recordManager as Manager<Col<REC, T>, T>, col, pk)
        }
    }

    private inner class RecordManager : Manager<Col<*, *>, Any?> {

        /**
         * col: records
         * record: fields
         */
        internal val records = ConcurrentHashMap<Col<*, *>, ConcurrentHashMap<Long, MutableProperty<*>>>()

        override fun getDirty(token: Col<*, *>, id: Long): Any? {
            TODO("getDirty")
        }

        override fun getClean(token: Col<*, *>, id: Long): Any? {
            TODO("getClean")
        }

        override fun set(token: Col<*, *>, id: Long, expected: Any?, update: Any?, onTransactionEnd: (newValue: Any?) -> Unit): Boolean {
            TODO("set")
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
