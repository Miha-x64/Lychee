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
import net.aquadc.persistence.array
import net.aquadc.persistence.eq
import net.aquadc.persistence.sql.Dao
import net.aquadc.persistence.sql.ExperimentalSql
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.NoOrder
import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.RealDao
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Record
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.Transaction
import net.aquadc.persistence.sql.VarFunc
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.sql.bindInsertionParams
import net.aquadc.persistence.sql.bindQueryParams
import net.aquadc.persistence.sql.bindValues
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.sql.flattened
import net.aquadc.persistence.sql.mapIndexedToArray
import net.aquadc.persistence.sql.noOrder
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i64
import org.intellij.lang.annotations.Language
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Represents a connection with an [SQLiteDatabase].
 */
// TODO: use simpleQueryForLong and simpleQueryForString with compiled statements where possible
@ExperimentalSql
class SqliteSession(
        @JvmSynthetic @JvmField internal val connection: SQLiteDatabase
) : Session<Blocking<Cursor>> {

    @JvmSynthetic internal val lock = ReentrantReadWriteLock()

    @Suppress("UNCHECKED_CAST")
    override fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(table: Table<SCH, ID, REC>): Dao<SCH, ID, REC> =
            getDao(table) as Dao<SCH, ID, REC>

    @JvmSynthetic internal fun <SCH : Schema<SCH>, ID : IdBound> getDao(table: Table<SCH, ID, *>): RealDao<SCH, ID, *, SQLiteStatement> =
            lowLevel.daos.getOrPut(table) { RealDao(this, lowLevel, table as Table<SCH, ID, Record<SCH, ID>>, SqliteDialect) } as RealDao<SCH, ID, *, SQLiteStatement>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    @JvmSynthetic @JvmField internal var transaction: RealTransaction? = null

    @JvmSynthetic @JvmField internal val lowLevel = object : LowLevelSession<SQLiteStatement, Cursor>() {

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID {
            val dao = getDao(table)
            val statement = dao.insertStatement ?: connection.compileStatement(SqliteDialect.insert(table)).also { dao.insertStatement = it }

            bindInsertionParams(table, data) { type, idx, value ->
                type.bind(statement, idx, value)
            }
            val id = statement.executeInsert()
            check(id != -1L)
            return table.idColType.let {
                it.load(when (it.kind) {
                    DataType.Simple.Kind.Bool -> throw IllegalArgumentException() // O RLY?! Boolean primary key?..
                    DataType.Simple.Kind.I32 -> id.chkIn(Int.MIN_VALUE, Int.MAX_VALUE, Int::class.java).toInt()
                    DataType.Simple.Kind.I64 -> id
                    DataType.Simple.Kind.F32 -> throw IllegalArgumentException() // O RLY?! Floating primary key?..
                    DataType.Simple.Kind.F64 -> throw IllegalArgumentException()
                    DataType.Simple.Kind.Str -> id.toString()
                    DataType.Simple.Kind.Blob -> throw IllegalArgumentException() // Possible but unclear what do you want
                    else -> throw AssertionError()
                })
            }
        }
        private fun Long.chkIn(min: Int, max: Int, klass: Class<*>): Long {
            check(this in min..max) { "value $this cannot be fit into ${klass.simpleName}" }
            return this
        }

        private fun <SCH : Schema<SCH>, ID : IdBound> updateStatementWLocked(table: Table<SCH, ID, *>, cols: Any): SQLiteStatement =
                getDao(table)
                        .updateStatements
                        .getOrPut(cols) {
                            val colArray =
                                    if (cols is Array<*>) cols as Array<out CharSequence>
                                    else arrayOf(cols as CharSequence)
                            connection.compileStatement(SqliteDialect.updateQuery(table, colArray))
                        }

        override fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID, *>, id: ID, columnNames: Any, columnTypes: Any, values: Any?) {
            val statement = updateStatementWLocked(table, columnNames)
            val colCount = bindValues(columnTypes, values) { type, idx, value ->
                type.bind(statement, idx, value)
            }
            table.idColType.bind(statement, colCount, id)
            check(statement.executeUpdateDelete() == 1)
        }

        override fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID, *>, primaryKey: ID) {
            val dao = getDao(table)
            val statement = dao.deleteStatement ?: connection.compileStatement(SqliteDialect.deleteRecordQuery(table)).also { dao.deleteStatement = it }
            table.idColType.bind(statement, 0, primaryKey)
            check(statement.executeUpdateDelete() == 1)
        }

        override fun truncate(table: Table<*, *, *>) {
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

                if (successful) {
                    transaction.deliverChanges()
                }
            } finally {
                lock.writeLock().unlock()
            }
        }

        private fun <SCH : Schema<SCH>, ID : IdBound> select(
                table: Table<SCH, ID, *>,
                columnNames: Array<out CharSequence>?,
                condition: WhereCondition<SCH>,
                order: Array<out Order<SCH>>
        ): Cursor = connection.rawQueryWithFactory(
                CurFac(condition, table, null, null),
                with(SqliteDialect) { SQLiteQueryBuilder.buildQueryString(
                        // fixme: building SQL myself could save some allocations
                        /*distinct=*/false,
                        table.name,
                        if (columnNames == null) arrayOf("COUNT(*)")
                        else columnNames.mapIndexedToArray { _, name -> name.toString() },
                        StringBuilder().appendWhereClause(table, condition).toString(),
                        /*groupBy=*/null,
                        /*having=*/null,
                        if (order.isEmpty()) null else StringBuilder().appendOrderClause(table.schema, order).toString(),
                        /*limit=*/null
                ) },
                /*selectionArgs=*/null,
                SQLiteDatabase.findEditTable(table.name), // TODO: whether it is necessary?
                /*cancellationSignal=*/null
        )

        // a workaround for binding BLOBs, as suggested in https://stackoverflow.com/a/23159664/3050249
        private inner class CurFac<ID : IdBound, SCH : Schema<SCH>>(
                private val condition: WhereCondition<SCH>?,
                private val table: Table<SCH, ID, *>?,
                private val argumentTypes: Array<out DataType.Simple<*>>?,
                private val arguments: Array<out Any>?
        ) : SQLiteDatabase.CursorFactory {

            override fun newCursor(db: SQLiteDatabase?, masterQuery: SQLiteCursorDriver?, editTable: String?, query: SQLiteQuery): Cursor {
                when {
                    condition != null ->
                        bindQueryParams(condition, table!!) { type, idx, value ->
                            type.bind(query, idx, value)
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
                table: Table<SCH, ID, *>, colName: CharSequence, colType: DataType<T>, id: ID
        ): T =
                select<SCH, ID>(table, arrayOf(colName) /* fixme allocation */, pkCond<SCH, ID>(table, id), noOrder())
                        .fetchSingle(colType)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
                table: Table<SCH, ID, *>, condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
        ): Array<ID> =
                select<SCH, ID>(table, arrayOf(table.pkColumn.name(table.schema)) /* fixme allocation */, condition, order)
                        .fetchAllRows(table.idColType)
                        .array<Any>() as Array<ID>

        override fun <SCH : Schema<SCH>, ID : IdBound> fetch(
                table: Table<SCH, ID, *>, columnNames: Array<out CharSequence>, columnTypes: Array<out DataType<*>>, id: ID
        ): Array<Any?> =
                select<SCH, ID>(table, columnNames, pkCond<SCH, ID>(table, id), noOrder()).fetchColumns(columnTypes)

        override fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(table: Table<SCH, ID, *>, condition: WhereCondition<SCH>): Long =
                select<SCH, ID>(table, null, condition, NoOrder as Array<out Order<SCH>>).fetchSingle(i64)

        override val transaction: RealTransaction?
            get() = this@SqliteSession.transaction

        private fun <T> Cursor.fetchAllRows(type: DataType<T>): List<T> {
            if (!moveToFirst()) {
                close()
                return emptyList()
            }

            val values = ArrayList<Any?>()
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

        private fun Cursor.fetchColumns(columnTypes: Array<out DataType<*>>): Array<Any?> =
                try {
                    check(moveToFirst())
                    columnTypes.mapIndexedToArray { index, type ->
                        type.get(this, index)
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
                        DataType.Simple.Kind.Bool -> statement.bindLong(i, if (v as Boolean) 1 else 0)
                        DataType.Simple.Kind.I32,
                        DataType.Simple.Kind.I64 -> statement.bindLong(i, (v as Number).toLong())
                        DataType.Simple.Kind.F32,
                        DataType.Simple.Kind.F64 -> statement.bindDouble(i, (v as Number).toDouble())
                        DataType.Simple.Kind.Str -> statement.bindString(i, v as String)
                        DataType.Simple.Kind.Blob -> statement.bindBlob(i, v as ByteArray)
                    }//.also { }
                }
            }
        }

        @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
        private fun <T> DataType<T>.get(cursor: Cursor, index: Int): T = flattened { isNullable, simple ->
            if (cursor.isNull(index))
                check(isNullable).let { null as T }
            else simple.load(when (simple.kind) {
                DataType.Simple.Kind.Bool -> cursor.getInt(index) == 1
                DataType.Simple.Kind.I32 -> cursor.getInt(index)
                DataType.Simple.Kind.I64 -> cursor.getLong(index)
                DataType.Simple.Kind.F32 -> cursor.getFloat(index)
                DataType.Simple.Kind.F64 -> cursor.getDouble(index)
                DataType.Simple.Kind.Str -> cursor.getString(index)
                DataType.Simple.Kind.Blob -> cursor.getBlob(index)
                else -> throw AssertionError()
            })
        }

        private fun Short.assertFitsByte(): Byte {
            require(this in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                "value $this cannot be fit into ${Byte::class.java.simpleName}"
            }
            return toByte()
        }

        override fun <T> cell(query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>, type: DataType<T>): T {
            val cur = select(query, argumentTypes, arguments, 1)
            try {
                check(cur.moveToFirst())
                val value = type.get(cur, 0)
                check(!cur.moveToNext())
                return value
            } finally {
                cur.close()
            }
        }
        override fun select(query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>, expectedCols: Int): Cursor =
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

        private fun <T> cellByName(cursor: Cursor, guess: Int, name: CharSequence, type: DataType<T>): T =
                type.get(
                        cursor,
                        cursor.getColIdx(guess, name)
                )
        override fun <T> cellByName(cursor: Cursor, name: CharSequence, type: DataType<T>): T =
                cellByName(cursor, Integer.MAX_VALUE /* don't even try to guess */, name, type)
        override fun <T> cellAt(cursor: Cursor, col: Int, type: DataType<T>): T =
                type.get(cursor, col)

        override fun rowByName(cursor: Cursor, columnNames: Array<out CharSequence>, columnTypes: Array<out DataType<*>>): Array<Any?> =
                Array(columnNames.size) { idx ->
                    cellByName(cursor, idx, columnNames[idx], columnTypes[idx])
                }
        override fun rowByPosition(cursor: Cursor, offset: Int, types: Array<out DataType<*>>): Array<Any?> =
                Array(types.size) { idx ->
                    types[idx].get(cursor, offset + idx)
                }

        // TODO: could subclass SQLiteCursor and attach IntArray<myColIdx, SQLiteColIdx> instead of looking this up every time
        private fun Cursor.getColIdx(guess: Int, name: CharSequence): Int { // native `getColumnIndex` wrecks labels with '.'!
            val columnNames = columnNames!!
            if (columnNames.size > guess && name.eq(columnNames[guess], false)) return guess
            val idx = columnNames.indexOfFirst { name.eq(it, false) }
            if (idx < 0) error { "$name !in ${columnNames.contentToString()}" }
            return idx
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


    fun dump(sb: StringBuilder) {
        sb.append("DAOs\n")
        lowLevel.daos.forEach { (table: Table<*, *, *>, dao: Dao<*, *, *>) ->
            sb.append(" ").append(table.name).append("\n")
            dao.dump("  ", sb)

            sb.append("  select statements (for current thread)\n")

            arrayOf(
                    "insert statements" to dao.insertStatement,
                    "update statements" to dao.updateStatements,
                    "delete statements" to dao.deleteStatement
            ).forEach { (text, stmts) ->
                sb.append("  ").append(text).append(": ").append(stmts)
            }
        }
    }

    override fun <R> rawQuery(@Language("SQL") query: String, argumentTypes: Array<out DataType.Simple<*>>, fetch: Fetch<Blocking<Cursor>, R>): VarFunc<Any, R> =
            BlockingQuery(lowLevel, query, argumentTypes, fetch)

}

/**
 * Calls [SQLiteDatabase.execSQL] for the given [table] in [this] database.
 */
fun <SCH : Schema<SCH>> SQLiteDatabase.createTable(table: Table<SCH, *, *>) {
    execSQL(SqliteDialect.createTable(table))
}
