package net.aquadc.persistence.sql.blocking

import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.forEach
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.fatMapTo
import net.aquadc.persistence.sql.ExperimentalSql
import net.aquadc.persistence.sql.FreeExchange
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.InternalTransaction
import net.aquadc.persistence.sql.ListChanges
import net.aquadc.persistence.sql.MutableTransaction
import net.aquadc.persistence.sql.ReadableTransaction
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.SqlTypeName
import net.aquadc.persistence.sql.Table
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
import net.aquadc.persistence.sql.mutate
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.minus
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.serialized
import java.io.Closeable
import java.io.PrintWriter
import java.lang.Exception
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Statement.RETURN_GENERATED_KEYS
import java.sql.Types
import javax.sql.DataSource

/**
 * Base for JDBC Session and Transaction.
 */
abstract class JdbcExchange internal constructor(
    @JvmField protected val dialect: Dialect,
) : FreeExchange<ResultSet> {

    // Source

    final override fun sizeHint(cursor: ResultSet): Int =
        -1
    final override fun next(cursor: ResultSet): Boolean =
        cursor.next()

    final override fun <T> cellByName(cursor: ResultSet, name: CharSequence, type: Ilk<T, *>): T =
        type.get1indexed(cursor, cursor.findColumn(name.toString()))
    final override fun <T> cellAt(cursor: ResultSet, col: Int, type: Ilk<T, *>): T =
        type.get(cursor, col)
    final override fun rowByName(cursor: ResultSet, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>): Array<Any?> =
        Array(columnNames.size) { idx -> cellByName(cursor, columnNames[idx], columnTypes[idx]) }
    final override fun rowByPosition(cursor: ResultSet, offset: Int, types: Array<out Ilk<*, *>>): Array<Any?> =
        Array(types.size) { idx -> types[idx].get(cursor, offset + idx) }

    final override fun close(cursor: ResultSet) =
        cursor.close()

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun <T> Ilk<out T, *>.get(resultSet: ResultSet, index: Int): T {
        return get1indexed(resultSet, 1 + index)
    }
    protected fun <T> Ilk<out T, *>.get1indexed(resultSet: ResultSet, i: Int): T = custom.let { custom ->
        if (custom != null) {
            custom.load(resultSet.statement.connection, resultSet.getObject(i))
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

    // FreeSource

    final override fun <T> cell(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        sessionAndArguments: Array<out Any>,
        type: Ilk<out T, *>,
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

    protected fun select(
        connection: Connection,
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        sessionAndArguments: Array<out Any>,
        expectedCols: Int
    ): ResultSet = connection.prepareStatement(query, 0).let { stmt ->
        for (idx in argumentTypes.indices) {
            (argumentTypes[idx] as Ilk<Any?, *>).bind(stmt, idx, sessionAndArguments[idx + 1])
        }
        closeAlongResultSet(stmt.executeQuery().also {
            val meta = stmt.metaData
            val actualCols = meta.columnCount
            if (actualCols != expectedCols) {
                val cols = Array(actualCols) { meta.getColumnLabel(it + 1) }.contentToString()
                throw IllegalArgumentException("Expected $expectedCols cols, got $cols") // todo relax, bro
            }
        }, stmt)
    }
    protected fun closeAlongResultSet(rs: ResultSet, victim: AutoCloseable) =
        Proxy.newProxyInstance(ResultSet::class.java.classLoader, arrayOf(ResultSet::class.java)) { _, meth, args ->
            if (meth.name == "close" && args.isNullOrEmpty()) {
                rs.close()
                victim.close()
            } else {
                try {
                    meth.invoke(rs, *(args ?: EMPTY_ARRAY))
                } catch (e: Exception) {
                    throw e
                }
            }
        } as ResultSet
    private companion object {
        private val EMPTY_ARRAY = emptyArray<Any>()
    }

    protected fun <ID> execute(
        connection: Connection,
        query: String,
        retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>
    ): Any? = connection.prepareStatement(query, if (retKeyType != null) RETURN_GENERATED_KEYS else 0).use { statement ->
        val altered = statement
            .also { stmt ->
                for (idx in argumentTypes.indices) {
                    (argumentTypes[idx] as Ilk<Any?, *>).bind(stmt, idx, transactionAndArguments[idx + 1])
                }
            }
            .executeUpdate()

        if (retKeyType == null) altered else {
            check(altered == 1) {
                "Returning generated key is possible for single inserted row but $altered rows were inserted"
                // xerial SQLite driver does not support selecting several inserted PKs, for example
            }
            statement.generatedKeys.fetchSingle(retKeyType)
        }
    }

    protected fun <T> Ilk<T, *>.bind(statement: PreparedStatement, index: Int, value: T) {
        // TODO try-catch-rethrow with cause, request, index
        val i = 1 + index
        val custom = this.custom
        if (custom != null) {
            statement.setObject(i, custom.store(statement.connection, value))
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
                                statement.connection.createArrayOf(
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
    protected fun <T> ResultSet.fetchSingle(type: Ilk<T, *>): T =
        try {
            check(next())
            type.get(this, 0)
        } finally {
            close()
        }

    // Exchange

    protected fun <ID : IdBound, SCH : Schema<SCH>> insert(conn: Connection, table: Table<SCH, ID>, data: PartialStruct<SCH>): ID {
        val sql = with(dialect) { StringBuilder().insert(table, data.fields).toString() }
        return conn.prepareStatement(sql, RETURN_GENERATED_KEYS).use {
            bindInsertionParams(table, data) { type, idx, value -> type.bind(it, idx, value) }
            check(it.executeUpdate() == 1)
            it.generatedKeys.fetchSingle(table.idColType)
        }
    }

    protected fun <ID : IdBound, SCH : Schema<SCH>> update(conn: Connection, table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>) {
        val fields = table.pkField?.let { patch.fields - it } ?: patch.fields
        val sql = with(dialect) { StringBuilder().update(table, fields).toString() }
        conn.prepareStatement(sql).use {
            val count = bindInsertionParams(table, patch) { type, idx, value -> type.bind(it, idx, value) }
            table.idColType.bind(it, count, id)
            check(it.executeUpdate() == 1)
        }
    }

    protected fun <ID : IdBound, SCH : Schema<SCH>> delete(conn: Connection, table: Table<SCH, ID>, id: ID) {
        val sql = dialect.deleteRecordQuery(table)
        conn.prepareStatement(sql).use {
            table.idColType.type.bind(it, 0, id)
            check(it.executeUpdate() == 1)
        }
    }

}

/**
 * Represents a database connection through JDBC.
 */
@ExperimentalSql
class JdbcSession

/**
 * Create the session with [DataSource] connection factory.
 *
 * Optimal choice for server environment.
 * SQLite triggers won't work here.
 */
constructor(
        @JvmField @JvmSynthetic internal val dataSource: DataSource,
        dialect: Dialect,
        /**
         * If your application is distributed, pass something identifying current node
         * to avoid temp table or trigger name clashes.
         */
        nodeName: String = genNodeName()
) : JdbcExchange(dialect), Session<ResultSet> {

    private var singleConnection: AutoCloseable? = null

    /**
     * Create the session with a single connection.
     *
     * This makes sense on SQLite where the triggers are connection-local.
     * Don't do this in server environment.
     * The resulting session won't be thread safe.
     */
    constructor(connection: Connection, dialect: Dialect, nodeName: String = genNodeName()) :
        this(
            @Suppress("ABSTRACT_MEMBER_NOT_IMPLEMENTED") object : DataSource {
                override fun getConnection(): Connection =
                    object : Connection by connection {
                        override fun close() {}
                    }
                override fun getConnection(username: String?, password: String?): Connection =
                    throw UnsupportedOperationException()
                override fun getLogWriter(): PrintWriter =
                    throw UnsupportedOperationException()
                override fun setLogWriter(out: PrintWriter?): Unit =
                    throw UnsupportedOperationException()
                override fun setLoginTimeout(seconds: Int): Unit =
                    throw UnsupportedOperationException()
                override fun getLoginTimeout(): Int =
                    throw UnsupportedOperationException()
                override fun <T : Any?> unwrap(iface: Class<T>?): T =
                    throw UnsupportedOperationException()
                override fun isWrapperFor(iface: Class<*>?): Boolean =
                    false
                override fun toString(): String =
                    javaClass.name + '(' + connection.toString() + ')'
            }, dialect, nodeName
        ) {
            singleConnection = connection
        }

    private companion object {
        private fun genNodeName() = // “-” does not seem to be a good identifier, let's be alphanumeric
            java.lang.Double.doubleToLongBits(Math.random()).toString(36).replace('-', 'm')
    }

    private val changesPostfix = '_' + nodeName + "_changes"

    // FreeSource

    override fun select(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        sessionAndArguments: Array<out Any>,
        expectedCols: Int
    ): ResultSet {
        val conn = dataSource.connection
        val rs = select(conn, query, argumentTypes, sessionAndArguments, expectedCols)
        return closeAlongResultSet(rs, conn)
    }

    override fun <ID> execute(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>,
        retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any? =
        dataSource.connection.use { execute(it, query, retKeyType, argumentTypes, transactionAndArguments) }
            .also { deliverTriggeredChanges() }

    // Exchange

    // Exchange

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID =
        dataSource.connection.use { insert(it, table, data) }
            .also { deliverTriggeredChanges() }

    override fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<Struct<SCH>>) {
        mutate {
            for (struct in data)
                insert(table, struct)
        }
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>): Unit =
        dataSource.connection.use { update(it, table, id, patch) }
            .also { deliverTriggeredChanges() }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID): Unit =
        dataSource.connection.use { delete(it, table, id) }
            .also { deliverTriggeredChanges() }

    // Session

    override fun read(): ReadableTransaction<ResultSet> =
        JdbcTransaction(newTrConn(), true)
    //       always commit read-only ^^^^ transactions
    //  https://medium.com/javarevisited/spring-never-rollback-readonly-transactions-ffc21958b0d0

    override fun mutate(): MutableTransaction<ResultSet> =
        JdbcTransaction(newTrConn(), false)

    private val triggers = Triggerz()
    private var triggerConn: Connection? = null // create temp table and set triggers in a single connection
    override fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable =
        synchronized(triggers) {
            val conn = triggerConn ?: dataSource.connection.also { triggerConn = it }
            triggers.addListener({
                conn.autoCommit = false
                JdbcTransaction(conn, false)
           }, subject, listener)
        }

    private fun newTrConn() =
        dataSource.connection.also { it.autoCommit = false }

    override fun trimMemory() {
        dialect.trimMemory()?.let { sql ->
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(sql)
                }
            }
        }
    }

    override fun close() {
        singleConnection?.let {
            it.close()
            singleConnection = null // just 'cause we can
        }
        synchronized(triggers) {
            triggerConn?.let {
                it.close()
                triggerConn = null
            }
        }
    }

    // misc

    override fun toString(): String =
        "JdbcSession(dataSource=$dataSource, dialect=${dialect.javaClass.simpleName})"

    private inner class JdbcTransaction(
        private val conn: Connection,
        readOnly: Boolean,
    ) : JdbcExchange(dialect), InternalTransaction<ResultSet> {

        private var isSuccessful = readOnly

        // FreeSource

        override fun select(
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            sessionAndArguments: Array<out Any>,
            expectedCols: Int
        ): ResultSet =
            select(conn, query, argumentTypes, sessionAndArguments, expectedCols)

        override fun <ID> execute(
            query: String,
            argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
            transactionAndArguments: Array<out Any>,
            retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
        ): Any? =
            execute(conn, query, retKeyType, argumentTypes, transactionAndArguments)

        // Transaction

        override fun setSuccessful() {
            isSuccessful = true
        }

        override fun close() {
            close(true)
        }

        override fun close(deliver: Boolean) {
            val successful = isSuccessful
            try {
                try {
                    if (successful) conn.commit()
                    else conn.rollback()
                } finally {
                    conn.autoCommit = true
                }
            } finally {
                if (deliver)
                    conn.close() // don't close triggerConn itself, we still need it
            }

            if (successful && deliver) {
                deliverTriggeredChanges()
            }
        }

        // Exchange

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID =
            insert(conn, table, data)

        override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>): Unit =
            update(conn, table, id, patch)

        override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID): Unit =
            delete(conn, table, id)

        // InternalTransaction

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
        ): T =
            select<SCH, ID>(conn, table, id, /* fixme allocation */ arrayOf(colName)).fetchSingle(colType)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
        ): Array<Any?> =
            select<SCH, ID>(conn, table, id, columnNames).fetchColumns(columnTypes)

        override fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            conn.createStatement().use { stmt ->
                newbies.forEach { (table, events) ->
                    val wordCount = wordCountForCols(table.managedColumns.size)
                    val typeNames = arrayOfNulls<SqlTypeName>(wordCount + 1)
                    typeNames[0] = i32
                    typeNames.fill(i64, 1)
                    with(dialect) {
                        stmt.execute(
                            sb.createTable(
                                temporary = true, ifNotExists = true,
                                name = table.name, namePostfix = changesPostfix,
                                idColName = "id", idColTypeName = table.idColTypeName,
                                managedPk = false /* avoid AUTO_INCREMENT or serial*/,
                                colNames = arrayOf("what") + (0 until wordCount).map { "ch$it" },
                                colTypes = typeNames as Array<out SqlTypeName>
                            ).toString()
                        )
                    }
                    events.forEach { event ->
                        prepareAndCreateTrigger(sb, event, table, stmt, create = true)
                    }
                }
            }
        }
        override fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            conn.createStatement().use { stmt ->
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
            }
        }

        // misc

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
            connection: Connection,
            table: Table<SCH, ID>,
            id: ID,
            columns: Array<out CharSequence>
        ): ResultSet {
            val query = dialect.run { StringBuilder().selectQuery(table, columns).toString() }
            val stmt = connection.prepareStatement(query, 0)
            bindQueryParams(table, id) { type, idx, value -> type.bind(stmt, idx, value) }
            return closeAlongResultSet(stmt.executeQuery(), stmt)
        }

        private fun ResultSet.fetchColumns(types: Array<out Ilk<*, *>>): Array<Any?> =
            try {
                check(next())
                types.mapIndexedToArray { index, type -> type.get(this, index) }
            } finally {
                close()
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

    private fun deliverTriggeredChanges() {
        synchronized(triggers) {
            triggerConn?.let(::deliverTriggeredChangesLocked)
        }
    }
    private fun deliverTriggeredChangesLocked(connection: Connection) {
        val activeSubjects = triggers.activeSubjects()
        if (activeSubjects.isEmpty()) return
        val tableToChanges = HashMap<Table<*, *>, ListChanges<*, *>>()
        connection.autoCommit = false
        val stmt = connection.createStatement()
        try {
            val sb = StringBuilder()
            activeSubjects.forEach { table ->
                sb.setLength(0)
                val wordCount = wordCountForCols(table.managedColumns.size)
                val inserted = HashSet<IdBound>()
                val updated = HashMap<IdBound, Int>()
                val removed = HashSet<IdBound>()
                val changes = ArrayList<Long>() // OMG

                stmt.executeQuery(
                    with(dialect) {
                        sb.append("SELECT * FROM").append(' ').appendName(table.name + changesPostfix).toString()
                    }
                ).use { rs ->
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
                }
                if (inserted.isNotEmpty() || updated.isNotEmpty() || removed.isNotEmpty() || changes.isNotEmpty())
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
        } finally {
            connection.autoCommit = true
        }

        triggers.notifyPending()
    }

}
