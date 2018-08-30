@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.MutableProperty
import net.aquadc.properties.Property
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.map
import net.aquadc.properties.propertyOf
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.sqlite.long
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

// TODO: evicting stale objects
// TODO: prop concurrency mode instead of hardcoded propertyOf()
class JdbcSession(
        private val connection: Connection,
        private val dialect: Dialect
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
                connection.prepareStatement(dialect.insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
            }

    private fun <REC : Record<REC, *>> updateStatementWLocked(table: Table<REC, *>, col: Col<REC, *>): PreparedStatement =
            updateStatements.getOrPut(Pair(table, col)) {
                connection.prepareStatement(dialect.updateFieldQuery(table, col))
            }

    private fun <REC : Record<REC, *>> deleteStatementWLocked(table: Table<REC, *>): PreparedStatement =
            deleteStatements.getOrPut(table) {
                connection.prepareStatement(dialect.deleteRecordQuery(table))
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
            get() = this@JdbcSession

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
            var cols = arrayOfNulls<Col<REC, *>>(size)
            scatter(contentValues, colsToFill = cols, valsToFill = vals)
            cols as Array<Col<REC, *>>

            val statement = insertStatementWLocked(table, cols)
            cols.forEachIndexed { idx, col -> col.converter.erased.bind(statement, idx, vals[idx]) }
            check(statement.executeUpdate() == 1)
            val keys = statement.generatedKeys
            val id: ID = keys.fetchSingle(table.idCol.converter)

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
            column.converter.bind(statement, 0, value)
            table.idCol.converter.bind(statement, 1, id)
            check(statement.executeUpdate() == 1)

            (updated ?: HashMap<Col<*, *>, HashMap<Long, Any?>>().also { updated = it })
                    .getOrPut(column, ::HashMap)[localId(table, id)] = value
        }

        override fun <REC : Record<REC, ID>, ID : IdBound> delete(record: REC) {
            checkOpenAndThread()
            check(session === record.session)

            val table = record.table
            val statement = deleteStatementWLocked(table)
            table.idCol.converter.bind(statement, 0, record.primaryKey)
            check(statement.executeUpdate() == 1)

            (deleted ?: HashMap<Table<*, *>, ArrayList<Long>>().also { deleted = it })
                    .getOrPut(table, ::ArrayList)
                    .add(localId(table, record.primaryKey))
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


    private fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
        is Int -> id.toLong()
        is Long -> id
        else -> TODO("${id.javaClass} keys support")
    }

    private fun <ID : IdBound> dbId(table: Table<*, ID>, localId: Long): ID =
            localId as ID // todo

    override fun toString(): String =
            "JdbcSession(connection=$connection, dialect=${dialect.javaClass.simpleName})"

    @Suppress("UPPER_BOUND_VIOLATED")
    private inline val Dao<*, *>.erased
        get() = this as Dao<Any, IdBound>

    private inner class Dao<REC : Record<REC, ID>, ID : IdBound>(
            private val table: Table<REC, ID>
    ) : net.aquadc.properties.sql.Dao<REC, ID>, Manager<Transaction> {

        private val records = ConcurrentHashMap<Long, REC>()

        // SELECT <field> WHERE _id = ?
        private val singleSelectStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        @Suppress("UPPER_BOUND_VIOLATED") private val reusableCond = ThreadLocal<WhereCondition.ColCond<Any, Any?>>()

        // SELECT COUNT(*) WHERE ...
        private val countStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        private val counts = ConcurrentHashMap<WhereCondition<out REC>, MutableProperty<WhereCondition<out REC>>>()

        // SELECT _id WHERE ...
        private val selectionStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
        private val selections = Vector<MutableProperty<WhereCondition<out REC>>>() // coding like in 1995, yay! TODO: deduplication

        internal fun <T> commitValue(localId: Long, column: Col<REC, T>, value: T) {
            (records[localId]?.fields?.get(column.ordinal) as ManagedProperty<Transaction, T>?)?.commit(value)
        }

        internal fun dropManagement(localId: Long) {
            records.remove(localId)?.fields?.forEach(ManagedProperty<*, *>::dropManagement)
        }

        internal fun onStructuralChange() {
            // TODO: trigger this only if something has changed

            selections.forEach {
                it.value = it.value
            }
            counts.forEach { (_, it) ->
                it.value = it.value
            }
        }

        private fun <ID : IdBound, REC : Record<REC, ID>> cachedSelectStmt(
                statements: ThreadLocal<HashMap<String, PreparedStatement>>,
                column: Col<REC, *>?,
                table: Table<REC, ID>,
                condition: WhereCondition<out REC>
        ): ResultSet {
            val query =
                    if (column == null) dialect.selectCountQuery(table, condition)
                    else dialect.selectFieldQuery(column, table, condition)

            return statements
                    .getOrSet(::HashMap)
                    .getOrPut(query) { connection.prepareStatement(query) }
                    .also { condition.bindValuesTo(it, 0) }
                    .executeQuery()
        }


        // region Dao implementation

        override fun find(id: ID): REC? {
            val localId = localId(table, id)
            return records.getOrPut<Long, REC>(localId) { table.create(this@JdbcSession, id) } // TODO: check whether exists
        }

        override fun select(condition: WhereCondition<out REC>): Property<List<REC>> =
                propertyOf(condition)
                        .also { selections.add(it) }
                        .map(Query(this))

        override fun count(condition: WhereCondition<out REC>): Property<Long> =
                counts.getOrPut(condition) {
                    propertyOf(condition)
                }.map(Count(this)) // todo cache, too

        // endregion Dao implementation

        // region low-level Dao implementation

        override fun fetchPrimaryKeys(condition: WhereCondition<out REC>): Array<ID> =
                table.idCol.let { idCol ->
                    cachedSelectStmt(selectionStatements, idCol, table, condition)
                            .fetchAll(idCol.converter)
                            .toTypedArray<Any>() as Array<ID>
                }

        override fun fetchCount(condition: WhereCondition<out REC>): Long =
                cachedSelectStmt(countStatements, null, table, condition).fetchSingle(long)

        override fun <T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<Transaction, T> {
            val localId = localId(table, id)
            val manager = daos.forTable(table)
            return ManagedProperty<Transaction, T>(manager, col, localId)
        }

        // endregion low-level Dao implementation

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
            val col = column as Col<REC, T>
            val condition = (reusableCond as ThreadLocal<WhereCondition.ColCond<REC, ID>>).getOrSet {
                WhereCondition.ColCond(table.idCol, " = ?", Unset)
            }
            condition.col = table.idCol
            condition.valueOrValues = dbId(col.table, id)

            return cachedSelectStmt<ID, REC>(singleSelectStatements, col, table, condition).fetchSingle(col.converter)
        }

        override fun <T> set(transaction: Transaction, column: Manager.Column<T>, id: Long, update: T) {
            column as Col<REC, T>
            val ourTransact = this@JdbcSession.transaction
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

        override fun toString(): String = "Dao(for $table in ${this@JdbcSession})"

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

private fun <T> ResultSet.fetchAll(converter: Converter<T>): List<T> {
    val values = ArrayList<Any?>()
    while (next())
        values.add(converter.get(this, 0))
    close()
    return values as List<T>
}

fun <T> ResultSet.fetchSingle(converter: Converter<T>): T {
    try {
        check(next())
        return converter.get(this, 0)
    } finally {
        close()
    }
}
