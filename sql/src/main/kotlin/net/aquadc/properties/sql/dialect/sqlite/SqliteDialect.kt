package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <REC : Record<REC, *>> insertQuery(table: Table<REC, *>, cols: Array<Col<REC, *>>): String =
            StringBuilder("INSERT INTO ").appendName(table.name)
                    .append(" (").appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size).append(");")
                    .toString()

    override fun <REC : Record<REC, *>> selectFieldQuery(column: Col<REC, *>, table: Table<REC, *>, condition: WhereCondition<out REC>): String =
            selectQuery(column, table, condition)

    override fun <REC : Record<REC, *>> selectCountQuery(table: Table<REC, *>, condition: WhereCondition<out REC>): String =
            selectQuery(null, table, condition)

    private fun <REC : Record<REC, *>> selectQuery(column: Col<REC, *>?, table: Table<REC, *>, condition: WhereCondition<out REC>): String {
        val sb = StringBuilder("SELECT ")
                .let { if (column == null) it.append("COUNT(*)") else it.appendName(column.name) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")

        val afterWhere = sb.length
        condition.appendSqlTo(this, sb)

        if (sb.length == afterWhere) sb.append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1

        return sb.toString()
    }

    override fun <REC : Record<REC, *>> updateFieldQuery(table: Table<REC, *>, col: Col<REC, *>): String =
            StringBuilder("UPDATE ").appendName(table.name)
                    .append(" SET ").appendName(col.name).append(" = ? WHERE ").appendName(table.idCol.name).append(" = ?;")
                    .toString()

    override fun <REC : Record<REC, *>> deleteRecordQuery(table: Table<REC, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idCol.name).append(" = ?;")
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

}
