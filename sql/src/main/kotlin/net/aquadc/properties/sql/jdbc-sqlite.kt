@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
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
    private val updateStatements = HashMap<Pair<Table<*, *>, Col<*, *>>, PreparedStatement>()

    private fun <REC : Record<REC, *>> insertStatementWLocked(table: Table<REC, *>, cols: Array<Col<REC, *>>): PreparedStatement =
            insertStatements.getOrPut(Pair(table, cols.asList())) {
                connection.prepareStatement(insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
            }

    private fun <REC : Record<REC, *>> updateStatementWLocked(table: Table<REC, *>, col: Col<REC, *>): PreparedStatement =
            updateStatements.getOrPut(Pair(table, col)) {
                connection.prepareStatement(updateQuery(table, col))
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
        val transaction = transaction ?: throw AssertionError()
        try {
            if (successful) connection.commit() else connection.rollback()
            this.transaction = null

            transaction.updated?.forEach { (col, pkToVal) ->
                pkToVal.forEach { (localId, value) ->
                    @Suppress("UPPER_BOUND_VIOLATED")
                    (recordManager.entities[col.table]?.get(localId) as Record<Any?, IdBound>?)
                            ?.fields
                            ?.get(col.ordinal)
                            ?.commit(value)
                }
            }
            transaction.inserted?.forEach { (table, pk) ->
                // todo
            }
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

        // table : primary keys
        internal var inserted: HashMap<Table<*, *>, ArrayList<Any>>? = null

        // column : localId : value
        internal var updated: HashMap<Col<*, *>, HashMap<Long, Any?>>? = null

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

            inserted ?: HashMap<Table<*, *>, ArrayList<Any>>().also { inserted = it }
                    .getOrPut(table, ::ArrayList)
                    .add(id)

            // writes all insertion fields as updates
            val updated = updated ?: HashMap<Col<*, *>, HashMap<Long, Any?>>().also { updated = it }
            contentValues.forEach {
                updated.getOrPut(it.col, ::HashMap)[localId(it.col.table as Table<*, ID>, id)] = it.value
            }

            return id
        }

        override fun <REC : Record<REC, ID>, ID : IdBound, T> update(table: Table<REC, ID>, id: ID, column: Col<REC, T>, value: T) {
            checkOpenAndThread()

            val statement = updateStatementWLocked(table, column)
            statement.setObject(1, value)
            statement.setObject(2, id)
            check(statement.executeUpdate() == 1)

            (updated ?: HashMap<Col<*, *>, HashMap<Long, Any?>>().also { updated = it })
                    .getOrPut(column, ::HashMap)[localId(table, id)] = value
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

    override fun <REC : Record<REC, ID>, ID : IdBound, T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<T, Col<REC, T>> {
        val localId = localId(col.table as Table<REC, ID>, id)
        return ManagedProperty(recordManager as Manager<Col<REC, T>, T>, col, localId)
    }

    private fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
        is Int -> id.toLong()
        is Long -> id
        else -> TODO("${id.javaClass} keys support")
    }

    private fun <ID : IdBound> dbId(table: Table<*, ID>, localId: Long): ID =
            localId as ID


    private inner class RecordManager : Manager<Col<*, *>, Any?> {

        /*
         * table: records
         * recordId: record
         */
        internal val entities = ConcurrentHashMap<Table<*, *>, ConcurrentHashMap<Long, Record<*, *>>>()

        private val statements = ThreadLocal<HashMap<String, PreparedStatement>>()

        @Suppress("UPPER_BOUND_VIOLATED")
        private val reusableCond = ThreadLocal<WhereCondition.ColCond<Any, Any>>()

        override fun getDirty(token: Col<*, *>, id: Long): Any? {
            val transaction = transaction ?: return Unset

            val thisCol = transaction.updated?.get(token) ?: return Unset
            val primaryKey = dbId(token.table, id)
            return if (thisCol.containsKey(primaryKey)) thisCol[primaryKey] else Unset
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

        @Suppress("UPPER_BOUND_VIOLATED")
        override fun set(token: Col<*, *>, id: Long, expected: Any?, update: Any?): Boolean {
            val transaction = transaction ?: throw IllegalStateException("This can be performed only within a transaction")
            getDirty(token, id).let {
                if (it === Unset) {
                    if (getClean(token, id) === update) return true
                } else {
                    if (it === update) return true
                }
            }
            transaction.update<Any?, Any?, Any?>(token.table as Table<Any?, Any?>, dbId<Any?>(token.table, id), token as Col<Any?, Any?>, update) // TODO: check expected
            return true
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

private fun <REC : Record<REC, *>> updateQuery(table: Table<REC, *>, col: Col<REC, *>): String =
        StringBuilder("UPDATE ").appendName(table.name)
                .append(" SET ").appendName(col.name).append(" = ? WHERE ").appendName(table.idCol.name).append(" = ?;")
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
