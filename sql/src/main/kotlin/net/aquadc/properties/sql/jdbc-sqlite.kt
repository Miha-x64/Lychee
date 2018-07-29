@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.internal.newManagedProperty
import net.aquadc.properties.propertyOf
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.getOrSet

// TODO: support dialects
// TODO: evicting stale objects
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

            // TODO notify
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
            val id: ID = keys.fetchSingle()
            return id
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


    override fun <REC : Record<REC, ID>, ID : IdBound> find(table: Table<REC, ID>, id: ID): REC? {
        val localId = localId(table, id)
        val records = recordManager.entities.getOrPut(table, ::ConcurrentHashMap) as ConcurrentHashMap<Long, REC>
        return records.getOrPut(localId) { table.create(this, id) } // TODO: check whether exists
    }


    private val selectStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
    private val selections = Vector<MutableProperty<out Selection<*, *>>>() // coding like in 1995, yay!

    override fun <REC : Record<REC, ID>, ID : IdBound> select(
            table: Table<REC, ID>, condition: WhereCondition<out REC>
    ): Property<List<REC>> { // TODO DiffProperty
        val primaryKeys = cachedSelectStmt(selectStatements, table.idCol, table, condition)
                .fetchAll<ID>()
                .toTypedArray<Any>() as Array<ID>

        return Selection(this, table, condition, primaryKeys)
                .let(::propertyOf)
                .also { selections.add(it) }
    }


    private val countStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
    private val counts = Vector<MutableProperty<Long>>() // coding like in 1995, yay!

    override fun <REC : Record<REC, ID>, ID : IdBound> count(table: Table<REC, ID>, condition: WhereCondition<out REC>): Property<Long> {
        return cachedSelectStmt(countStatements, null, table, condition)
                .fetchSingle<Number>()
                .let { propertyOf(it.toLong()) }
                .also { counts.add(it) }
    }


    private fun <ID : IdBound, REC : Record<REC, ID>> cachedSelectStmt(
            statements: ThreadLocal<HashMap<String, PreparedStatement>>,
            column: Col<REC, *>?,
            table: Table<REC, ID>, condition: WhereCondition<out REC>
    ): ResultSet {
        val query = selectQuery(column, table, condition)

        return statements
                .getOrSet(::HashMap)
                .getOrPut(query) { connection.prepareStatement(query) }
                .also { it.bind(ArrayList<Any>().also(condition::appendValuesTo)) }
                .executeQuery()
    }


    override fun <REC : Record<REC, ID>, ID : IdBound, T> fieldOf(
            table: Table<REC, ID>, col: Col<REC, T>, id: ID
    ): MutableProperty<T> {
        val tableCols =
                recordManager.records.getOrPut(col, ::ConcurrentHashMap) as ConcurrentHashMap<Long, MutableProperty<T>>

        val localId = localId(table, id)

        return tableCols.getOrPut(localId) {
            newManagedProperty(recordManager as Manager<Col<REC, T>, T>, col, localId)
        }
    }

    private fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
        is Int -> id.toLong()
        is Long -> id
        else -> TODO("${id.javaClass} keys support")
    }

    private inner class RecordManager : Manager<Col<*, *>, Any?> {

        /*
         * table: records
         * recordId: record
         */
        internal val entities = ConcurrentHashMap<Table<*, *>, ConcurrentHashMap<Long, Any>>()

        /*
         * col: records
         * record: fields
         */
        internal val records = ConcurrentHashMap<Col<*, *>, ConcurrentHashMap<Long, MutableProperty<*>>>()

        private val statements = ThreadLocal<HashMap<String, PreparedStatement>>()

        @Suppress("UPPER_BOUND_VIOLATED")
        private val reusableCond = ThreadLocal<WhereCondition.ColCond<Any, Any>>()

        override fun getDirty(token: Col<*, *>, id: Long): Any? {
            return Unset // todo
        }

        @Suppress("UPPER_BOUND_VIOLATED")
        override fun getClean(token: Col<*, *>, id: Long): Any? {
            val condition = reusableCond.getOrSet {
                WhereCondition.ColCond<Any, Any>(token as Col<Any, Any>, " = ?", Unset)
            }
            condition.col = token.table.idCol as Col<Any, Any>
            condition.valueOrValues = id

            return cachedSelectStmt<Any, Any>(statements, token as Col<Any, *>, token.table as Table<Any, Any>, condition)
                    .fetchSingle()
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

private fun <REC : Record<REC, *>> insertQuery(table: Table<REC, *>, cols: Array<Col<REC, *>>): String =
        StringBuilder("INSERT INTO ").appendName(table.name)
                .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
                .toString()

private fun <REC : Record<REC, *>> selectQuery(column: Col<REC, *>?, table: Table<REC, *>, condition: WhereCondition<out REC>): String {
    val sb = StringBuilder("SELECT ")
            .let { if (column == null) it.append("COUNT(*)") else it.appendName(column.name) }
            .append(" FROM ").appendName(table.name)
            .append(" WHERE ")

    val afterWhere = sb.length
    condition.appendTo(sb)

    if (sb.length == afterWhere) sb.append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1

    return sb.toString()
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

private fun PreparedStatement.bind(params: ArrayList<Any>) {
    for (i in params.indices)
        setObject(i + 1, params[i])
}

private fun <T> ResultSet.fetchAll(): List<T> {
    val values = ArrayList<Any>()
    while (next())
        values.add(getObject(1))
    close()
    return values as List<T>
}

fun <T> ResultSet.fetchSingle(): T {
    try {
        check(next())
        return getObject(1) as T
    } finally {
        close()
    }
}
