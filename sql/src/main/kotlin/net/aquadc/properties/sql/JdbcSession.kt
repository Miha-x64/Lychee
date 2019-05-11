package net.aquadc.properties.sql

import net.aquadc.persistence.array
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.properties.sql.dialect.Dialect
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
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
    override fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(
            table: Table<SCH, ID, REC>
    ): Dao<SCH, ID, REC> =
            getDao(table) as Dao<SCH, ID, REC>

    private fun <SCH : Schema<SCH>, ID : IdBound> getDao(table: Table<SCH, ID, *>): RealDao<SCH, ID, *, PreparedStatement> =
            lowLevel.daos.getOrPut(table) { RealDao(this, lowLevel, table as Table<SCH, ID, Record<SCH, ID>>, dialect) } as RealDao<SCH, ID, *, PreparedStatement>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: RealTransaction? = null

    private val lowLevel: LowLevelSession<PreparedStatement> = object : LowLevelSession<PreparedStatement> {

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID {
            val dao = getDao(table)
            val statement = dao.insertStatement ?: connection.prepareStatement(dialect.insert(table), Statement.RETURN_GENERATED_KEYS).also { dao.insertStatement = it }

            val offset = if (table.pkField === null) 1 else 0
            val cols = table.columns
            for (i in 0 until cols.size - offset) {
                val col = cols[i + offset].erased
                col.type.bind(statement, i, col(data))
            }
            try {
                check(statement.executeUpdate() == 1)
            } catch (e: SQLException) {
                statement.close() // poisoned statement
                dao.insertStatement = null

                throw e
            }
            return statement.generatedKeys.fetchSingle(table.idColType)
        }

        private fun <SCH : Schema<SCH>> updateStatementWLocked(table: Table<SCH, *, *>, cols: Any): PreparedStatement =
                getDao(table)
                        .updateStatements
                        .getOrPut(cols) {
                            val colArray =
                                    if (cols is Array<*>) cols as Array<NamedLens<SCH, Struct<SCH>, *>>
                                    else arrayOf(cols as NamedLens<SCH, Struct<SCH>, *>)
                            connection.prepareStatement(dialect.updateQuery(table, colArray))
                        }

        override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID, *>, id: ID, columns: Any, values: Any?) {
            val statement = updateStatementWLocked(table, columns)
            val colCount = if (columns is Array<*>) {
                columns as Array<NamedLens<SCH, Struct<SCH>, *>>
                values as Array<*>?
                columns.forEachIndexed { i, col ->
                    col.type.erased.bind(statement, i, values?.get(i))
                }
                columns.size
            } else {
                (columns as NamedLens<SCH, Struct<SCH>, *>).type.erased.bind(statement, 0, values)
                1
            }
            table.idColType.bind(statement, colCount, id)
            check(statement.executeUpdate() == 1)
        }

        override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID, *>, primaryKey: ID) {
            val dao = getDao(table)
            val statement = dao.deleteStatement ?: connection.prepareStatement(dialect.deleteRecordQuery(table)).also { dao.deleteStatement = it }
            table.idColType.bind(statement, 0, primaryKey)
            check(statement.executeUpdate() == 1)
        }

        override fun truncate(table: Table<*, *, *>) {
            val stmt = connection.createStatement()
            try {
                stmt.execute(dialect.truncate(table))
            } finally {
                stmt.close()
            }
        }

        override val daos = ConcurrentHashMap<Table<*, *, *>, RealDao<*, *, *, PreparedStatement>>()

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

        private fun <ID : IdBound, SCH : Schema<SCH>> select(
                columnName: String?,
                table: Table<SCH, ID, *>,
                condition: WhereCondition<out SCH>,
                order: Array<out Order<out SCH>>
        ): ResultSet {
            val query =
                    if (columnName == null) dialect.selectCountQuery(table, condition)
                    else dialect.selectFieldQuery(columnName, table, condition, order)

            return getDao(table)
                    .selectStatements
                    .getOrSet(::HashMap)
                    .getOrPut(query) { connection.prepareStatement(query) }
                    .also { stmt ->
                        val argNames = ArrayList<String>()
                        val argValues = ArrayList<Any>()
                        condition.appendValuesTo(argNames, argValues)
                        forEachOfBoth(argNames, argValues) { idx, name, value ->
                            val conv =
                                    if (name == table.idColName) table.idColType
                                    else table.schema.fieldsByName[name]!!.type
                            conv.erased.bind(stmt, idx, value)
                        }
                    }
                    .executeQuery()
        }

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
                table: Table<SCH, ID, *>, column: NamedLens<SCH, *, T>, id: ID
        ): T =
                select(column.name, table, reusableCond(table, table.idColName, id), NoOrder).fetchSingle(column.type)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
                table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>, order: Array<out Order<SCH>>
        ): Array<ID> =
                select(table.idColName, table, condition, order)
                        .fetchAll(table.idColType)
                        .array<Any>() as Array<ID>

        private fun <T> ResultSet.fetchAll(type: DataType<T>): List<T> {
            val values = ArrayList<T>()
            while (next())
                values.add(type.get(this, 0))
            close()
            return values
        }

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(
                table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>
        ): Long =
                select(null, table, condition, NoOrder).fetchSingle(long)

        override val transaction: RealTransaction?
            get() = this@JdbcSession.transaction

        @Suppress("UPPER_BOUND_VIOLATED")
        private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

        @Suppress("UNCHECKED_CAST")
        override fun <SCH : Schema<SCH>, T : Any> reusableCond(
                table: Table<SCH, *, *>, colName: String, value: T
        ): ColCond<SCH, T> {
            val condition = (localReusableCond as ThreadLocal<ColCond<SCH, T>>).getOrSet {
                ColCond(table.schema.fields[0] as FieldDef<SCH, T>, " = ?", value)
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

            sb.append("  select statements (for current thread)\n")
            dao.selectStatements.get()?.keys?.forEach { sql ->
                sb.append(' ').append(sql).append("\n")
            }

            arrayOf(
                    "insert statements" to dao.insertStatement,
                    "update statements" to dao.updateStatements,
                    "delete statements" to dao.deleteStatement
            ).forEach { (text, stmts) ->
                sb.append("  ").append(text).append(": ").append(stmts)
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
        flattened { isNullable, simple ->
            if (value == null) {
                check(isNullable)
                statement.setNull(i, Types.NULL)
            } else {
                val v = simple.store(value)
                when (simple.kind) {
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
                }.also { }
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> DataType<T>.get(resultSet: ResultSet, index: Int): T {
        val i = 1 + index

        return flattened { isNullable, simple ->
            val v = when (simple.kind) {
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

            // must check, will get zeroes otherwise
            if (resultSet.wasNull()) check(isNullable).let { null as T }
            else simple.load(v)
        }
    }

}
