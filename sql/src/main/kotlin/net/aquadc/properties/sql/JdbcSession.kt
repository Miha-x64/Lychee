@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.struct.converter.JdbcConverter
import net.aquadc.struct.converter.long
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.getOrSet

// TODO: prop concurrency mode instead of hardcoded propertyOf() and ConcurrentHashMap()
class JdbcSession(
        private val connection: Connection,
        private val dialect: Dialect
) : Session {

    init {
        connection.autoCommit = false
    }

    private val lock = ReentrantReadWriteLock()

    private fun <REC : Record<REC, ID>, ID : IdBound> forTable(table: Table<REC, ID>) =
            lowLevel.daos.getOrPut(table) { RealDao(this, lowLevel, table, dialect) } as Dao<REC, ID>

    override fun <REC : Record<REC, ID>, ID : IdBound> get(table: Table<REC, ID>): Dao<REC, ID> =
            forTable(table)

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: RealTransaction? = null
    private val selectStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
    private val insertStatements = HashMap<Pair<Table<*, *>, List<Col<*, *>>>, PreparedStatement>()
    private val updateStatements = HashMap<Pair<Table<*, *>, Col<*, *>>, PreparedStatement>()
    private val deleteStatements = HashMap<Table<*, *>, PreparedStatement>()

    private val lowLevel = object : LowLevelSession {

        override fun <REC : Record<REC, ID>, ID : IdBound> exists(table: Table<REC, ID>, primaryKey: ID): Boolean {
            val count = select(null, table, reusableCond(table, table.idColName, primaryKey)).fetchSingle(long)
            return when (count) {
                0L -> false
                1L -> true
                else -> throw AssertionError()
            }
        }

        private fun <REC : Record<REC, *>> insertStatementWLocked(table: Table<REC, *>, cols: Array<Col<REC, *>>): PreparedStatement =
                insertStatements.getOrPut(Pair(table, cols.asList())) {
                    connection.prepareStatement(dialect.insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
                }

        override fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, cols: Array<Col<REC, *>>, vals: Array<Any?>): ID {
            val statement = insertStatementWLocked(table, cols)
            cols.forEachIndexed { idx, col -> (col.converter as JdbcConverter<*>).erased.bind(statement, idx, vals[idx]) }
            check(statement.executeUpdate() == 1)
            val keys = statement.generatedKeys
            return keys.fetchSingle(table.idColConverter)
        }

        private fun <REC : Record<REC, *>> updateStatementWLocked(table: Table<REC, *>, col: Col<REC, *>): PreparedStatement =
                updateStatements.getOrPut(Pair(table, col)) {
                    connection.prepareStatement(dialect.updateFieldQuery(table, col))
                }

        override fun <REC : Record<REC, ID>, ID : IdBound, T> update(table: Table<REC, ID>, id: ID, column: Col<REC, T>, value: T) {
            val statement = updateStatementWLocked(table, column)
            (column.converter as JdbcConverter<T>).bind(statement, 0, value)
            table.idColConverter.bind(statement, 1, id)
            check(statement.executeUpdate() == 1)
        }

        override fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
            is Int -> id.toLong()
            is Long -> id
            else -> TODO("${id.javaClass} keys support")
        }

        override fun <ID : IdBound> primaryKey(table: Table<*, ID>, localId: Long): ID =
                localId as ID // todo

        private fun deleteStatementWLocked(table: Table<*, *>): PreparedStatement =
                deleteStatements.getOrPut(table) {
                    connection.prepareStatement(dialect.deleteRecordQuery(table))
                }

        override fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID>, primaryKey: ID): Long {
            val statement = deleteStatementWLocked(table)
            table.idColConverter.bind(statement, 0, primaryKey)
            check(statement.executeUpdate() == 1)
            return localId(table, primaryKey)
        }

        override val daos = ConcurrentHashMap<Table<*, *>, RealDao<*, *>>()

        override fun onTransactionEnd(successful: Boolean) {
            val transaction = transaction ?: throw AssertionError()
            try {
                if (successful) {
                    connection.commit()
                } else {
                    connection.rollback()
                }
                this@JdbcSession.transaction = null

                if (successful) {
                    transaction.deliverChanges()
                }
            } finally {
                lock.writeLock().unlock()
            }
        }

        private fun <ID : IdBound, REC : Record<REC, ID>> select(
                columnName: String?,
                table: Table<REC, ID>,
                condition: WhereCondition<out REC>
        ): ResultSet {
            val query =
                    if (columnName == null) dialect.selectCountQuery(table, condition)
                    else dialect.selectFieldQuery(columnName, table, condition)

            return selectStatements
                    .getOrSet(::HashMap)
                    .getOrPut(query) { connection.prepareStatement(query) }
                    .also { stmt ->
                        val list = ArrayList<Pair<String, Any>>()
                        condition.appendValuesTo(list)
                        list.forEachIndexed { idx, (name, value) ->
                            val conv =
                                    if (name == table.idColName) table.idColConverter
                                    else table.fields.first { it.name == name }.converter
                            (conv as JdbcConverter<Any>).bind(stmt, idx, value)
                        }
                    }
                    .executeQuery()
        }

        override fun <ID : IdBound, REC : Record<REC, ID>, T> fetchSingle(column: Col<REC, T>, table: Table<REC, ID>, condition: WhereCondition<out REC>): T =
                select(column.name, table, condition).fetchSingle(column.converter as JdbcConverter<T>)

        override fun <ID : IdBound, REC : Record<REC, ID>> fetchPrimaryKeys(table: Table<REC, ID>, condition: WhereCondition<out REC>): Array<ID> =
                select(table.idColName, table, condition)
                        .fetchAll(table.idColConverter)
                        .toTypedArray<Any>() as Array<ID>

        private fun <T> ResultSet.fetchAll(converter: JdbcConverter<T>): List<T> {
            val values = ArrayList<Any?>()
            while (next())
                values.add(converter.get(this, 0))
            close()
            return values as List<T>
        }

        override fun <ID : IdBound, REC : Record<REC, ID>> fetchCount(table: Table<REC, ID>, condition: WhereCondition<out REC>): Long =
                select(null, table, condition).fetchSingle(long)

        override val transaction: RealTransaction?
            get() = this@JdbcSession.transaction

        @Suppress("UPPER_BOUND_VIOLATED") private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

        override fun <REC : Record<REC, *>, T : Any> reusableCond(table: Table<REC, *>, colName: String, value: T): ColCond<REC, T> {
            val condition = (localReusableCond as ThreadLocal<ColCond<REC, T>>).getOrSet {
                ColCond(table.fields[0] as Col<REC, T>, " = ?", value)
            }
            condition.colName = colName
            condition.valueOrValues = value
            return condition
        }

    }


    override fun beginTransaction(): Transaction {
        val wLock = lock.writeLock()
        check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in transaction" }
        wLock.lock()
        val tr = RealTransaction(this, lowLevel)
        transaction = tr
        return tr
    }

    // endregion transactions and modifying statements

    override fun toString(): String =
            "JdbcSession(connection=$connection, dialect=${dialect.javaClass.simpleName})"


    fun dump(sb: StringBuilder) {
        sb.append("DAOs\n")
        lowLevel.daos.forEach { (table: Table<*, *>, dao: Dao<*, *>) ->
            sb.append(" ").append(table.name).append("\n")
            dao.dump("  ", sb)
        }

        arrayOf(
                "select statements" to selectStatements
        ).forEach { (name, stmts) ->
            sb.append(name).append(" (for current thread)\n")
            stmts.get()?.keys?.forEach { sql ->
                sb.append(' ').append(sql).append("\n")
            }
        }

        arrayOf(
                "insert statements" to insertStatements,
                "update statements" to updateStatements,
                "delete statements" to deleteStatements
        ).forEach { (text, stmts) ->
            sb.append(text).append('\n')
            stmts.keys.forEach {
                sb.append(' ').append(it).append('\n')
            }
        }
    }

    private fun <T> ResultSet.fetchSingle(converter: JdbcConverter<T>): T {
        try {
            check(next())
            return converter.get(this, 0)
        } finally {
            close()
        }
    }

}
