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

    override fun <REC : Record<REC, *>> selectFieldQuery(columnName: String, table: Table<REC, *>, condition: WhereCondition<out REC>): String =
            selectQuery(columnName, table, condition)

    override fun <REC : Record<REC, *>> selectCountQuery(table: Table<REC, *>, condition: WhereCondition<out REC>): String =
            selectQuery(null, table, condition)

    private fun <REC : Record<REC, *>> selectQuery(columnName: String?, table: Table<REC, *>, condition: WhereCondition<out REC>): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columnName == null) it.append("COUNT(*)") else it.appendName(columnName) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")

        val afterWhere = sb.length
        condition.appendSqlTo(this, sb)

        if (sb.length == afterWhere) sb.append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1

        return sb.toString()
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

}
