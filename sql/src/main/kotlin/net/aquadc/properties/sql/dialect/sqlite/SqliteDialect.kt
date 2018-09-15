package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import net.aquadc.struct.converter.DataType

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <REC : Record<REC, *>> insertQuery(table: Table<REC, *>, cols: Array<Col<REC, *>>): String =
            StringBuilder("INSERT INTO ").appendName(table.name)
                    .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
                    .toString()

    override fun <REC : Record<REC, *>> selectFieldQuery(
            columnName: String, table: Table<REC, *>, condition: WhereCondition<out REC>, order: Array<out Order<out REC>>
    ): String =
            selectQuery(columnName, table, condition, order)

    override fun <REC : Record<REC, *>> selectCountQuery(
            table: Table<REC, *>, condition: WhereCondition<out REC>
    ): String =
            selectQuery(null, table, condition, NoOrder)

    private fun <REC : Record<REC, *>> selectQuery(columnName: String?, table: Table<REC, *>, condition: WhereCondition<out REC>, order: Array<out Order<out REC>>): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columnName == null) it.append("COUNT(*)") else it.appendName(columnName) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")
        sb.appendWhereClause(condition)

        if (!order.isEmpty())
            sb.append(" ORDER BY ").appendOrderClause(order)

        return sb.toString()
    }

    override fun <REC : Record<REC, *>> StringBuilder.appendWhereClause(condition: WhereCondition<out REC>): StringBuilder {
        val afterWhere = length
        condition.appendSqlTo(this@SqliteDialect, this)

        if (length == afterWhere) append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1
        return this
    }

    override fun <REC : Record<REC, *>> StringBuilder.appendOrderClause(order: Array<out Order<out REC>>): StringBuilder {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
        return this
    }

    override fun <REC : Record<REC, *>> updateFieldQuery(table: Table<REC, *>, col: Col<REC, *>): String =
            StringBuilder("UPDATE ").appendName(table.name)
                    .append(" SET ").appendName(col.name).append(" = ? WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun deleteRecordQuery(table: Table<*, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: String): StringBuilder =
            append('"').append(name.replace("\"", "\"\"")).append('"')

    private fun <REC : Record<REC, *>> StringBuilder.appendNames(cols: Array<Col<REC, *>>): StringBuilder {
        if (cols.isEmpty()) return this

        cols.forEach { col ->
            appendName(col.name).append(", ")
        }
        setLength(length - 2) // trim comma

        return this
    }

    override fun nameOf(dataType: DataType): String = when (dataType) {
        is DataType.Integer -> "INTEGER"
        is DataType.Float -> "REAL"
        is DataType.String -> "TEXT"
        is DataType.Blob -> "BLOB"
    }

    override fun createTable(table: Table<*, *>): String {
        val sb = StringBuilder("CREATE TABLE ").append(table.name).append(" (")
                .append(table.idColName).append(' ').append(nameOf(table.idColConverter.dataType)).append(" PRIMARY KEY, ")
        table.fields.forEach { col ->
            sb.append(col.name).append(' ').append(nameOf(col.converter.dataType))
            if (!col.converter.isNullable) sb.append(" NOT NULL")
            sb.append(", ")
        }
        sb.setLength(sb.length - 2) // trim last comma
        return sb.append(");").toString()
    }

}
