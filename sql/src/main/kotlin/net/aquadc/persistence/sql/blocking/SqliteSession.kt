@file:JvmName("SqliteUtils")
package net.aquadc.persistence.sql.blocking

import android.database.Cursor
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteCursorDriver
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteProgram
import android.database.sqlite.SQLiteQuery
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteStatement
import net.aquadc.collections.InlineEnumSet
import net.aquadc.collections.forEach
import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.array
import net.aquadc.persistence.eq
import net.aquadc.persistence.newMap
import net.aquadc.persistence.sql.ExperimentalSql
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.FuncN
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
import net.aquadc.persistence.sql.bindValues
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.appendName
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.changesTrigger
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect.createTable
import net.aquadc.persistence.sql.flattened
import net.aquadc.persistence.sql.mapIndexedToArray
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import org.intellij.lang.annotations.Language
import java.io.Closeable
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet

/**
 * Represents a connection with an [SQLiteDatabase].
 */
// TODO: use simpleQueryForLong and simpleQueryForString with compiled statements where possible
@ExperimentalSql
class SqliteSession(
        @JvmSynthetic @JvmField internal val connection: SQLiteDatabase
) : Session<Blocking<Cursor>> {

    @JvmSynthetic internal val lock = ReentrantReadWriteLock()

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    @JvmSynthetic @JvmField internal var transaction: RealTransaction? = null

    @JvmSynthetic @JvmField internal val lowLevel = object : LowLevelSession<SQLiteStatement, Cursor>() {

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: Struct<SCH>): ID {
            val sql = SqliteDialect.insert(table)
            val statement = statements.getOrSet(::newMap).getOrPut(sql) { connection.compileStatement(sql) }
            bindInsertionParams(table, data) { type, idx, value ->
                (type.type as DataType<Any?>).bind(statement, idx, value)
            }
            val id = statement.executeInsert()
            check(id != -1L)
            return table.idColType.type.let {
                it.load(when (it.kind) {
                    DataType.NotNull.Simple.Kind.Bool -> throw IllegalArgumentException() // O RLY?! Boolean primary key?..
                    DataType.NotNull.Simple.Kind.I32 -> id.chkIn(Int.MIN_VALUE, Int.MAX_VALUE, Int::class.java).toInt()
                    DataType.NotNull.Simple.Kind.I64 -> id
                    DataType.NotNull.Simple.Kind.F32 -> throw IllegalArgumentException() // O RLY?! Floating primary key?..
                    DataType.NotNull.Simple.Kind.F64 -> throw IllegalArgumentException()
                    DataType.NotNull.Simple.Kind.Str -> id.toString()
                    DataType.NotNull.Simple.Kind.Blob -> throw IllegalArgumentException() // Possible but unclear what do you want
                    else -> throw AssertionError()
                })
            }
        }
        private fun Long.chkIn(min: Int, max: Int, klass: Class<*>): Long {
            check(this in min..max) { "value $this cannot be fit into ${klass.simpleName}" }
            return this
        }

        override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, primaryKey: ID) {
            val sql = SqliteDialect.deleteRecordQuery(table)
            val statement = statements.getOrSet(::newMap).getOrPut(sql) { connection.compileStatement(sql) }
            table.idColType.type.bind(statement, 0, primaryKey)
            check(statement.executeUpdateDelete() == 1)
        }

        override fun truncate(table: Table<*, *>) {
            connection.execSQL(SqliteDialect.truncate(table))
        }

        override fun onTransactionEnd(successful: Boolean) {
            val transaction = transaction ?: throw AssertionError()
            try {
                if (successful) {
                    connection.setTransactionSuccessful()
                }
                connection.endTransaction()
                this@SqliteSession.transaction = null
            } finally {
                lock.writeLock().unlock()
            }

            // new SQL-friendly API
            if (successful) {
                deliverTriggeredChanges()
            }
        }

        // copy-paste, keep in sync with JDBC session
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

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
                table: Table<SCH, ID>,
                columnNames: Array<out CharSequence>,
                id: ID
        ): Cursor = connection.rawQueryWithFactory(
                CurFac(table, id, null, null),
                SQLiteQueryBuilder.buildQueryString(
                        // fixme: building SQL myself could save some allocations
                        /*distinct=*/false,
                        table.name,
                        columnNames.mapIndexedToArray { _, name -> name.toString() },
                        "${table.idColName} = ?",
                        /*groupBy=*/null,
                        /*having=*/null,
                        /*orderBy=*/null,
                        /*limit=*/null
                ),
                /*selectionArgs=*/null,
                SQLiteDatabase.findEditTable(table.name), // TODO: whether it is necessary?
                /*cancellationSignal=*/null
        )

        // a workaround for binding BLOBs, as suggested in https://stackoverflow.com/a/23159664/3050249
        private inner class CurFac<ID : IdBound, SCH : Schema<SCH>>(
            private val table: Table<SCH, ID>?,
            private val pk: ID?,
            private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>?,
            private val arguments: Array<out Any>?
        ) : SQLiteDatabase.CursorFactory {

            override fun newCursor(db: SQLiteDatabase?, masterQuery: SQLiteCursorDriver?, editTable: String?, query: SQLiteQuery): Cursor {
                when {
                    pk != null ->
                        bindQueryParams(table!!, pk) { type, idx, value ->
                            (type.type as DataType<Any?>).bind(query, idx, value)
                        }
                    argumentTypes != null -> arguments!!.let { args ->
                        argumentTypes.forEachIndexed { idx, type ->
                            (type as DataType<Any?>).bind(query, idx, args[idx])
                        }
                    }
                    else ->
                        throw AssertionError()
                }

                return SQLiteCursor(masterQuery, editTable, query)
            }
        }

        override fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
        ): T =
            select<SCH, ID>(table, arrayOf(colName) /* fixme allocation */, id)
                .fetchSingle(colType.type as DataType<T>)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
        ): Array<Any?> =
                select<SCH, ID>(table, columnNames, id).fetchColumns(columnTypes)

        override val transaction: RealTransaction?
            get() = this@SqliteSession.transaction

        private fun <T> Cursor.fetchAllRows(type: DataType<T>): List<T> {
            if (!moveToFirst()) {
                close()
                return emptyList()
            }

            val values = ArrayList<Any?>() // fixme pre-allocate a fixed array
            do {
                values.add(type.get(this, 0))
            } while (moveToNext())
            close()
            return values as List<T>
        }

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
                    columnTypes.mapIndexedToArray { index, type ->
                        type.type.get(this, index)
                    }
                } finally {
                    close()
                }

        internal fun <T> DataType<T>.bind(statement: SQLiteProgram, index: Int, value: T) {
            val i = 1 + index
            flattened { isNullable, simple ->
                if (value == null) {
                    check(isNullable)
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

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        private fun <T> DataType<T>.get(cursor: Cursor, index: Int): T = flattened { isNullable, simple ->
            if (cursor.isNull(index))
                check(isNullable).let { null as T }
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

        override fun <T> cell(
            query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, arguments: Array<out Any>, type: Ilk<T, *>, orElse: () -> T
        ): T {
            val cur = select(query, argumentTypes, arguments, 1)
            try {
                if (!cur.moveToFirst()) return orElse()
                val value = (type.type as DataType<T>).get(cur, 0)
                check(!cur.moveToNext())
                return value
            } finally {
                cur.close()
            }
        }
        override fun select(query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, arguments: Array<out Any>, expectedCols: Int): Cursor =
                connection.rawQueryWithFactory(
                        CurFac<Nothing, Nothing>(null, null, argumentTypes, arguments),
                        query,
                        null, null, null
                ).also {
                    if (it.columnCount != expectedCols) {
                        val cols = it.columnNames.contentToString()
                        it.close()
                        throw IllegalArgumentException("Expected $expectedCols cols, got $cols")
                    }
                }
        override fun sizeHint(cursor: Cursor): Int = cursor.count
        override fun next(cursor: Cursor): Boolean = cursor.moveToNext()

        override fun execute(
            query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, transactionAndArguments: Array<out Any>
        ) {
            statements
                .getOrSet(::newMap)
                .getOrPut(query) { connection.compileStatement(query) }
                .also { stmt ->
                    argumentTypes.forEachIndexed { idx, type ->
                        (type as DataType<Any?>).bind(stmt, idx, transactionAndArguments[idx + 1])
                    }
                }
                .executeUpdateDelete()
        }

        private fun <T> cellByName(cursor: Cursor, guess: Int, name: CharSequence, type: Ilk<T, *>): T =
            (type.type as DataType<T>).get(cursor, cursor.getColIdx(guess, name))
        override fun <T> cellByName(cursor: Cursor, name: CharSequence, type: Ilk<T, *>): T =
                cellByName(cursor, Integer.MAX_VALUE /* don't even try to guess */, name, type)
        override fun <T> cellAt(cursor: Cursor, col: Int, type: Ilk<T, *>): T =
            (type.type as DataType<T>).get(cursor, col)

        override fun rowByName(cursor: Cursor, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>): Array<Any?> =
                Array(columnNames.size) { idx ->
                    cellByName(cursor, idx, columnNames[idx], columnTypes[idx])
                }
        override fun rowByPosition(cursor: Cursor, offset: Int, types: Array<out Ilk<*, *>>): Array<Any?> =
                Array(types.size) { idx ->
                    types[idx].type.get(cursor, offset + idx)
                }

        // TODO: could subclass SQLiteCursor and attach IntArray<myColIdx, SQLiteColIdx> instead of looking this up every time
        private fun Cursor.getColIdx(guess: Int, name: CharSequence): Int { // native `getColumnIndex` wrecks labels with '.'!
            val columnNames = columnNames!!
            if (columnNames.size > guess && name.eq(columnNames[guess], false)) return guess
            val idx = columnNames.indexOfFirst { name.eq(it, false) }
            if (idx < 0) error { "$name !in ${columnNames.contentToString()}" }
            return idx
        }

        override fun close(cursor: Cursor) =
            cursor.close()

        // copy-paste, keep in sync with JDBC session
        override fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            connection.beginTransaction()
            try {
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
                connection.setTransactionSuccessful()
            } finally {
                connection.endTransaction()
            }
        }
        override fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>) {
            val sb = StringBuilder()
            connection.beginTransaction()
            try {
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
                connection.setTransactionSuccessful()
            } finally {
                connection.endTransaction()
            }
        }
    }


    override fun beginTransaction(): Transaction =
        createTransaction(lock, lowLevel).also {
            connection.beginTransaction()
            transaction = it
        }

    // endregion transactions and modifying statements

    override fun toString(): String =
            "SqliteSession(connection=$connection)"

    @Deprecated("This was intended to list ActiveRecord's queries")
    fun dump(sb: StringBuilder) {
    }

    override fun <R> rawQuery(
        @Language("SQL") query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        fetch: Fetch<Blocking<Cursor>, R>
    ): FuncN<Any, R> =
            BlockingQuery(lowLevel, query, argumentTypes, fetch)

    private val triggers: Triggerz = Triggerz(lowLevel)
    override fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable =
        triggers.addListener(subject, listener)

    override fun trimMemory() {
        connection.execSQL(SqliteDialect.trimMemory())
    }

    override fun close() {
        lowLevel.statements.get()?.values
            ?.forEach(SQLiteStatement::close) // FIXME: Other threads' statements gonna dangle until GC
        connection.close()
    }

}

/**
 * Calls [SQLiteDatabase.execSQL] for the given [table] in [this] database.
 */
fun SQLiteDatabase.createTable(table: Table<*, *>) {
    execSQL(SqliteDialect.createTable(table))
}
