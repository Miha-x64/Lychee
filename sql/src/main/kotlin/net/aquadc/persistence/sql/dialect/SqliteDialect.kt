package net.aquadc.persistence.sql.dialect.sqlite

import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.appendPlaceholders
import net.aquadc.persistence.sql.dialect.appendReplacing
import net.aquadc.persistence.sql.noOrder
import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Implements SQLite [Dialect].
 */
object SqliteDialect : Dialect {

    override fun <SCH : Schema<SCH>> insert(table: Table<SCH, *, *>): String = buildString {
        val cols = table.managedColNames
        append("INSERT INTO ").appendName(table.name).append(" (")
                .appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size)
                .append(");")
    }

    override fun <SCH : Schema<SCH>> selectQuery(
            table: Table<SCH, *, *>, columns: Array<out CharSequence>,
            condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): String =
            selectQueryInternal(table, columns, condition, order)

    override fun <SCH : Schema<SCH>> selectCountQuery(
            table: Table<SCH, *, *>, condition: WhereCondition<SCH>
    ): String =
            selectQueryInternal(table, null, condition, noOrder())

    private fun <SCH : Schema<SCH>> selectQueryInternal(
            table: Table<SCH, *, *>, columns: Array<out CharSequence>?,
            condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columns == null) it.append("COUNT(*)") else it.appendNames(columns) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")
        sb.appendWhereClause(table, condition)

        if (order.isNotEmpty())
            sb.append(" ORDER BY ").appendOrderClause(table.schema, order)

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
            schema: SCH, order: Array<out Order<SCH>>
    ): StringBuilder = apply {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name(schema)).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
    }

    override fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *, *>, cols: Array<out CharSequence>): String =
            buildString {
                append("UPDATE ").appendName(table.name).append(" SET ")

                cols.forEach { col ->
                    appendName(col).append(" = ?, ")
                }
                setLength(length - 2) // assume not empty

                append(" WHERE ").appendName(table.idColName).append(" = ?;")
            }

    override fun deleteRecordQuery(table: Table<*, *, *>): String =
            StringBuilder("DELETE FROM ").appendName(table.name)
                    .append(" WHERE ").appendName(table.idColName).append(" = ?;")
                    .toString()

    override fun StringBuilder.appendName(name: CharSequence): StringBuilder =
            append('"').appendReplacing(name, '"', "\"\"").append('"')

    private fun StringBuilder.appendNames(cols: Array<out CharSequence>): StringBuilder = apply {
        if (cols.isNotEmpty()) {
            cols.forEach { col ->
                appendName(col).append(", ")
            }
            setLength(length - 2) // trim comma
        }
    }

    override fun createTable(table: Table<*, *, *>): String {
        val sb = StringBuilder("CREATE TABLE ").appendName(table.name).append(" (")
                .appendName(table.idColName).append(' ').appendNameOf(table.idColType).append(" PRIMARY KEY")

        val colNames = table.managedColNames
        val colTypes = table.managedColTypes

        val startIndex = if (table.pkField == null) 0 else 1
        val endExclusive = colNames.size
        if (endExclusive != startIndex) {
            sb.append(", ")

            // skip
            for (i in startIndex until endExclusive) {
                sb.appendName(colNames[i]).append(' ').appendNameOf(colTypes[i])

                /* this is useless since we can store only a full struct with all fields filled:
                if (hasDefault) sb.appendDefault(...) */

                .append(", ")
            }
        }
        sb.setLength(sb.length - 2) // trim last comma; schema.fields must not be empty
        return sb.append(");").toString()
    }

    private fun <T> StringBuilder.appendDefault(type: DataType<T>, default: T) {
        object : DataTypeVisitor<StringBuilder, T, T, Unit> {
            override fun StringBuilder.simple(arg: T, nullable: Boolean, type: DataType.Simple<T>) {
                if (nullable && arg === null) append("NULL")
                else {
                    val v = type.store(arg)
                    when (type.kind) {
                        DataType.Simple.Kind.Bool -> append(if (v as Boolean) '1' else '0')
                        DataType.Simple.Kind.I32,
                        DataType.Simple.Kind.I64,
                        DataType.Simple.Kind.F32,
                        DataType.Simple.Kind.F64 -> append('\'').append(v.toString()).append('\'')
                        DataType.Simple.Kind.Str -> append('\'').append(v as String).append('\'')
                        DataType.Simple.Kind.Blob -> append("x'").append(TODO("append HEX") as String).append('\'')
                    }//.also { }
                }
            }

            /**
             * We had out own `StringBuilder.appendHex(ByteArray)` which was removed since it is unused.
             * Take a look at [net.aquadc.persistence.toHexString] if you're gonna resurrect this functionality.
             */
            override fun <E> StringBuilder.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>) {
                if (nullable && arg === null) append("NULL")
                else append("x'").append(
                        TODO("append HEX" + ByteArrayOutputStream().also { type.write(DataStreams, DataOutputStream(it), arg) }.toByteArray()) as String
                ).append('\'')
            }

            override fun <SCH : Schema<SCH>> StringBuilder.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
                error("unsupported²")
            }
        }.match(type, this, default)
    }

    private fun <T> StringBuilder.appendNameOf(dataType: DataType<T>) = apply {
        object : DataTypeVisitor<StringBuilder, Nothing?, T, Unit> {
            override fun StringBuilder.simple(arg: Nothing?, nullable: Boolean, type: DataType.Simple<T>) {
                append(when (type.kind) {
                    DataType.Simple.Kind.Bool,
                    DataType.Simple.Kind.I32,
                    DataType.Simple.Kind.I64 -> "INTEGER"
                    DataType.Simple.Kind.F32,
                    DataType.Simple.Kind.F64 -> "REAL"
                    DataType.Simple.Kind.Str -> "TEXT"
                    DataType.Simple.Kind.Blob -> "BLOB"
                    else -> throw AssertionError()
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