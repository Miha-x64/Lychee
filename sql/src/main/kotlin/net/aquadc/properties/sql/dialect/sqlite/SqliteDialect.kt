package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <SCH : Schema<SCH>> replace(table: Table<SCH, *, *>, cols: List<FieldDef<SCH, *>>): String =
            StringBuilder("REPLACE INTO ").appendName(table.name)
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

    override fun <SCH : Schema<SCH>> updateFieldQuery(table: Table<SCH, *, *>, colName: String): String =
            StringBuilder("UPDATE ").appendName(table.name)
                    .append(" SET ").appendName(colName).append(" = ? WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun deleteRecordQuery(table: Table<*, *, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: String): StringBuilder =
            append('"').append(name.replace("\"", "\"\"")).append('"')

    private fun <SCH : Schema<SCH>> StringBuilder.appendNames(cols: List<FieldDef<SCH, *>>): StringBuilder = apply {
        if (cols.isNotEmpty()) {
            for (i in cols.indices) {
                appendName(cols[i].name).append(", ")
            }
            setLength(length - 2) // trim comma
        }
    }

    override fun createTable(table: Table<*, *, *>): String {
        val sb = StringBuilder("CREATE TABLE ").append(table.name).append(" (")
        table.columns.forEach { (col, rel) ->
            val name: String
            val type: DataType<*>
            if (col is Lens<*, *>) {
                name = col.name
                type = col.type
            } else if (col is Pair<*, *>) {
                name = col.first as String
                type = col.second as DataType<*>
            } else {
                throw AssertionError()
            }

            sb.appendName(name).append(' ').appendNameOf(type)
            when (rel) {
                Relation.PrimaryKey -> sb.append(" PRIMARY KEY")
                is Relation.Embedded -> throw AssertionError("unexpected")
                is Relation.ToOne<*, *, *> -> TODO()
                is Relation.ToMany<*, *, *, *> -> TODO()
                is Relation.ManyToMany<*, *, *> -> TODO()
                null -> { /* ok */ }
            }.also { }

            /* this is useless since we can store only a full struct with all fields filled:
            if (col.hasDefault) sb.appendDefault(col) */

            sb.append(", ")
        }
        sb.setLength(sb.length - 2) // trim last comma; schema.fields must not be empty
        return sb.append(");").toString()
    }

    private fun <T> StringBuilder.appendDefault(col: FieldDef<*, T>) {
        object : DataTypeVisitor<StringBuilder, T, T, Unit> {
            override fun StringBuilder.simple(arg: T, nullable: Boolean, type: DataType.Simple<T>) {
                if (nullable && arg === null) append("NULL")
                else {
                    val v = type.store(arg)
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
                    }.also { }
                }
            }

            override fun <E> StringBuilder.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E>) {
                if (nullable && arg === null) append("NULL")
                else append("x'").appendHex(ByteArrayOutputStream().also { type.write(DataStreams, DataOutputStream(it), arg) }.toByteArray()).append('\'')
            }

            override fun <SCH : Schema<SCH>> StringBuilder.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
                error("unsupported²")
            }
        }.match(col.type, this, col.default)
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

    private fun <T> StringBuilder.appendNameOf(dataType: DataType<T>) = apply {
        object : DataTypeVisitor<StringBuilder, Nothing?, T, Unit> {
            override fun StringBuilder.simple(arg: Nothing?, nullable: Boolean, type: DataType.Simple<T>) {
                append(when (type.kind) {
                    DataType.Simple.Kind.Bool,
                    DataType.Simple.Kind.I8,
                    DataType.Simple.Kind.I16,
                    DataType.Simple.Kind.I32,
                    DataType.Simple.Kind.I64 -> "INTEGER"
                    DataType.Simple.Kind.F32,
                    DataType.Simple.Kind.F64 -> "REAL"
                    DataType.Simple.Kind.Str -> "TEXT"
                    DataType.Simple.Kind.Blob -> "BLOB"
                })
                if (!nullable) append(" NOT NULL")
            }

            override fun <E> StringBuilder.collection(arg: Nothing?, nullable: Boolean, type: DataType.Collect<T, E>) {
                append("BLOB")
                if (!nullable) append(" NOT NULL")
            }

            override fun <SCH : Schema<SCH>> StringBuilder.partial(arg: Nothing?, nullable: Boolean, type: DataType.Partial<T, SCH>) {
                TODO("embedded + relations")
            }
        }.match(dataType, this, null)
    }

    /**
     * {@implNote SQLite does not have TRUNCATE statement}
     */
    override fun truncate(table: Table<*, *, *>): String =
            buildString(13 + table.name.length) {
                append("DELETE FROM").appendName(table.name)
            }

}
