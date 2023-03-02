@file:JvmName("SqliteUtils")
package net.aquadc.persistence.sql.blocking

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteProgram
import android.database.sqlite.SQLiteQuery
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.forEach
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.eq
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
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.appendName
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.changesTrigger
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.createTable
import net.aquadc.persistence.sql.flattened
import net.aquadc.persistence.sql.mapIndexedToArray
import net.aquadc.persistence.sql.mutate
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.minus
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

/**
 * Base for SQLite Session and Transaction.
 */
abstract class SqliteExchange internal constructor(
    @JvmField protected val connection: SQLiteDatabase,
    @JvmField protected val statements: ConcurrentHashMap<String, SQLiteStatement>,
) : FreeExchange<Cursor> {

    // Source

    final override fun sizeHint(cursor: Cursor): Int =
        cursor.count
    final override fun next(cursor: Cursor): Boolean =
        cursor.moveToNext()

    final override fun <T> cellByName(cursor: Cursor, name: CharSequence, type: Ilk<T, *>): T =
        cellByName(cursor, Integer.MAX_VALUE /* don't even try to guess */, name, type)
    final override fun <T> cellAt(cursor: Cursor, col: Int, type: Ilk<T, *>): T =
        (type.type as DataType<T>).get(cursor, col)

    final override fun rowByName(cursor: Cursor, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>): Array<Any?> =
        Array(columnNames.size) { idx ->
            cellByName(cursor, idx, columnNames[idx], columnTypes[idx])
        }
    final override fun rowByPosition(cursor: Cursor, offset: Int, types: Array<out Ilk<*, *>>): Array<Any?> =
        Array(types.size) { idx ->
            types[idx].type.get(cursor, offset + idx)
        }

    final override fun close(cursor: Cursor) =
        cursor.close()

    private fun <T> cellByName(cursor: Cursor, guess: Int, name: CharSequence, type: Ilk<T, *>): T =
        (type.type as DataType<T>).get(cursor, cursor.getColIdx(guess, name))
    protected fun <T> DataType<T>.get(cursor: Cursor, index: Int): T = flattened { isNullable, simple ->
        if (cursor.isNull(index))
            castNull(isNullable) { "$this at [$index]" }
        else simple.load(when (simple.kind) {
            DataType.NotNull.Simple.Kind.Bool -> cursor.getInt(index) == 1
            DataType.NotNull.Simple.Kind.I32 -> cursor.getInt(index)
            DataType.NotNull.Simple.Kind.I64 -> cursor.getLong(index)
            DataType.NotNull.Simple.Kind.F32 -> cursor.getFloat(index)
            DataType.NotNull.Simple.Kind.F64 -> cursor.getDouble(index)
            DataType.NotNull.Simple.Kind.Str -> cursor.getString(index)
            DataType.NotNull.Simple.Kind.Blob -> cursor.getBlob(index)
            else -> throw AssertionError()
        })
    }
    // TODO: could subclass SQLiteCursor and attach IntArray<myColIdx, SQLiteColIdx> instead of looking this up every time
    private fun Cursor.getColIdx(guess: Int, name: CharSequence): Int { // native `getColumnIndex` wrecks labels with '.'!
        val columnNames = columnNames!!
        if (columnNames.size > guess && name.eq(columnNames[guess], false)) return guess
        val idx = columnNames.indexOfFirst { name.eq(it, false) }
        if (idx < 0) error { "$name !in ${columnNames.contentToString()}" }
        return idx
    }

    // FreeSource

    final override fun <T> cell(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        type: Ilk<out T, *>, orElse: () -> T
    ): T {
        val tt = type.type
        val isIntOrLong = tt is DataType.NotNull.Simple &&
            (tt.kind == DataType.NotNull.Simple.Kind.I32 || tt.kind == DataType.NotNull.Simple.Kind.I64)
        if (isIntOrLong || tt.isStringInclNullable) {
            return try {
                simpleQueryForCell(query, argumentTypes, sessionAndArguments, isIntOrLong, tt)
            } catch (e: SQLiteDoneException) {
                orElse()
            }
        }

        val cur = select(query, argumentTypes, sessionAndArguments, 1)
        try {
            if (!cur.moveToFirst()) return orElse()
            val value = (tt as DataType<out T>).get(cur, 0)
            check(!cur.moveToNext()) { "cursor returned ${cur.count} rows, 1 needed" }
            return value
        } finally {
            cur.close()
        }
    }
    private val DataType<*>.isStringInclNullable
        get() = (this is DataType.NotNull.Simple && this.kind == DataType.NotNull.Simple.Kind.Str) ||
            (this is DataType.Nullable<*, *> &&
                this.actualType.let { it is DataType.NotNull.Simple<*> && it.kind == DataType.NotNull.Simple.Kind.Str })
    private fun <T> simpleQueryForCell(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        sessionAndArguments: Array<out Any>,
        isIntOrLong: Boolean,
        tt: DataType<*>
    ) = statement(query) { statement ->
        bindParams(argumentTypes, statement, sessionAndArguments)
        if (isIntOrLong) {
            val long = statement.simpleQueryForLong()
            if ((tt as DataType.NotNull.Simple).kind == DataType.NotNull.Simple.Kind.I32)
                tt.load(long.toInt())
            else tt.load(long)
        } else {
            val string = statement.simpleQueryForString()
            if (tt is DataType.Nullable<*, *>) {
                string?.let((tt.actualType as DataType.NotNull.Simple<*>)::load)
            } else {
                (tt as DataType.NotNull.Simple).load(string)
            }
        }
    } as T

    final override fun select(query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>, expectedCols: Int): Cursor =
        connection.rawQueryWithFactory(
            CurFac<Nothing, Nothing>(null, null, argumentTypes, sessionAndArguments),
            query,
            null, null, null
        ).also {
            if (it.columnCount != expectedCols) {
                val cols = it.columnNames.contentToString()
                it.close()
                throw IllegalArgumentException("Expected $expectedCols cols, got $cols")
            }
        }

    private inline fun <R> statement(query: String, block: (SQLiteStatement) -> R): R {
        val stmt = acquireStmt(query)
        try {
            return block(stmt)
        } finally {
            freeStmt(query, stmt)
        }
    }
    private fun acquireStmt(query: String): SQLiteStatement =
        statements.remove(query) ?: connection.compileStatement(query)
    private fun freeStmt(query: String, stmt: SQLiteStatement) {
        statements.put(query, stmt)?.close()
    }

    override fun <ID> execute(
        query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>, retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any? = statement(query) { statement ->
        bindParams(argumentTypes, statement, transactionAndArguments)

        if (retKeyType == null) statement.executeUpdateDelete()
        else statement.executeInsert().coercePk(retKeyType)
    }
    private fun <T> Long.coercePk(type: Ilk<T, DataType.NotNull.Simple<T>>): T {
        check(this != -1L)
        return type.type.let {
            it.load(when (it.kind) {
                DataType.NotNull.Simple.Kind.Bool -> throw IllegalArgumentException() // O RLY?! Boolean primary key?..
                DataType.NotNull.Simple.Kind.I32 -> chkIn(Int.MIN_VALUE, Int.MAX_VALUE, Int::class.java).toInt()
                DataType.NotNull.Simple.Kind.I64 -> this
                DataType.NotNull.Simple.Kind.F32 -> throw IllegalArgumentException() // O RLY?! Floating primary key?..
                DataType.NotNull.Simple.Kind.F64 -> throw IllegalArgumentException()
                DataType.NotNull.Simple.Kind.Str -> toString()
                DataType.NotNull.Simple.Kind.Blob -> throw IllegalArgumentException() // Possible but unclear what do you want
                else -> throw AssertionError()
            })
        }
    }
    private fun Long.chkIn(min: Int, max: Int, klass: Class<*>): Long {
        check(this in min..max) { "value $this cannot be fit into ${klass.simpleName}" }
        return this
    }

    // Exchange

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(
        table: Table<SCH, ID>, data: PartialStruct<SCH>
    ): ID {
        val sql = with(SqliteDialect) { StringBuilder().insert(table, data.fields).toString() }
        return statement(sql) { statement ->
            bindInsertionParams(table, data) { type, idx, value ->
                (type.type as DataType<Any?>).bind(statement, idx, value)
            }
            statement.executeInsert().coercePk(table.idColType)
        }
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>) {
        val fields = table.pkField?.let { patch.fields - it } ?: patch.fields
        val sql = with(SqliteDialect) { StringBuilder().update(table, fields).toString() }
        statement(sql) { statement ->
            val count = bindInsertionParams(table, patch) { type, idx, value ->
                (type.type as DataType<Any?>).bind(statement, idx, value)
            }
            table.idColType.type.bind(statement, count, id)
            statement.executeUpdateDelete()
        }
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID) {
        val sql = SqliteDialect.deleteRecordQuery(table)
        statement(sql) { statement ->
            table.idColType.type.bind(statement, 0, id)
            check(statement.executeUpdateDelete() == 1)
        }
    }

}

/**
 * Represents a connection with an [SQLiteDatabase].
 */
@ExperimentalSql
class SqliteSession(
        connection: SQLiteDatabase
) : SqliteExchange(connection, ConcurrentHashMap()), Session<Cursor> {

    init {
        // https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/room/runtime/src/main/java/androidx/room/InvalidationTracker.java#176
//        connection.execSQL("PRAGMA temp_store=MEMORY;") // override default settings and store changes for triggers in memory
//        connection.execSQL("PRAGMA recursive_triggers=1;") // fire triggers if data changed from user-defined triggers

        // https://www.sqlite.org/pragma.html
//        connection.execSQL("PRAGMA foreign_keys=1;") // enable foreign key enforcement: let's fail fast
//        connection.execSQL("PRAGMA journal_mode=WAL;") // progressive & concurrent, yay!
//        connection.execSQL("PRAGMA trusted_schema=0;") // “There are advantages to turning it off (…), all applications are encouraged to switch this setting off”
    }

    // FreeSource

    override fun <ID> execute(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>,
        retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any? =
        super.execute(query, argumentTypes, transactionAndArguments, retKeyType)
            .also { deliverTriggeredChanges() }

    // Exchange

    override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID =
        super.insert(table, data)
            .also { deliverTriggeredChanges() }

    override fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<PartialStruct<SCH>>) {
        mutate {
            for (struct in data)
                insert(table, struct)
        }
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>): Unit =
        super.update(table, id, patch)
            .also { deliverTriggeredChanges() }

    override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID): Unit =
        super.delete(table, id)
            .also { deliverTriggeredChanges() }

    // Session

    override fun read(): ReadableTransaction<Cursor> {
        connection.beginTransactionNonExclusive()
        return SqliteTransaction()
    }

    override fun mutate(): MutableTransaction<Cursor> {
        connection.beginTransaction()
        return SqliteTransaction()
    }

    private val triggers = Triggerz()
    override fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable =
        triggers.addListener({
            connection.beginTransaction()
            SqliteTransaction()
        }, subject, listener)

    override fun trimMemory() {
        statements.keys.forEach { sql ->
            // slow but concurrently safe
            statements.remove(sql)?.close()
        }

        connection.execSQL(SqliteDialect.trimMemory())
    }

    override fun close() {
        connection.close()
        // At this point, all statements are already invalid.
        // Waste some cycles to help GC free up useless SQL queries and statements if we dangle.
        statements.clear()
    }

    // misc

    override fun toString(): String =
        "SqliteSession(connection=$connection)"

    private fun deliverTriggeredChanges() {
        val activeSubjects = triggers.activeSubjects()
        if (activeSubjects.isEmpty()) return
        val tableToChanges = HashMap<Table<*, *>, ListChanges<*, *>>()
        connection.beginTransaction()
        try {
            val sb = StringBuilder()
            activeSubjects.forEach { table ->
                sb.setLength(0)
                val cursor = connection.rawQuery(
                    sb.append("SELECT * FROM").append(' ').appendName(table.name, "_lychee_changes").toString(),
                    null
                )

                val wordCount = wordCountForCols(table.managedColumns.size)
                val inserted = HashSet<IdBound>()
                val updated = HashMap<IdBound, Int>()
                val removed = HashSet<IdBound>()
                // fixme: there will be some zeroes for inserted and removed columns, should be more compact
                val changes = LongArray(cursor.count * wordCount)

                val pkType = table.idColType.type
                while (cursor.moveToNext()) {
                    val pk = pkType.get(cursor, 0)
                    when (val what = cursor.getInt(1)) {
                        -1 -> removed.add(pk)
                        0 -> {
                            val position = cursor.position
                            val offset = position * wordCount
                            repeat(wordCount) { word -> changes[offset + word] = cursor.getLong(2 + word) }
                            check(updated.put(pk, position) == null)
                        }
                        1 -> inserted.add(pk)
                        else -> error(what.toString())
                    }
                }
                if (inserted.isNotEmpty() || updated.isNotEmpty() || removed.isNotEmpty() || changes.isNotEmpty())
                    check(tableToChanges.put(
                        table,
                        ListChanges(inserted, updated, removed, table as Table<NullSchema, IdBound>, changes)
                    ) == null)

                sb.setLength(0)
                connection.execSQL(
                    sb.append("DELETE FROM").append(' ').appendName(table.name, "_lychee_changes").toString()
                )
            }

            triggers.enqueue(TriggerReport(tableToChanges))
            connection.setTransactionSuccessful()
        } finally {
            connection.endTransaction()
        }

        triggers.notifyPending()
    }

    private inner class SqliteTransaction : SqliteExchange(connection, statements), InternalTransaction<Cursor> {

        private var thread: Thread? = Thread.currentThread() // null means that this transaction has ended
        private var isSuccessful = false

        // Transaction

        override fun setSuccessful() {
            checkOpenAndThread()
            isSuccessful = true
        }

        override fun close() {
            close(true)
        }
        override fun close(deliver: Boolean) {
            checkOpenAndThread()
            val successful = isSuccessful
            if (successful) {
                connection.setTransactionSuccessful()
            }
            connection.endTransaction()

            // new SQL-friendly API
            if (successful && deliver) {
                deliverTriggeredChanges()
            }
            thread = null
        }

        // InternalTransaction

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
        ): T {
            checkOpenAndThread() // FIXME vvvvvvvvvvvvvvvv allocation
            return select<SCH, ID>(table, arrayOf(colName), id).fetchSingle(colType.type as DataType<T>)
        }

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
        ): Array<Any?> {
            checkOpenAndThread()
            return select<SCH, ID>(table, columnNames, id).fetchColumns(columnTypes)
        }

        override fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            newbies.forEach { (table, events) ->
                val wordCount = wordCountForCols(table.managedColumns.size)
                val typeNames = arrayOfNulls<SqlTypeName>(wordCount + 1)
                typeNames[0] = i32
                typeNames.fill(i64, 1)
                connection.execSQL(sb.createTable(
                    temporary = true, ifNotExists = true,
                    name = table.name, namePostfix = "_lychee_changes",
                    idColName = "id", idColTypeName = table.idColTypeName,
                    managedPk = false /* avoid AUTO_INCREMENT or serial*/,
                    colNames = arrayOf("what") + (0 until wordCount).map { "ch$it" },
                    colTypes = typeNames as Array<out SqlTypeName>
                ).toString())
                events.forEach { event ->
                    sb.setLength(0)
                    connection.execSQL(
                        @Suppress("UPPER_BOUND_VIOLATED")
                        sb.changesTrigger<Schema<*>, IdBound>(
                            "_lychee_changes", event, table as Table<Schema<*>, IdBound>,
                            create = true
                        ).toString()
                    )
                }
            }
        }
        override fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            victims.forEach { (table, events) ->
                connection.execSQL(
                    sb.append("DELETE FROM").append(' ').appendName(table.name, "_lychee_changes")
                        .append(' ').append("WHERE").append(' ')
                        .appendJoining(events, " OR ") { ev -> append("what").append('=').append(ev.balance) }
                        .toString()
                )
                events.forEach { event ->
                    sb.setLength(0)
                    connection.execSQL(
                        @Suppress("UPPER_BOUND_VIOLATED")
                        sb.changesTrigger<Schema<*>, IdBound>(
                            "_lychee_changes", event, table as Table<Schema<*>, IdBound>,
                            create = false
                        ).toString()
                    )
                }
            }
        }
        
        // etc

        private fun checkOpenAndThread() {
            check(thread === Thread.currentThread()) {
                if (thread === null) "this transaction was already closed" else "called from wrong thread"
            }
        }

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
            table: Table<SCH, ID>,
            columnNames: Array<out CharSequence>,
            id: ID
        ): Cursor = connection.rawQueryWithFactory(
            CurFac(table, id, null, null),
            SQLiteQueryBuilder.buildQueryString(
                // fixme: building SQL myself could save some allocations
                /*distinct=*/false, table.name, columnNames.mapIndexedToArray { _, name -> name.toString() },
                "${table.idColName} = ?", /*groupBy=*/null, /*having=*/null, /*orderBy=*/null, /*limit=*/null,
            ),
            /*selectionArgs=*/null,
            SQLiteDatabase.findEditTable(table.name), // TODO: whether it is necessary?
            /*cancellationSignal=*/null
        )

        private fun <T> Cursor.fetchSingle(type: DataType<T>): T =
            try {
                check(moveToFirst())
                type.get(this, 0)
            } finally {
                close()
            }
        private fun Cursor.fetchColumns(columnTypes: Array<out Ilk<*, *>>): Array<Any?> =
            try {
                check(moveToFirst())
                columnTypes.mapIndexedToArray { index, type -> type.type.get(this, index) }
            } finally {
                close()
            }
    }
}

