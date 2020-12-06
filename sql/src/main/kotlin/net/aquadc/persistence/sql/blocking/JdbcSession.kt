package net.aquadc.persistence.sql.blocking

import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.forEach
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.fatMapTo
import net.aquadc.persistence.newMap
import net.aquadc.persistence.sql.ExperimentalSql
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.ListChanges
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.SqlTypeName
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.Transaction
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.sql.TriggerReport
import net.aquadc.persistence.sql.TriggerSubject
import net.aquadc.persistence.sql.Triggerz
import net.aquadc.persistence.sql.appendJoining
import net.aquadc.persistence.sql.bindInsertionParams
import net.aquadc.persistence.sql.bindQueryParams
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.foldArrayType
import net.aquadc.persistence.sql.mapIndexedToArray
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.serialized
import java.io.Closeable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Types
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet

/**
 * Represents a database connection through JDBC.
 */
@ExperimentalSql
class JdbcSession(
        @JvmField @JvmSynthetic internal val connection: Connection,
        @JvmField @JvmSynthetic internal val dialect: Dialect,
        /**
         * If your application is distributed, pass something identifying current node
         * to avoid temp table or trigger name clashes.
         */
        nodeName: String = // “-” does not seem to be a good identifier, let's be alphanumeric
            java.lang.Double.doubleToLongBits(Math.random()).toString(36).replace('-', 'm')
) : Session<Blocking<ResultSet>> {
    val changesPostfix = '_' + nodeName + "_changes"

    @JvmField @JvmSynthetic internal val lock = ReentrantReadWriteLock()

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    @JvmField @JvmSynthetic internal var transaction: RealTransaction<Blocking<ResultSet>>? = null

    private val lowLevel: LowLevelSession<PreparedStatement, ResultSet> = object : LowLevelSession<PreparedStatement, ResultSet>() {

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID {
            val sql = with(dialect) { StringBuilder().insert(table, data.fields).toString() }
            val key = Pair(sql, null)
            val statements = statements.getOrSet(::newMap)
            val statement = statements.getOrPut(key) {
                connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
            }
            bindInsertionParams(table, data) { type, idx, value ->
                type.bind(statement, idx, value)
            }
            statement.executeOrEvict(statements, key)
            return statement.generatedKeys.fetchSingle(table.idColType)
        }
        override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, primaryKey: ID) {
            val sql = dialect.deleteRecordQuery(table)
            val statements = statements.getOrSet(::newMap)
            val statement = statements.getOrPut(sql) { connection.prepareStatement(sql) }
            table.idColType.type.bind(statement, 0, primaryKey)
            statement.executeOrEvict(statements, sql)
        }
        private fun PreparedStatement.executeOrEvict(statements: MutableMap<Any, PreparedStatement>, key: Any): Unit =
            try {
                check(executeUpdate() == 1)
            } catch (e: SQLException) {
                close() // poisoned statement
                statements.remove(key)
                throw e
            }

        override fun truncate(table: Table<*, *>) {
            val stmt = connection.createStatement()
            try {
                stmt.execute(dialect.truncate(table))
            } finally {
                stmt.close()
            }
        }

        override fun onTransactionEnd(successful: Boolean) {
            val transaction = transaction ?: throw AssertionError()
            try {
                if (successful) {
                    connection.commit()
                } else {
                    connection.rollback()
                }
                this@JdbcSession.transaction = null
            } finally {
                lock.writeLock().unlock()
            }

            // new SQL-friendly API
            if (successful) {
                deliverTriggeredChanges()
            }
        }

        // copy-paste, keep in sync with SQLite session
        private fun deliverTriggeredChanges() {
            val activeSubjects = triggers.activeSubjects()
            if (activeSubjects.isEmpty()) return
            val tableToChanges = HashMap<Table<*, *>, ListChanges<*, *>>()
            connection.autoCommit = false
            val stmt = connection.createStatement()
            try {
                val sb = StringBuilder()
                activeSubjects.forEach { table ->
                    sb.setLength(0)
                    val rs = stmt.executeQuery(
                        with(dialect) {
                            sb.append("SELECT * FROM").append(' ').appendName(table.name + changesPostfix).toString()
                        }
                    )

                    val wordCount = wordCountForCols(table.managedColumns.size)
                    val inserted = HashSet<IdBound>()
                    val updated = HashMap<IdBound, Int>()
                    val removed = HashSet<IdBound>()
                    val changes = ArrayList<Long>() // OMG

                    val pkType = table.idColType
                    while (rs.next()) {
                        val pk = pkType.get(rs, 0)
                        when (val what = rs.getInt(1 + 1)) {
                            -1 -> removed.add(pk)
                            0 -> {
                                repeat(wordCount) { word -> changes.add(rs.getLong(2 + 1 + word)) }
                                check(updated.put(pk, changes.size / wordCount - 1) == null)
                            }
                            1 -> inserted.add(pk)
                            else -> error(what.toString())
                        }
                    }
                    check(tableToChanges.put(
                        table,
                        ListChanges(inserted, updated, removed, table as Table<NullSchema, IdBound>, changes.toLongArray())
                    ) == null)

                    sb.setLength(0)
                    stmt.execute(
                        with(dialect) {
                            sb.append("DELETE FROM").append(' ').appendName(table.name + changesPostfix).toString()
                        }
                    )
                }
                stmt.close()

                triggers.enqueue(TriggerReport(tableToChanges))
                connection.commit()
            } catch (t: Throwable) {
                connection.rollback()
                throw t
            }

            triggers.notifyPending()
        }

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
            table: Table<SCH, ID>,
            id: ID,
            columns: Array<out CharSequence>
        ): ResultSet {
            val query = dialect.run { StringBuilder().selectQuery(table, columns).toString() }

            return statement(query, false)
                    .also { stmt ->
                        bindQueryParams(table, id) { type, idx, value ->
                            type.bind(stmt, idx, value)
                        }
                    }
                    .executeQuery()
        }

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
        ): T =
            select<SCH, ID>(table, id, /* fixme allocation */ arrayOf(colName)).fetchSingle(colType)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
        ): Array<Any?> =
            select<SCH, ID>(table, id, columnNames).fetchColumns(columnTypes)

        override val transaction: RealTransaction<Blocking<ResultSet>>?
            get() = this@JdbcSession.transaction

        private fun <T> ResultSet.fetchAllRows(type: Ilk<T, *>): List<T> {
            // TODO pre-size collection && try not to box primitives
            val values = ArrayList<T>()
            while (next())
                values.add(type.get(this, 0))
            close()
            return values
        }

        private fun <T> ResultSet.fetchSingle(type: Ilk<T, *>): T =
                try {
                    check(next())
                    type.get(this, 0)
                } finally {
                    close()
                }

        private fun ResultSet.fetchColumns(types: Array<out Ilk<*, *>>): Array<Any?> =
                try {
                    check(next())
                    types.mapIndexedToArray { index, type ->
                        type.get(this, index)
                    }
                } finally {
                    close()
                }

        private fun <T> Ilk<T, *>.bind(statement: PreparedStatement, index: Int, value: T) {
            val i = 1 + index
            val custom = this.custom
            if (custom != null) {
                statement.setObject(i, custom.invoke(value))
            } else {
                val t = type as DataType<T>
                val type = if (t is DataType.Nullable<*, *>) {
                    if (value == null) {
                        statement.setNull(i, Types.NULL)
                        return
                    }
                    t.actualType as DataType.NotNull<T>
                } else type as DataType.NotNull<T>

                when (type) {
                    is DataType.NotNull.Simple -> {
                        val v = type.store(value)
                        when (type.kind) {
                            DataType.NotNull.Simple.Kind.Bool -> statement.setBoolean(i, v as Boolean)
                            DataType.NotNull.Simple.Kind.I32 -> statement.setInt(i, v as Int)
                            DataType.NotNull.Simple.Kind.I64 -> statement.setLong(i, v as Long)
                            DataType.NotNull.Simple.Kind.F32 -> statement.setFloat(i, v as Float)
                            DataType.NotNull.Simple.Kind.F64 -> statement.setDouble(i, v as Double)
                            DataType.NotNull.Simple.Kind.Str -> statement.setString(i, v as String)
                            // not sure whether setBlob should be used:
                            DataType.NotNull.Simple.Kind.Blob -> statement.setObject(i, v as ByteArray)
                        }//.also { }
                    }
                    is DataType.NotNull.Collect<T, *, *> -> {
                        foldArrayType(
                            dialect.hasArraySupport, type.elementType,
                            { nullable, elT ->
                                statement.setArray(i,
                                    connection.createArrayOf(
                                        jdbcElType(type.elementType),
                                        toArray(type.store(value), nullable, elT)
                                    )
                                )
                            },
                            {
                                statement.setObject(i, serialized(type).store(value))
                            }
                        )
                    }
                    is DataType.NotNull.Partial<T, *> -> {
                        throw AssertionError() // 🤔 btw, Oracle supports Struct type
                    }
                }
            }
        }
        private fun jdbcElType(t: DataType<*>): String = when (t) {
            is DataType.Nullable<*, *> -> jdbcElType(t.actualType)
            is DataType.NotNull.Simple -> dialect.nameOf(t.kind)
            is DataType.NotNull.Collect<*, *, *> -> jdbcElType(t.elementType)
            is DataType.NotNull.Partial<*, *> -> dialect.nameOf(DataType.NotNull.Simple.Kind.Blob)
        }
        private fun <T> toArray(value: AnyCollection, nullable: Boolean, elT: DataType.NotNull.Simple<T>): Array<out Any?> =
            (value.fatAsList() as List<T?>).let { value ->
                Array<Any?>(value.size) {
                    val el = value[it]
                    if (el == null) castNull(nullable, elT::toString)
                    else elT.store(el)
                }
            }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        private /*wannabe inline*/ fun <T> Ilk<T, *>.get(resultSet: ResultSet, index: Int): T {
            return get1indexed(resultSet, 1 + index)
        }

        private fun <T> Ilk<T, *>.get1indexed(resultSet: ResultSet, i: Int): T = custom.let { custom ->
            if (custom != null) {
                custom.back(resultSet.getObject(i))
            } else {
                val t = type as DataType<T>
                val nullable: Boolean
                val type =
                    if (t is DataType.Nullable<*, *>) { nullable = true; t.actualType as DataType.NotNull<T> }
                    else { nullable = false; type as DataType.NotNull<T> }
                when (type) {
                    is DataType.NotNull.Simple -> {
                        val v = when (type.kind) {
                            DataType.NotNull.Simple.Kind.Bool -> resultSet.getBoolean(i)
                            DataType.NotNull.Simple.Kind.I32 -> resultSet.getInt(i)
                            DataType.NotNull.Simple.Kind.I64 -> resultSet.getLong(i)
                            DataType.NotNull.Simple.Kind.F32 -> resultSet.getFloat(i)
                            DataType.NotNull.Simple.Kind.F64 -> resultSet.getDouble(i)
                            DataType.NotNull.Simple.Kind.Str -> resultSet.getString(i)
                            DataType.NotNull.Simple.Kind.Blob -> resultSet.getBytes(i)
                            else -> throw AssertionError()
                        }
                        // must check, will get zeroes otherwise
                        if (resultSet.wasNull()) castNull(nullable) { "$type at [$i+1]" }
                        else type.load(v)
                    }
                    is DataType.NotNull.Collect<T, *, *> -> {
                        foldArrayType(dialect.hasArraySupport, type.elementType,
                            { nullable, elT ->
                                val arr = resultSet.getArray(i)
                                if (resultSet.wasNull()) castNull(nullable) { "$type at [$i+1]" }
                                else fromArray(type, arr.array as Array<out Any?>, nullable, elT)
                            },
                            {
                                val obj = resultSet.getObject(i)
                                if (resultSet.wasNull()) castNull(nullable) { "$type at [$i+1]" }
                                else serialized(type).load(obj)
                            }
                        )
                    }
                    is DataType.NotNull.Partial<T, *> -> {
                        throw AssertionError()
                    }
                }
            }
        }
        private fun <T> fromArray(type: DataType.NotNull.Collect<T, *, *>, value: AnyCollection, nullable: Boolean, elT: DataType.NotNull.Simple<*>): T =
            type.load(value.fatMapTo(ArrayList<Any?>()) { it: Any? ->
                if (it == null) castNull(nullable, elT::toString) else elT.load(it)
            })


        override fun <T> cell(
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            sessionAndArguments: Array<out Any>,
            type: Ilk<T, *>,
            orElse: () -> T
        ): T {
            val rs = select(query, argumentTypes, sessionAndArguments, 1)
            try {
                if (!rs.next()) return orElse()
                val value = type.get(rs, 0)
                check(!rs.next())
                return value
            } finally {
                rs.close()
            }
        }

        override fun select(
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
            expectedCols: Int
        ): ResultSet = statement(query, false)
                .also { stmt ->
                    for (idx in argumentTypes.indices) {
                        (argumentTypes[idx] as Ilk<Any?, *>).bind(stmt, idx, sessionAndArguments[idx+1])
                    }
                }
                .executeQuery()
                .also {
                    val meta = it.metaData
                    val actualCols = meta.columnCount
                    if (actualCols != expectedCols) {
                        val cols = Array(actualCols) { meta.getColumnLabel(it + 1) }.contentToString()
                        it.close()
                        throw IllegalArgumentException("Expected $expectedCols cols, got $cols") // todo relax, bro
                    }
                }
        override fun sizeHint(cursor: ResultSet): Int = -1
        override fun next(cursor: ResultSet): Boolean = cursor.next()

        override fun <ID> execute(
            query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            transactionAndArguments: Array<out Any>, retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
        ): Any? {
            val statement = statement(query, retKeyType != null)
            val altered = statement
                .also { stmt ->
                    for (idx in argumentTypes.indices) {
                        (argumentTypes[idx] as Ilk<Any?, *>).bind(stmt, idx, transactionAndArguments[idx + 1])
                    }
                }
                .executeUpdate()
            return if (retKeyType == null) altered else {
                check(altered == 1) {
                    "Returning generated key is possible for single inserted row but $altered rows were inserted"
                    // xerial SQLite driver does not support selecting several inserted PKs, for example
                }
                statement.generatedKeys.fetchSingle(retKeyType)
            }
        }

        private fun statement(query: String, retKeys: Boolean): PreparedStatement =
            statements
                .getOrSet(::HashMap)
                .getOrPut(if (retKeys) Pair(query, null) else query) {
                    connection.prepareStatement(query, if (retKeys) Statement.RETURN_GENERATED_KEYS else 0)
                }

        override fun <T> cellByName(cursor: ResultSet, name: CharSequence, type: Ilk<T, *>): T =
                type.get1indexed(cursor, cursor.findColumn(name.toString()))
        override fun <T> cellAt(cursor: ResultSet, col: Int, type: Ilk<T, *>): T =
                type.get(cursor, col)
        override fun rowByName(cursor: ResultSet, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>): Array<Any?> =
                Array(columnNames.size) { idx -> cellByName(cursor, columnNames[idx], columnTypes[idx]) }
        override fun rowByPosition(cursor: ResultSet, offset: Int, types: Array<out Ilk<*, *>>): Array<Any?> =
                Array(types.size) { idx -> types[idx].get(cursor, offset + idx) }

        override fun close(cursor: ResultSet) =
            cursor.close()

        // copy-paste, keep in sync with SQLite session
        override fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            connection.autoCommit = false
            val stmt = connection.createStatement()
            try {
                newbies.forEach { (table, events) ->
                    val wordCount = wordCountForCols(table.managedColumns.size)
                    val typeNames = arrayOfNulls<SqlTypeName>(wordCount + 1)
                    typeNames[0] = i32
                    typeNames.fill(i64, 1)
                    with(dialect) {
                        stmt.execute(sb.createTable(
                            temporary = true, ifNotExists = true,
                            name = table.name, namePostfix = changesPostfix,
                            idColName = "id", idColTypeName = table.idColTypeName,
                            managedPk = false /* avoid AUTO_INCREMENT or serial*/,
                            colNames = arrayOf("what") + (0 until wordCount).map { "ch$it" },
                            colTypes = typeNames as Array<out SqlTypeName>
                        ).toString())
                    }
                    events.forEach { event ->
                        prepareAndCreateTrigger(sb, event, table, stmt, create = true)
                    }
                }
                stmt.close()
                connection.commit()
            } catch (t: Throwable) {
                connection.rollback()
                throw t
            }
        }

        override fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            val stmt = connection.createStatement()
            try {
                victims.forEach { (table, events) ->
                    stmt.execute(
                        with(dialect) {
                            sb.append("DELETE FROM").append(' ').appendName(table.name + changesPostfix)
                                .append(' ').append("WHERE").append(' ')
                                .appendJoining(events, " OR ") { ev -> append("what").append('=').append(ev.balance) }
                                .toString()
                        }
                    )
                    events.forEach { event ->
                        prepareAndCreateTrigger(sb, event, table, stmt, create = false)
                    }
                }
                stmt.close()
                connection.commit()
            } catch (t: Throwable) {
                connection.rollback()
                throw t
            }
        }

        @Suppress("UPPER_BOUND_VIOLATED")
        private fun prepareAndCreateTrigger(
            sb: StringBuilder, event: TriggerEvent, table: Table<*, *>, stmt: Statement, create: Boolean
        ): Unit = with(dialect) {
            table as Table<Schema<*>, IdBound>

            if (!create) { // drop trigger before dropping function as trigger depends on it
                sb.setLength(0)
                stmt.execute(sb.changesTrigger<Schema<*>, IdBound>(changesPostfix, event, table, create = false).toString())
            }

            sb.setLength(0)
            if (sb.prepareChangesTrigger<Schema<*>, IdBound>(changesPostfix, event, table, create).isNotEmpty()) {
                stmt.execute(sb.toString())
            }

            if (create) {
                sb.setLength(0)
                stmt.execute(sb.changesTrigger<Schema<*>, IdBound>(changesPostfix, event, table, create = true).toString())
            }
        }
    }


    override fun beginTransaction(): Transaction<Blocking<ResultSet>> =
        createTransaction(lock, lowLevel).also {
            connection.autoCommit = false
            transaction = it
        }

    // endregion transactions and modifying statements

    override fun toString(): String =
            "JdbcSession(connection=$connection, dialect=${dialect.javaClass.simpleName})"

    @Deprecated("This was intended to list ActiveRecord's queries", level = DeprecationLevel.ERROR)
    fun dump(sb: StringBuilder) {
    }

    override fun <R> rawQuery(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        argumentValues: Array<out Any>,
        fetch: Fetch<Blocking<ResultSet>, R>
    ): R =
        fetch.fetch(lowLevel, query, argumentTypes, argumentValues)

    private val triggers = Triggerz(lowLevel)
    override fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable =
        triggers.addListener(subject, listener)

    override fun trimMemory() {
        dialect.trimMemory()?.let { sql ->
            val stmt = connection.createStatement()
            stmt.execute(sql)
            stmt.close()
        }
    }

    override fun close() {
        lowLevel.statements.get()?.values
            ?.forEach(PreparedStatement::close) // FIXME: Other threads' statements gonna dangle until GC
        connection.close()
    }

}
