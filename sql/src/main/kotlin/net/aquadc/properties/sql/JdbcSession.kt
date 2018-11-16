package net.aquadc.properties.sql

import net.aquadc.persistence.type.DataType
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.persistence.type.long
import java.sql.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.getOrSet

/**
 * Represents a database connection through JDBC.
 */
class JdbcSession(
        private val connection: Connection,
        private val dialect: Dialect
) : Session {

    init {
        connection.autoCommit = false
    }

    private val lock = ReentrantReadWriteLock()

    @Suppress("UNCHECKED_CAST")
    override fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> get(
            table: Table<TBL, ID, REC>
    ): Dao<TBL, ID, REC> =
            lowLevel.daos.getOrPut(table) { RealDao(this, lowLevel, table, dialect) } as Dao<TBL, ID, REC>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: RealTransaction? = null
    private val selectStatements = ThreadLocal<HashMap<String, PreparedStatement>>()
    private val insertStatements = HashMap<Pair<Table<*, *, *>, List<Col<*, *>>>, PreparedStatement>()
    private val updateStatements = HashMap<Pair<Table<*, *, *>, Col<*, *>>, PreparedStatement>()
    private val deleteStatements = HashMap<Table<*, *, *>, PreparedStatement>()

    private val lowLevel = object : LowLevelSession {

        override fun <TBL : Table<TBL, ID, *>, ID : IdBound> exists(table: Table<TBL, ID, *>, primaryKey: ID): Boolean {
            val count = select(null, table, reusableCond(table, table.idColName, primaryKey), NoOrder).fetchSingle(long)
            return when (count) {
                0L -> false
                1L -> true
                else -> throw AssertionError()
            }
        }

        private fun <TBL : Table<TBL, *, *>> insertStatementWLocked(table: Table<TBL, *, *>, cols: Array<Col<TBL, *>>): PreparedStatement =
                insertStatements.getOrPut(Pair(table, cols.asList())) {
                    connection.prepareStatement(dialect.insertQuery(table, cols), Statement.RETURN_GENERATED_KEYS)
                }

        override fun <TBL : Table<TBL, ID, *>, ID : IdBound> insert(table: Table<TBL, ID, *>, cols: Array<Col<TBL, *>>, vals: Array<Any?>): ID {
            val statement = insertStatementWLocked(table, cols)
            cols.forEachIndexed { idx, col -> col.type.erased.bind(statement, idx, vals[idx]) }
            check(statement.executeUpdate() == 1)
            val keys = statement.generatedKeys
            return keys.fetchSingle(table.idColType)
        }

        private fun <TBL : Table<TBL, *, *>> updateStatementWLocked(table: Table<TBL, *, *>, col: Col<TBL, *>): PreparedStatement =
                updateStatements.getOrPut(Pair(table, col)) {
                    connection.prepareStatement(dialect.updateFieldQuery(table, col))
                }

        override fun <TBL : Table<TBL, ID, *>, ID : IdBound, T> update(table: Table<TBL, ID, *>, id: ID, column: Col<TBL, T>, value: T) {
            val statement = updateStatementWLocked(table, column)
            column.type.bind(statement, 0, value)
            table.idColType.bind(statement, 1, id)
            check(statement.executeUpdate() == 1)
        }

        override fun <ID : IdBound> localId(table: Table<*, ID, *>, id: ID): Long = when (id) {
            is Int -> id.toLong()
            is Long -> id
            else -> TODO("${id.javaClass} keys support")
        }

        override fun <ID : IdBound> primaryKey(table: Table<*, ID, *>, localId: Long): ID =
                localId as ID // todo

        private fun deleteStatementWLocked(table: Table<*, *, *>): PreparedStatement =
                deleteStatements.getOrPut(table) {
                    connection.prepareStatement(dialect.deleteRecordQuery(table))
                }

        override fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID, *>, primaryKey: ID): Long {
            val statement = deleteStatementWLocked(table)
            table.idColType.bind(statement, 0, primaryKey)
            check(statement.executeUpdate() == 1)
            return localId(table, primaryKey)
        }

        override val daos = ConcurrentHashMap<Table<*, *, *>, RealDao<*, *, *>>()

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

        private fun <ID : IdBound, TBL : Table<TBL, ID, *>> select(
                columnName: String?,
                table: Table<TBL, ID, *>,
                condition: WhereCondition<out TBL>,
                order: Array<out Order<out TBL>>
        ): ResultSet {
            val query =
                    if (columnName == null) dialect.selectCountQuery(table, condition)
                    else dialect.selectFieldQuery(columnName, table, condition, order)

            return selectStatements
                    .getOrSet(::HashMap)
                    .getOrPut(query) { connection.prepareStatement(query) }
                    .also { stmt ->
                        val list = ArrayList<Pair<String, Any>>()
                        condition.appendValuesTo(list)
                        list.forEachIndexed { idx, (name, value) ->
                            val conv =
                                    if (name == table.idColName) table.idColType
                                    else table.fields.first { it.name == name }.type
                            conv.erased.bind(stmt, idx, value)
                        }
                    }
                    .executeQuery()
        }

        override fun <ID : IdBound, TBL : Table<TBL, ID, *>, T> fetchSingle(
                column: Col<TBL, T>, table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>
        ): T =
                select(column.name, table, condition, NoOrder).fetchSingle(column.type)

        override fun <ID : IdBound, TBL : Table<TBL, ID, *>> fetchPrimaryKeys(
                table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>, order: Array<out Order<TBL>>
        ): Array<ID> =
                select(table.idColName, table, condition, order)
                        .fetchAll(table.idColType)
                        .toTypedArray<Any>() as Array<ID>

        private fun <T> ResultSet.fetchAll(type: DataType<T>): List<T> {
            val values = ArrayList<T>()
            while (next())
                values.add(type.get(this, 0))
            close()
            return values
        }

        override fun <ID : IdBound, TBL : Table<TBL, ID, *>> fetchCount(table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>): Long =
                select(null, table, condition, NoOrder).fetchSingle(long)

        override val transaction: RealTransaction?
            get() = this@JdbcSession.transaction

        @Suppress("UPPER_BOUND_VIOLATED") private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

        @Suppress("UNCHECKED_CAST")
        override fun <TBL : Table<TBL, *, *>, T : Any> reusableCond(
                table: Table<TBL, *, *>, colName: String, value: T
        ): ColCond<TBL, T> {
            val condition = (localReusableCond as ThreadLocal<ColCond<TBL, T>>).getOrSet {
                ColCond(table.fields[0] as Col<TBL, T>, " = ?", value)
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
        lowLevel.daos.forEach { (table: Table<*, *, *>, dao: Dao<*, *, *>) ->
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

    private fun <T> ResultSet.fetchSingle(type: DataType<T>): T {
        try {
            check(next())
            return type.get(this, 0)
        } finally {
            close()
        }
    }

    private fun <T> DataType<T>.bind(statement: PreparedStatement, index: Int, value: T) {
        val i = 1 + index
        if (value == null) {
            check(isNullable)
            statement.setNull(i, Types.NULL)
        } else {
            when (this) {
                is DataType.Simple<T> -> {
                    val v = encode(value)
                    when (kind) {
                        DataType.Simple.Kind.Bool -> statement.setBoolean(i, v as Boolean)
                        DataType.Simple.Kind.I8 -> statement.setByte(i, v as Byte)
                        DataType.Simple.Kind.I16 -> statement.setShort(i, v as Short)
                        DataType.Simple.Kind.I32 -> statement.setInt(i, v as Int)
                        DataType.Simple.Kind.I64 -> statement.setLong(i, v as Long)
                        DataType.Simple.Kind.F32 -> statement.setFloat(i, v as Float)
                        DataType.Simple.Kind.F64 -> statement.setDouble(i, v as Double)
                        DataType.Simple.Kind.Str -> statement.setString(i, v as String)
                        // not sure whether setBlob should be used:
                        DataType.Simple.Kind.Blob -> statement.setObject(i, v as ByteArray)
                    }
                }
            }.also { }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> DataType<T>.get(resultSet: ResultSet, index: Int): T {
        val i = 1 + index

        val value = when (this) {
            is DataType.Simple -> when (kind) {
                DataType.Simple.Kind.Bool -> resultSet.getBoolean(i)
                DataType.Simple.Kind.I8 -> resultSet.getByte(i)
                DataType.Simple.Kind.I16 -> resultSet.getShort(i)
                DataType.Simple.Kind.I32 -> resultSet.getInt(i)
                DataType.Simple.Kind.I64 -> resultSet.getLong(i)
                DataType.Simple.Kind.F32 -> resultSet.getFloat(i)
                DataType.Simple.Kind.F64 -> resultSet.getDouble(i)
                DataType.Simple.Kind.Str -> resultSet.getString(i)
                DataType.Simple.Kind.Blob -> resultSet.getBytes(i)
            }
        }

        return if (resultSet.wasNull())
            check(isNullable).let { null as T }
        else
            decode(value)
    }

}