// a workaround for binding BLOBs, as suggested in https://stackoverflow.com/a/23159664/3050249
private class CurFac<ID : IdBound, SCH : Schema<SCH>>(
    private val table: Table<SCH, ID>?,
    private val pk: ID?,
    private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>?,
    private val sessionAndArguments: Array<out Any>?
) : SQLiteDatabase.CursorFactory {

    override fun newCursor(db: SQLiteDatabase?, masterQuery: SQLiteCursorDriver?, editTable: String?, query: SQLiteQuery): Cursor {
        when {
            pk != null ->
                bindQueryParams(table!!, pk) { type, idx, value ->
                    (type.type as DataType<Any?>).bind(query, idx, value)
                }
            argumentTypes != null -> sessionAndArguments!!.let { args ->
                bindParams(argumentTypes, query, args)
            }
            else ->
                throw AssertionError()
        }

        return SQLiteCursor(masterQuery, editTable, query)
    }
}
private fun bindParams(
    argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
    statement: SQLiteProgram,
    transactionAndArguments: Array<out Any>,
) {
    argumentTypes.forEachIndexed { idx, type ->
        (type as DataType<Any?>).bind(statement, idx, transactionAndArguments[idx + 1])
    }
}
private fun <T> DataType<T>.bind(statement: SQLiteProgram, index: Int, value: T) {
    val i = 1 + index
    flattened { isNullable, simple ->
        if (value == null) {
            castNull<T>(isNullable, simple::toString)
            statement.bindNull(i)
        } else {
            val v = simple.store(value)
            when (simple.kind) {
                DataType.NotNull.Simple.Kind.Bool -> statement.bindLong(i, if (v as Boolean) 1 else 0)
                DataType.NotNull.Simple.Kind.I32,
                DataType.NotNull.Simple.Kind.I64 -> statement.bindLong(i, (v as Number).toLong())
                DataType.NotNull.Simple.Kind.F32,
                DataType.NotNull.Simple.Kind.F64 -> statement.bindDouble(i, (v as Number).toDouble())
                DataType.NotNull.Simple.Kind.Str -> statement.bindString(i, v as String)
                DataType.NotNull.Simple.Kind.Blob -> statement.bindBlob(i, v as ByteArray)
            }//.also { }
        }
    }
}

/**
 * Calls [SQLiteDatabase.execSQL] for the given [table] in [this] database.
 */
fun SQLiteDatabase.createTable(table: Table<*, *>) {
    execSQL(SqliteDialect.createTable(table))
}
