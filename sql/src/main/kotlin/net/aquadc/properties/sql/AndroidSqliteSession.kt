package net.aquadc.properties.sql

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import net.aquadc.struct.converter.AndroidSqliteConverter
import net.aquadc.struct.converter.long
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet

/**
 * Represents a connection with an [SQLiteDatabase].
 */
// TODO: use simpleQueryForLong and simpleQueryForString with compiled statements where possible
class AndroidSqliteSession(
        private val connection: SQLiteDatabase
) : Session {

    private val lock = ReentrantReadWriteLock()

    override fun <REC : Record<REC, ID>, ID : IdBound> get(table: Table<REC, ID>): Dao<REC, ID> =
            lowLevel.daos.getOrPut(table) {
                check(table.idColConverter === long)
                RealDao(this, lowLevel, table, SqliteDialect)
            } as Dao<REC, ID>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: RealTransaction? = null
//    private val selectStatements = ThreadLocal<HashMap<String, SQLiteStatement>>()
    private val insertStatements = HashMap<Pair<Table<*, *>, List<Col<*, *>>>, SQLiteStatement>()
    private val updateStatements = HashMap<Pair<Table<*, *>, Col<*, *>>, SQLiteStatement>()
    private val deleteStatements = HashMap<Table<*, *>, SQLiteStatement>()

    private val lowLevel = object : LowLevelSession {

        override fun <REC : Record<REC, ID>, ID : IdBound> exists(table: Table<REC, ID>, primaryKey: ID): Boolean {

            val count = select(null, table, reusableCond(table, table.idColName, primaryKey), NoOrder).fetchSingle(long)
            return when (count) {
                0L -> false
                1L -> true
                else -> throw AssertionError()
            }
        }

        private fun <REC : Record<REC, *>> insertStatementWLocked(table: Table<REC, *>, cols: Array<Col<REC, *>>): SQLiteStatement =
                insertStatements.getOrPut(Pair(table, cols.asList())) {
                    connection.compileStatement(SqliteDialect.insertQuery(table, cols))
                }

        override fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, cols: Array<Col<REC, *>>, vals: Array<Any?>): ID {
            val statement = insertStatementWLocked(table, cols)
            cols.forEachIndexed { idx, col -> (col.converter as AndroidSqliteConverter<*>).erased.bind(statement, idx, vals[idx]) }
            val id = statement.executeInsert()
            check(id != -1L)
            return id as ID
        }

        private fun <REC : Record<REC, *>> updateStatementWLocked(table: Table<REC, *>, col: Col<REC, *>): SQLiteStatement =
                updateStatements.getOrPut(Pair(table, col)) {
                    connection.compileStatement(SqliteDialect.updateFieldQuery(table, col))
                }

        override fun <REC : Record<REC, ID>, ID : IdBound, T> update(table: Table<REC, ID>, id: ID, column: Col<REC, T>, value: T) {
            val statement = updateStatementWLocked(table, column)
            (column.converter as AndroidSqliteConverter<T>).bind(statement, 0, value)
            (table.idColConverter as AndroidSqliteConverter<ID>).bind(statement, 1, id)
            check(statement.executeUpdateDelete() == 1)
        }

        override fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long = when (id) {
            is Int -> id.toLong()
            is Long -> id
            else -> throw AssertionError()
        }

        override fun <ID : IdBound> primaryKey(table: Table<*, ID>, localId: Long): ID =
                localId as ID

        private fun deleteStatementWLocked(table: Table<*, *>): SQLiteStatement =
                deleteStatements.getOrPut(table) {
                    connection.compileStatement(SqliteDialect.deleteRecordQuery(table))
                }

        override fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID>, primaryKey: ID): Long {
            val statement = deleteStatementWLocked(table)
            (table.idColConverter as AndroidSqliteConverter<ID>).bind(statement, 0, primaryKey)
            check(statement.executeUpdateDelete() == 1)
            return localId(table, primaryKey)
        }

        override val daos = ConcurrentHashMap<Table<*, *>, RealDao<*, *>>()

        override fun onTransactionEnd(successful: Boolean) {
            val transaction = transaction ?: throw AssertionError()
            try {
                if (successful) {
                    connection.setTransactionSuccessful()
                }
                connection.endTransaction()
                this@AndroidSqliteSession.transaction = null

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
                condition: WhereCondition<out REC>,
                order: Array<out Order<out REC>>
        ): Cursor {
            val args = ArrayList<Pair<String, Any>>()
            condition.appendValuesTo(args)

            val selectionArgs = args.mapToArray { (name, value) ->
                val conv = // looks like a slow place
                        if (name == table.idColName) table.idColConverter
                        else table.fields.first { it.name == name }.converter
                (conv as AndroidSqliteConverter<Any>).asString(value)
            }

            return with(SqliteDialect) {
                connection.query(
                        table.name, // ...may reuse a single array
                        if (columnName == null) arrayOf("COUNT(*)") else arrayOf(columnName),
                        StringBuilder().appendWhereClause(condition).toString(), selectionArgs,
                        null, null,
                        if (order.isEmpty()) null else StringBuilder().appendOrderClause(order).toString()
                )
            }
        }

        override fun <ID : IdBound, REC : Record<REC, ID>, T> fetchSingle(column: Col<REC, T>, table: Table<REC, ID>, condition: WhereCondition<out REC>): T =
                select(column.name, table, condition, NoOrder).fetchSingle(column.converter as AndroidSqliteConverter<T>)

        override fun <ID : IdBound, REC : Record<REC, ID>> fetchPrimaryKeys(
                table: Table<REC, ID>, condition: WhereCondition<out REC>, order: Array<out Order<REC>>
        ): Array<ID> =
                select(table.idColName, table, condition, order)
                        .fetchAll(table.idColConverter as AndroidSqliteConverter<ID>) // converter here is obviously 'long', may seriously optimize this place
                        .toTypedArray<Any>() as Array<ID>

        private fun <T> Cursor.fetchAll(converter: AndroidSqliteConverter<T>): List<T> {
            if (!moveToFirst()) {
                close()
                return emptyList()
            }

            val values = ArrayList<Any?>()
            do {
                values.add(converter.get(this, 0))
            } while (moveToNext())
            close()
            return values as List<T>
        }

        override fun <ID : IdBound, REC : Record<REC, ID>> fetchCount(table: Table<REC, ID>, condition: WhereCondition<out REC>): Long =
                select(null, table, condition, NoOrder).fetchSingle(long)

        override val transaction: RealTransaction?
            get() = this@AndroidSqliteSession.transaction

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
        connection.beginTransaction()
        val tr = RealTransaction(this, lowLevel)
        transaction = tr
        return tr
    }

    // endregion transactions and modifying statements

    override fun toString(): String =
            "AndroidSqliteSession(connection=$connection)"


    fun dump(sb: StringBuilder) {
        sb.append("DAOs\n")
        lowLevel.daos.forEach { (table: Table<*, *>, dao: Dao<*, *>) ->
            sb.append(" ").append(table.name).append("\n")
            dao.dump("  ", sb)
        }

        /*arrayOf(
                "select statements" to selectStatements
        ).forEach { (name, stmts) ->
            sb.append(name).append(" (for current thread)\n")
            stmts.get()?.keys?.forEach { sql ->
                sb.append(' ').append(sql).append("\n")
            }
        }*/

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

    private fun <T> Cursor.fetchSingle(converter: AndroidSqliteConverter<T>): T {
        try {
            check(moveToFirst())
            return converter.get(this, 0)
        } finally {
            close()
        }
    }

}
