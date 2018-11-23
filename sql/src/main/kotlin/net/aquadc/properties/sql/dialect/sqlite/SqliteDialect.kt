package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <SCH : Schema<SCH>> insertQuery(table: Table<SCH, *, *>, cols: Array<FieldDef<SCH, *>>): String =
            StringBuilder("INSERT INTO ").appendName(table.name)
                    .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
                    .toString()

    override fun <SCH : Schema<SCH>> selectFieldQuery(
            columnName: String, table: Table<SCH, *, *>, condition: WhereCondition<out SCH>, order: Array<out Order<out SCH>>
    ): String =
            selectQuery(columnName, table, condition, order)

    override fun <SCH : Schema<SCH>> selectCountQuery(
            table: Table<SCH, *, *>, condition: WhereCondition<out SCH>
    ): String =
            selectQuery(null, table, condition, NoOrder)

    private fun <SCH : Schema<SCH>> selectQuery(
            columnName: String?, table: Table<SCH, *, *>, condition: WhereCondition<out SCH>, order: Array<out Order<out SCH>>
    ): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columnName == null) it.append("COUNT(*)") else it.appendName(columnName) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")
        sb.appendWhereClause(condition)

        if (!order.isEmpty())
            sb.append(" ORDER BY ").appendOrderClause(order)

        return sb.toString()
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(
            condition: WhereCondition<out SCH>
    ): StringBuilder = apply {
        val afterWhere = length
        condition.appendSqlTo(this@SqliteDialect, this)

        if (length == afterWhere) append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(
            order: Array<out Order<out SCH>>
    ): StringBuilder = apply {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
    }

    override fun <SCH : Schema<SCH>> updateFieldQuery(table: Table<SCH, *, *>, col: FieldDef<SCH, *>): String =
            StringBuilder("UPDATE ").appendName(table.name)
                    .append(" SET ").appendName(col.name).append(" = ? WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun deleteRecordQuery(table: Table<*, *, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: String): StringBuilder =
            append('"').append(name.replace("\"", "\"\"")).append('"')

    private fun <SCH : Schema<SCH>> StringBuilder.appendNames(cols: Array<FieldDef<SCH, *>>): StringBuilder = apply {
        if (cols.isNotEmpty()) {
            cols.forEach { col ->
                appendName(col.name).append(", ")
            }
            setLength(length - 2) // trim comma
        }
    }

    override fun createTable(table: Table<*, *, *>): String {
        val sb = StringBuilder("CREATE TABLE ").append(table.name).append(" (")
                .append(table.idColName).append(' ').append(nameOf(table.idColType)).append(" PRIMARY KEY, ")
        table.schema.fields.forEach { col ->
            val type = col.type
            sb.append(col.name).append(' ').append(nameOf(type))
            if (!type.isNullable) sb.append(" NOT NULL")
            if (col.hasDefault) sb.appendDefault(col)
            sb.append(", ")
        }
        sb.setLength(sb.length - 2) // trim last comma
        return sb.append(");").toString()
    }

    private fun <T> StringBuilder.appendDefault(col: FieldDef<*, T>) {
        val type = col.type
        val value = col.default
        append(" DEFAULT ")
        when (type) {
            is DataType.Simple<T> -> {
                val v = type.encode(value)
                when (type.kind) {
                    DataType.Simple.Kind.Bool -> append(if (v as Boolean) '1' else '0')
                    DataType.Simple.Kind.I8,
                    DataType.Simple.Kind.I16,
                    DataType.Simple.Kind.I32,
                    DataType.Simple.Kind.I64,
                    DataType.Simple.Kind.F32,
                    DataType.Simple.Kind.F64 -> append('\'').append(v.toString()).append('\'')
                    DataType.Simple.Kind.Str -> append('\'').append(v as String).append('\'')
                    DataType.Simple.Kind.Blob -> append("x'").appendHex(v as ByteArray).append('\'')
                }
            }
        }.also { }
    }

    private val hexChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    private fun StringBuilder.appendHex(bytes: ByteArray): StringBuilder {
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append(hexChars[v ushr 4])
            append(hexChars[v and 0x0F])
        }
        return this
    }

    private fun nameOf(dataType: DataType<*>): String = when (dataType) {
        is DataType.Simple<*> -> {
            when (dataType.kind) {
                DataType.Simple.Kind.Bool,
                DataType.Simple.Kind.I8,
                DataType.Simple.Kind.I16,
                DataType.Simple.Kind.I32,
                DataType.Simple.Kind.I64 -> "INTEGER"
                DataType.Simple.Kind.F32,
                DataType.Simple.Kind.F64 -> "REAL"
                DataType.Simple.Kind.Str -> "TEXT"
                DataType.Simple.Kind.Blob -> "BLOB"
            }
        }
    }

}
