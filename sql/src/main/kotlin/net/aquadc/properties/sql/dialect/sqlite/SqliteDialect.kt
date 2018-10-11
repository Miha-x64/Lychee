package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import net.aquadc.persistence.type.DataType

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <TBL : Table<TBL, *, *>> insertQuery(table: Table<TBL, *, *>, cols: Array<Col<TBL, *>>): String =
            StringBuilder("INSERT INTO ").appendName(table.name)
                    .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
                    .toString()

    override fun <TBL : Table<TBL, *, *>> selectFieldQuery(
            columnName: String, table: Table<TBL, *, *>, condition: WhereCondition<out TBL>, order: Array<out Order<out TBL>>
    ): String =
            selectQuery(columnName, table, condition, order)

    override fun <TBL : Table<TBL, *, *>> selectCountQuery(
            table: Table<TBL, *, *>, condition: WhereCondition<out TBL>
    ): String =
            selectQuery(null, table, condition, NoOrder)

    private fun <TBL : Table<TBL, *, *>> selectQuery(
            columnName: String?, table: Table<TBL, *, *>, condition: WhereCondition<out TBL>, order: Array<out Order<out TBL>>
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

    override fun <TBL : Table<TBL, *, *>> StringBuilder.appendWhereClause(
            condition: WhereCondition<out TBL>
    ): StringBuilder = apply {
        val afterWhere = length
        condition.appendSqlTo(this@SqliteDialect, this)

        if (length == afterWhere) append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1
    }

    override fun <TBL : Table<TBL, *, *>> StringBuilder.appendOrderClause(
            order: Array<out Order<out TBL>>
    ): StringBuilder = apply {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
    }

    override fun <TBL : Table<TBL, *, *>> updateFieldQuery(table: Table<TBL, *, *>, col: Col<TBL, *>): String =
            StringBuilder("UPDATE ").appendName(table.name)
                    .append(" SET ").appendName(col.name).append(" = ? WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun deleteRecordQuery(table: Table<*, *, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: String): StringBuilder =
            append('"').append(name.replace("\"", "\"\"")).append('"')

    private fun <TBL : Table<TBL, *, *>> StringBuilder.appendNames(cols: Array<Col<TBL, *>>): StringBuilder = apply {
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
        table.fields.forEach { col ->
            sb.append(col.name).append(' ').append(nameOf(col.type))
            if (!col.type.isNullable) sb.append(" NOT NULL")
            sb.append(", ")
        }
        sb.setLength(sb.length - 2) // trim last comma
        return sb.append(");").toString()
    }

    private fun nameOf(dataType: DataType<*>): String = when (dataType) {
        is DataType.Integer<*> -> "INTEGER"
        is DataType.Floating<*> -> "REAL"
        is DataType.Str<*> -> "TEXT"
        is DataType.Blob<*> -> "BLOB"
    }

}
