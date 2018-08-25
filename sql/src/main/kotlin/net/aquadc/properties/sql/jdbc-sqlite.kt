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
// TODO: prop concurrency mode instead of hardcoded propertyOf()
class JdbcSqliteSession(
        private val connection: Connection
) : Session {

    init {
        connection.autoCommit = false
    }

    private val lock = ReentrantReadWriteLock()
    private val daos = ConcurrentHashMap<Table<*, *>, Dao<*, *>>()

    private fun <REC : Record<REC, ID>, ID : IdBound> ConcurrentHashMap<Table<*, *>, Dao<*, *>>
            .forTable(table: Table<REC, ID>) =
            daos.getOrPut(table, { Dao(table) }) as Dao<REC, ID>

    override fun <REC : Record<REC, ID>, ID : IdBound> get(table: Table<REC, ID>): net.aquadc.properties.sql.Dao<REC, ID> =
            daos.forTable(table)

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: JdbcTransaction? = null
    private val insertStatements = HashMap<Pair<Table<*, *>, List<Col<*, *>>>, PreparedStatement>()
    private val updateStatements = HashMap<Pair<Table<*, *>, Col<*, *>>, PreparedStatement>()
    private val deleteStatements = HashMap<Table<*, *>, PreparedStatement>()

    private fun <REC : Record<REC, *>> insertStatementWLocked(table: Table<REC, *>, cols: Array<Col<REC, *>>): PreparedStatement =
            insertStatements.getOrPut(Pair(table, cols.asList())) {
                connection.prepareStatement(insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
            }

    private fun <REC : Record<REC, *>> updateStatementWLocked(table: Table<REC, *>, col: Col<REC, *>): PreparedStatement =
            updateStatements.getOrPut(Pair(table, col)) {
                connection.prepareStatement(updateQuery(table, col))
            }

    private fun <REC : Record<REC, *>> deleteStatementWLocked(table: Table<REC, *>): PreparedStatement =
            deleteStatements.getOrPut(table) {
                connection.prepareStatement(deleteQuery(table))
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
            if (successful) {
                connection.commit()
            } else {
                connection.rollback()
            }
            this.transaction = null

            if (successful) {
                deliverChanges(transaction)
            }
        } finally {
            lock.writeLock().unlock()
        }
    }

    // endregion transactions and modifying statements

    private fun deliverChanges(transaction: JdbcTransaction) {
        val del = transaction.deleted
        del?.forEach { (table, localIDs) ->
            @Suppress("UPPER_BOUND_VIOLATED")
            val man = daos.forTable<Any, IdBound>(table.erased)
            localIDs.forEach(man::dropManagement)
        }
        // Deletions first! Now we're not going to disturb souls of dead records & properties.

        // value changes
        transaction.updated?.forEach { (col, pkToVal) ->
            pkToVal.forEach { (localId, value) ->
                daos.get(col.table)?.erased?.commitValue(localId, col.erased, value)
                Unit
            }
        }

        // structure changes
        val ins = transaction.inserted
        if (ins != null || del != null) {
            val changedTables = (ins?.keys ?: emptySet<Table<*, *>>()) + (del?.keys ?: emptySet())

            @Suppress("UPPER_BOUND_VIOLATED")
            changedTables.forEach { table ->
                daos.forTable<Any, IdBound>(table.erased).onStructuralChange()
            }
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
        internal var updated: UpdatesHashMap? = null

        // table : local IDs
        internal var deleted: HashMap<Table<*, *>, ArrayList<Long>>? = null

        override fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, vararg contentValues: ColValue<REC, *>): ID {
            checkOpenAndThread()

            val size = contentValues.size
            val vals = arrayOfNulls<Any>(size)
            val cols = arrayOfNulls<Col<REC, *>>(size)
            scatter(contentValues, colsToFill = cols, valsToFill = vals)

            val statement = insertStatementWLocked(table, cols.hopeNoNulls)
            vals.forEachIndexed { idx, x -> statement.setObject(idx + 1, x) }
            check(statement.executeUpdate() == 1)
            val keys = statement.generatedKeys
            val id: ID = keys.fetchSingle()

            inserted ?: HashMap<Table<*, *>, ArrayList<Any>>().also { inserted = it }
                    .getOrPut(table, ::ArrayList)
                    .add(id)

            // writes all insertion fields as updates
            val updated = updated ?: UpdatesHashMap().also { updated = it }
            contentValues.forEach {
                updated.put(it, localId(it.col.table as Table<REC, ID>, id))
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

        override fun <REC : Record<REC, ID>, ID : IdBound> delete(record: REC) {
            checkOpenAndThread()
            check(session === record.session)

            val statement = deleteStatementWLocked(record.table)
            statement.setObject(1, record.primaryKey)
            check(statement.executeUpdate() == 1)

            (deleted ?: HashMap<Table<*, *>, ArrayList<Long>>().also { deleted = it })
                    .getOrPut(record.table, ::ArrayList)
                    .add(localId(record.table, record.primaryKey))
        }

        override fun setSuccessful() {
            checkOpenAndThread()
            isSuccessful = true
        }

        override fun close() {
            checkOpenAndThread()
            onTransactionEnd(isSuccessful)
            thread = null
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

    }


    override fun <REC : Record<REC, ID>, ID : IdBound, T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<Transaction, T> {
        val table = col.table as Table<REC, ID>
        val localId = localId(table, id)
        val manager = daos.forTable(table)
        return ManagedProperty<Transaction, T>(manager, col, localId)
    }

    private fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
        is Int -> id.toLong()
        is Long -> id
        else -> TODO("${id.javaClass} keys support")
    }

    private fun <ID : IdBound> dbId(table: Table<*, ID>, localId: Long): ID =
            localId as ID // todo

    override fun toString(): String = "JdbcSqliteSession($connection)"

    @Suppress("UPPER_BOUND_VIOLATED")
    private inline val Dao<*, *>.erased
        get() = this as Dao<Any, IdBound>

    private inner class Dao<REC : Record<REC, ID>, ID : IdBound>(
            private val table: Table<REC, ID>
    ) : net.aquadc.properties.sql.Dao<REC, ID>, Manager<Transaction> {

        private val records = ConcurrentHashMap<Long, REC>()

        private val singleSelectStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        @Suppress("UPPER_BOUND_VIOLATED") private val reusableCond = ThreadLocal<WhereCondition.ColCond<Any, Any?>>()

        private val countStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        private val counts = ConcurrentHashMap<WhereCondition<out REC>, MutableProperty<Long>>()

        private val selectionStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        private val selections = Vector<MutableProperty<Selection<REC, ID>>>() // coding like in 1995, yay!

        internal fun <T> commitValue(localId: Long, column: Col<REC, T>, value: T) {
            (records[localId]?.fields?.get(column.ordinal) as ManagedProperty<Transaction, T>?)?.commit(value)
        }

        internal fun dropManagement(localId: Long) {
            records.remove(localId)?.fields?.forEach(ManagedProperty<*, *>::dropManagement)
        }

        internal fun onStructuralChange() {
            selections.forEach {
                val sel = it.value
                it.value = Selection(this, sel.condition, fetchPrimaryKeys(table, sel.condition))
            }
            counts.forEach { (condition, prop) ->
                prop.value = cachedSelectStmt(countStatements, null, table, condition).fetchSingle<Number>().toLong()
            }
        }

        private fun <REC : Record<REC, ID>, ID : IdBound> fetchPrimaryKeys(table: Table<REC, ID>, condition: WhereCondition<out REC>): Array<ID> {
            val primaryKeys = cachedSelectStmt(selectionStatements, table.idCol, table, condition)
                    .fetchAll<ID>()
                    .toTypedArray<Any>() as Array<ID>
            return primaryKeys
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


        // region Dao implementation

        override fun find(id: ID): REC? {
            val localId = localId(table, id)
            return records.getOrPut<Long, REC>(localId) { table.create(this@JdbcSqliteSession, id) } // TODO: check whether exists
        }

        override fun select(condition: WhereCondition<out REC>): Property<List<REC>> {
            val primaryKeys = fetchPrimaryKeys(table, condition) // TODO: lazy

            return Selection(this, condition, primaryKeys)
                    .let(::propertyOf)
                    .also { selections.add(it) }
        }

        override fun count(condition: WhereCondition<out REC>): Property<Long> =
                counts.getOrPut(condition) {
                    cachedSelectStmt(countStatements, null, table, condition)
                            .fetchSingle<Number>().toLong()
                            .let(::propertyOf)
                }

        // endregion Dao implementation

        // region Manager implementation

        override fun <T> getDirty(column: Manager.Column<T>, id: Long): T {
            val transaction = transaction ?: return unset()

            val thisCol = transaction.updated?.getFor(column as Col<REC, T>) ?: return unset()
            // we've created this column, we can cast it to original type

            val localId = id
            return if (thisCol.containsKey(localId)) thisCol[localId] as T else unset()
            // 'as T' is safe since thisCol won't contain non-T value
        }

        @Suppress("UPPER_BOUND_VIOLATED")
        override fun <T> getClean(column: Manager.Column<T>, id: Long): T {
            val col = column as Col<Any, Any?>
            val condition = reusableCond.getOrSet {
                WhereCondition.ColCond<Any, Any?>(col, " = ?", Unset)
            }
            condition.col = col.table.idCol as Col<Any, Any?>
            condition.valueOrValues = dbId(col.table, id)

            return cachedSelectStmt<Any, Any>(singleSelectStatements, col, col.table as Table<Any, Any>, condition).fetchSingle()
        }

        override fun <T> set(transaction: Transaction, column: Manager.Column<T>, id: Long, update: T) {
            column as Col<REC, T>
            val ourTransact = this@JdbcSqliteSession.transaction
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

            transaction.update<REC, ID, T>(column.table as Table<REC, ID>, dbId(column.table, id), column, update)
        }

        // endregion Manager implementation

        override fun toString(): String = "Dao(for $table in ${this@JdbcSqliteSession})"

        @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
        private inline fun <T> unset(): T =
                Unset as T

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

private fun <REC : Record<REC, *>> deleteQuery(table: Table<REC, *>): String =
        StringBuilder("DELETE FROM ").appendName(table.name)
                .append(" WHERE ").appendName(table.idCol.name).append(" = ?;")
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
