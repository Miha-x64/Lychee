package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Named
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.approxType
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import net.aquadc.properties.sql.NoOrder
import net.aquadc.properties.sql.Order
import net.aquadc.properties.sql.PkLens
import net.aquadc.properties.sql.Table
import net.aquadc.properties.sql.WhereCondition
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendHex
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <SCH : Schema<SCH>> insert(table: Table<SCH, *, *>): String = buildString {
        val cols = table.columnsMappedToFields
        append("INSERT INTO ").appendName(table.name).append(" (")
                .appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size)
                .append(");")
    }

    override fun <SCH : Schema<SCH>> selectQuery(
            table: Table<SCH, *, *>, columns: Array<out StoredNamedLens<SCH, *, *>>,
            condition: WhereCondition<SCH>, order: Array<out Order<out SCH>>
    ): String =
            selectQueryInternal(table, columns, condition, order)

    override fun <SCH : Schema<SCH>> selectCountQuery(
            table: Table<SCH, *, *>, condition: WhereCondition<SCH>
    ): String =
            selectQueryInternal(table, null, condition, NoOrder)

    private fun <SCH : Schema<SCH>> selectQueryInternal(
            table: Table<SCH, *, *>, columns: Array<out StoredNamedLens<SCH, *, *>>?,
            condition: WhereCondition<SCH>, order: Array<out Order<out SCH>>
    ): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columns == null) it.append("COUNT(*)") else it.appendNames(columns) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")
        sb.appendWhereClause(table, condition)

        if (order.isNotEmpty())
            sb.append(" ORDER BY ").appendOrderClause(order)

        return sb.toString()
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(
            context: Table<SCH, *, *>,
            condition: WhereCondition<SCH>
    ): StringBuilder = apply {
        val afterWhere = length
        condition.appendSqlTo(context, this@SqliteDialect, this)

        if (length == afterWhere) append('1') // no condition: SELECT "whatever" FROM "somewhere" WHERE 1
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(
            order: Array<out Order<out SCH>>
    ): StringBuilder = apply {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
    }

    override fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *, *>, cols: Array<out StoredNamedLens<SCH, *, *>>): String =
            buildString {
                append("UPDATE ").appendName(table.name).append(" SET ")

                cols.forEach { col ->
                    appendName(col.name).append(" = ?, ")
                }
                setLength(length - 2) // assume not empty

                append(" WHERE ").appendName(table.idColName).append(" = ?;")
            }

    override fun deleteRecordQuery(table: Table<*, *, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: String): StringBuilder =
            append('"').append(name.replace("\"", "\"\"")).append('"')

    private fun StringBuilder.appendNames(cols: Array<out Named>): StringBuilder = apply {
        if (cols.isNotEmpty()) {
            cols.forEach { col ->
                appendName(col.name).append(", ")
            }
            setLength(length - 2) // trim comma
        }
    }

    override fun createTable(table: Table<*, *, *>): String {
        val sb = StringBuilder("CREATE TABLE ").append(table.name).append(" (")
        table.columns.forEach { col ->
            sb.appendName(col.name).append(' ').appendNameOf(col.type)
            if (col is PkLens<*, *> || col === table.pkField)
                sb.append(" PRIMARY KEY")

            /* this is useless since we can store only a full struct with all fields filled:
            if (col.hasDefault) sb.appendDefault(col) */

            sb.append(", ")
        }
        sb.setLength(sb.length - 2) // trim last comma; schema.fields must not be empty
        return sb.append(");").toString()
    }

    private fun <T> StringBuilder.appendDefault(col: FieldDef<*, T, *>) {
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

            override fun <E> StringBuilder.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>) {
                if (nullable && arg === null) append("NULL")
                else append("x'").appendHex(ByteArrayOutputStream().also { type.write(DataStreams, DataOutputStream(it), arg) }.toByteArray()).append('\'')
            }

            override fun <SCH : Schema<SCH>> StringBuilder.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
                error("unsupported²")
            }
        }.match(col.approxType, this, col.default)
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

            override fun <E> StringBuilder.collection(arg: Nothing?, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>) {
                append("BLOB")
                if (!nullable) append(" NOT NULL")
            }

            override fun <SCH : Schema<SCH>> StringBuilder.partial(arg: Nothing?, nullable: Boolean, type: DataType.Partial<T, SCH>) {
                throw UnsupportedOperationException() // column can't be of Partial type at this point
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
