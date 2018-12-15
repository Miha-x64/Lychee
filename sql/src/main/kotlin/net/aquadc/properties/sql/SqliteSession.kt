@file:JvmName("SqliteUtils")
package net.aquadc.properties.sql

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.long
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet

/**
 * Represents a connection with an [SQLiteDatabase].
 */
// TODO: use simpleQueryForLong and simpleQueryForString with compiled statements where possible
class SqliteSession(
        private val connection: SQLiteDatabase
) : Session {

    private val lock = ReentrantReadWriteLock()

    @Suppress("UNCHECKED_CAST")
    override fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(table: Table<SCH, ID, REC>): Dao<SCH, ID, REC> =
            lowLevel.daos.getOrPut(table) {
                check(table.idColType === long)
                RealDao(this, lowLevel, table, SqliteDialect)
            } as Dao<SCH, ID, REC>

    // region transactions and modifying statements

    // transactional things, guarded by write-lock
    private var transaction: RealTransaction? = null
//    private val selectStatements = ThreadLocal<HashMap<String, SQLiteStatement>>()
    private val insertStatements = HashMap<Table<*, *, *>, SQLiteStatement>()
    private val updateStatements = HashMap<Pair<Table<*, *, *>, FieldDef<*, *>>, SQLiteStatement>()
    private val deleteStatements = HashMap<Table<*, *, *>, SQLiteStatement>()

    private val lowLevel = object : LowLevelSession {

        override fun <SCH : Schema<SCH>, ID : IdBound> exists(table: Table<SCH, ID, *>, primaryKey: ID): Boolean {

            val count = select(null, table, reusableCond(table, table.idColName, primaryKey), NoOrder).fetchSingle(long)
            return when (count) {
                0L -> false
                1L -> true
                else -> throw AssertionError()
            }
        }

        private fun <SCH : Schema<SCH>> insertStatementWLocked(table: Table<SCH, *, *>): SQLiteStatement =
                insertStatements.getOrPut(table) {
                    connection.compileStatement(SqliteDialect.insertQuery(table, table.schema.fields))
                }

        override fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID {
            val statement = insertStatementWLocked(table)
            val fields = table.schema.fields
            for (i in fields.indices) {
                val field = fields[i]
                field.type.erased.bind(statement, i, data[field])
            }
            val id = statement.executeInsert()
            check(id != -1L)
            return id as ID
        }

        private fun <SCH : Schema<SCH>> updateStatementWLocked(table: Table<SCH, *, *>, col: FieldDef<SCH, *>): SQLiteStatement =
                updateStatements.getOrPut(Pair(table, col)) {
                    connection.compileStatement(SqliteDialect.updateFieldQuery(table, col))
                }

        override fun <SCH : Schema<SCH>, ID : IdBound, T> update(table: Table<SCH, ID, *>, id: ID, column: FieldDef<SCH, T>, value: T) {
            val statement = updateStatementWLocked(table, column)
            column.type.bind(statement, 0, value)
            table.idColType.bind(statement, 1, id)
            check(statement.executeUpdateDelete() == 1)
        }

        override fun <ID : IdBound> localId(table: Table<*, ID, *>, id: ID): Long = when (id) {
            is Int -> id.toLong()
            is Long -> id
            else -> throw AssertionError()
        }

        override fun <ID : IdBound> primaryKey(table: Table<*, ID, *>, localId: Long): ID =
                localId as ID

        private fun deleteStatementWLocked(table: Table<*, *, *>): SQLiteStatement =
                deleteStatements.getOrPut(table) {
                    connection.compileStatement(SqliteDialect.deleteRecordQuery(table))
                }

        override fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID, *>, primaryKey: ID): Long {
            val statement = deleteStatementWLocked(table)
            table.idColType.bind(statement, 0, primaryKey)
            check(statement.executeUpdateDelete() == 1)
            return localId(table, primaryKey)
        }

        override val daos = ConcurrentHashMap<Table<*, *, *>, RealDao<*, *, *>>()

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

        private fun <ID : IdBound, SCH : Schema<SCH>> select(
                columnName: String?,
                table: Table<SCH, ID, *>,
                condition: WhereCondition<out SCH>,
                order: Array<out Order<out SCH>>
        ): Cursor {
            val args = ArrayList<Pair<String, Any>>() // why not `Any?`? Because you can't treat ` = ?` as `IS NULL`.
            condition.appendValuesTo(args)

            val selectionArgs = args.mapToArray { (name, value) ->
                val conv = // looks like a slow place
                        if (name == table.idColName) table.idColType
                        else table.schema.fields.first { it.name == name }.type
                conv.erased.asString(value)
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

        override fun <ID : IdBound, SCH : Schema<SCH>, T> fetchSingle(
                column: FieldDef<SCH, T>, table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>
        ): T =
                select(column.name, table, condition, NoOrder).fetchSingle(column.type)

        override fun <ID : IdBound, SCH : Schema<SCH>> fetchPrimaryKeys(
                table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>, order: Array<out Order<SCH>>
        ): Array<ID> =
                select(table.idColName, table, condition, order)
                        .fetchAll(table.idColType) // type here is obviously 'long', may seriously optimize this place
                        .toTypedArray<Any>() as Array<ID>

        private fun <T> Cursor.fetchAll(converter: DataType<T>): List<T> {
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

        override fun <ID : IdBound, SCH : Schema<SCH>> fetchCount(table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>): Long =
                select(null, table, condition, NoOrder).fetchSingle(long)

        override val transaction: RealTransaction?
            get() = this@SqliteSession.transaction

        @Suppress("UPPER_BOUND_VIOLATED") private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

        override fun <SCH : Schema<SCH>, T : Any> reusableCond(table: Table<SCH, *, *>, colName: String, value: T): ColCond<SCH, T> {
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
        connection.beginTransaction()
        val tr = RealTransaction(this, lowLevel)
        transaction = tr
        return tr
    }

    // endregion transactions and modifying statements

    override fun toString(): String =
            "SqliteSession(connection=$connection)"


    fun dump(sb: StringBuilder) {
        sb.append("DAOs\n")
        lowLevel.daos.forEach { (table: Table<*, *, *>, dao: Dao<*, *, *>) ->
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

    private fun <T> Cursor.fetchSingle(converter: DataType<T>): T {
        try {
            check(moveToFirst())
            return converter.get(this, 0)
        } finally {
            close()
        }
    }

    private fun <T> DataType<T>.bind(statement: SQLiteStatement, index: Int, value: T) {
        val i = 1 + index
        if (value == null) {
            check(isNullable)
            statement.bindNull(i)
        } else {
            when (this) {
                is DataType.Simple<T> -> {
                    val v = encode(value)
                    when (kind) {
                        DataType.Simple.Kind.Bool -> statement.bindLong(i, if (v as Boolean) 1 else 0)
                        DataType.Simple.Kind.I8,
                        DataType.Simple.Kind.I16,
                        DataType.Simple.Kind.I32,
                        DataType.Simple.Kind.I64 -> statement.bindLong(i, (v as Number).toLong())
                        DataType.Simple.Kind.F32,
                        DataType.Simple.Kind.F64 -> statement.bindDouble(i, (v as Number).toDouble())
                        DataType.Simple.Kind.Str -> statement.bindString(i, v as String)
                        DataType.Simple.Kind.Blob -> statement.bindBlob(i, v as ByteArray)
                    }
                }
            }.also { }
        }
    }

    private fun <T> DataType<T>.asString(value: T): String = when (this) {
        is DataType.Simple<T> -> {
            val v = encode(value!!)
            when (kind) {
                DataType.Simple.Kind.Bool,
                DataType.Simple.Kind.I8,
                DataType.Simple.Kind.I16,
                DataType.Simple.Kind.I32,
                DataType.Simple.Kind.I64,
                DataType.Simple.Kind.F32,
                DataType.Simple.Kind.F64 -> value.toString()
                DataType.Simple.Kind.Str -> value as String
                DataType.Simple.Kind.Blob -> TODO("binding a BLOB as selectionArgs?")
            }
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    private fun <T> DataType<T>.get(cursor: Cursor, index: Int): T {
        return if (cursor.isNull(index))
            check(isNullable).let { null as T }
        else when (this) {
            is DataType.Simple<T> -> {
                when (kind) {
                    DataType.Simple.Kind.Bool -> cursor.getInt(index) == 1
                    DataType.Simple.Kind.I8 -> cursor.getShort(index).assertFitsByte()
                    DataType.Simple.Kind.I16 -> cursor.getShort(index)
                    DataType.Simple.Kind.I32 -> cursor.getInt(index)
                    DataType.Simple.Kind.I64 -> cursor.getLong(index)
                    DataType.Simple.Kind.F32 -> cursor.getFloat(index)
                    DataType.Simple.Kind.F64 -> cursor.getDouble(index)
                    DataType.Simple.Kind.Str -> cursor.getString(index)
                    DataType.Simple.Kind.Blob -> cursor.getBlob(index)
                } as T
            }
        }
    }

    private fun Short.assertFitsByte(): Byte {
        if (this !in Byte.MIN_VALUE..Byte.MAX_VALUE)
            throw IllegalStateException("value ${this} cannot be fit into a byte")
        return toByte()
    }

}

/**
 * Calls [SQLiteDatabase.execSQL] for the given [table] in [this] database.
 */
fun SQLiteDatabase.createTable(table: Table<*, Long, *>) {
    execSQL(SqliteDialect.createTable(table))
}
