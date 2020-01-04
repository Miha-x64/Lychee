package net.aquadc.persistence.sql

import net.aquadc.persistence.array
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.approxType
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import net.aquadc.persistence.sql.dialect.Dialect
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
@ExperimentalSql
class JdbcSession(
        @JvmField @JvmSynthetic internal val connection: Connection,
        @JvmField @JvmSynthetic internal val dialect: Dialect
) : Session {

    init {
        connection.autoCommit = false
    }

    @JvmField @JvmSynthetic internal val lock = ReentrantReadWriteLock()

    @Suppress("UNCHECKED_CAST")
    override fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(
            table: Table<SCH, ID, REC>
    ): Dao<SCH, ID, REC> =
            getDao(table) as Dao<SCH, ID, REC>

    @JvmSynthetic internal fun <SCH : Schema<SCH>, ID : IdBound> getDao(table: Table<SCH, ID, *>): RealDao<SCH, ID, *, PreparedStatement> =
            lowLevel.daos.getOrPut(table) { RealDao(this, lowLevel, table as Table<SCH, ID, Record<SCH, ID>>, dialect) } as RealDao<SCH, ID, *, PreparedStatement>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    @JvmField @JvmSynthetic internal var transaction: RealTransaction? = null

    private val lowLevel: LowLevelSession<PreparedStatement> = object : LowLevelSession<PreparedStatement> {

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID {
            val dao = getDao(table)
            val statement = dao.insertStatement ?: connection.prepareStatement(dialect.insert(table), Statement.RETURN_GENERATED_KEYS).also { dao.insertStatement = it }

            bindInsertionParams(table, data) { type, idx, value ->
                type.bind(statement, idx, value)
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
                                    if (cols is Array<*>) cols as Array<out StoredNamedLens<SCH, *, *>>
                                    else arrayOf(cols as StoredNamedLens<SCH, *, *>)
                            connection.prepareStatement(dialect.updateQuery(table, colArray))
                        }

        override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID, *>, id: ID, columns: Any, values: Any?) {
            val statement = updateStatementWLocked(table, columns)
            val colCount = bindValues(columns, values) { type, idx, value ->
                type.bind(statement, idx, value)
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

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
                table: Table<SCH, ID, *>,
                columns: Array<out StoredNamedLens<SCH, *, *>>?,
                condition: WhereCondition<SCH>,
                order: Array<out Order<out SCH>>
        ): ResultSet {
            val query =
                    if (columns == null) dialect.selectCountQuery(table, condition)
                    else dialect.selectQuery(table, columns, condition, order)

            return getDao(table)
                    .selectStatements
                    .getOrSet(::HashMap)
                    .getOrPut(query) { connection.prepareStatement(query) }
                    .also { stmt ->
                        bindQueryParams(condition, table) { type, idx, value ->
                            type.bind(stmt, idx, value)
                        }
                    }
                    .executeQuery()
        }

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
                table: Table<SCH, ID, *>, column: StoredNamedLens<SCH, T, *>, id: ID
        ): T =
                select<SCH, ID>(table /* fixme allocation */, arrayOf(column), localReusableCond.pkCond<SCH, ID>(table, id), NoOrder)
                        .fetchSingle(column.approxType)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
                table: Table<SCH, ID, *>, condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
        ): Array<ID> =
                select<SCH, ID>(table /* fixme allocation */, arrayOf(table.pkColumn), condition, order)
                        .fetchAllRows(table.idColType)
                        .array<Any>() as Array<ID>

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(
                table: Table<SCH, ID, *>, condition: WhereCondition<SCH>
        ): Long =
                select<SCH, ID>(table, null, condition, NoOrder).fetchSingle(long)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
                table: Table<SCH, ID, *>, columns: Array<out StoredNamedLens<SCH, *, *>>, id: ID
        ): Array<Any?> =
                select<SCH, ID>(table, columns, localReusableCond.pkCond<SCH, ID>(table, id), NoOrder).fetchColumns(columns)

        override val transaction: RealTransaction?
            get() = this@JdbcSession.transaction

        @Suppress("UPPER_BOUND_VIOLATED")
        private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

        private fun <T> ResultSet.fetchAllRows(type: DataType<T>): List<T> {
            // TODO pre-size collection && try not to box primitives
            val values = ArrayList<T>()
            while (next())
                values.add(type.get(this, 0))
            close()
            return values
        }

        private fun <T> ResultSet.fetchSingle(type: DataType<T>): T =
                try {
                    check(next())
                    type.get(this, 0)
                } finally {
                    close()
                }

        private fun <SCH : Schema<SCH>> ResultSet.fetchColumns(columns: Array<out StoredNamedLens<SCH, *, *>>): Array<Any?> =
                try {
                    check(next())
                    columns.mapIndexedToArray { index, column ->
                        column.type.get(this, index)
                    }
                } finally {
                    close()
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


    override fun beginTransaction(): Transaction =
        createTransaction(lock, lowLevel).also { transaction = it }

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

}
