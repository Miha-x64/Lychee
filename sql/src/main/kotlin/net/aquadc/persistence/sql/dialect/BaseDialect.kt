package net.aquadc.persistence.sql.dialect

import androidx.annotation.RestrictTo
import net.aquadc.collections.InlineEnumMap
import net.aquadc.collections.get
import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import net.aquadc.persistence.sql.noOrder
import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

@RestrictTo(RestrictTo.Scope.LIBRARY)
/*internal*/ open class BaseDialect(
    private val types: InlineEnumMap<DataType.Simple.Kind, String>,
    private val truncate: String
) : Dialect {

    override fun <SCH : Schema<SCH>> insert(table: Table<SCH, *>): String = buildString {
        val cols = table.managedColNames
        append("INSERT INTO ").appendName(table.name).append(" (")
                .appendNames(cols).append(") VALUES (").appendPlaceholders(cols.size)
                .append(");")
    }

    override fun <SCH : Schema<SCH>> selectQuery(
            table: Table<SCH, *>, columns: Array<out CharSequence>,
            condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): String =
            selectQueryInternal(table, columns, condition, order)

    override fun <SCH : Schema<SCH>> selectCountQuery(
            table: Table<SCH, *>, condition: WhereCondition<SCH>
    ): String =
            selectQueryInternal(table, null, condition, noOrder())

    private fun <SCH : Schema<SCH>> selectQueryInternal(
            table: Table<SCH, *>, columns: Array<out CharSequence>?,
            condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): String {
        val sb = StringBuilder("SELECT ")
                .let { if (columns == null) it.append("COUNT(*)") else it.appendNames(columns) }
                .append(" FROM ").appendName(table.name)
                .append(" WHERE ")

        val afterWhere = sb.length
        condition.appendSqlTo(table, this@BaseDialect, sb)
        sb.length.let { if (it == afterWhere) sb.setLength(it - 7) } // erase " WHERE "

        if (order.isNotEmpty())
            sb.append(" ORDER BY ").appendOrderClause(table.schema, order)

        return sb.toString()
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(
            context: Table<SCH, *>,
            condition: WhereCondition<SCH>
    ): StringBuilder = apply {
        val afterWhere = length
        condition.appendSqlTo(context, this@BaseDialect, this)

        // no condition: SELECT "whatever" FROM "somewhere" WHERE true
        if (length == afterWhere) append(if (this@BaseDialect === SqliteDialect) "1" else "true")
    }

    override fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(
            schema: SCH, order: Array<out Order<SCH>>
    ): StringBuilder = apply {
        if (order.isEmpty()) throw IllegalArgumentException()
        order.forEach { appendName(it.col.name(schema)).append(if (it.desc) " DESC, " else " ASC, ") }
        setLength(length - 2)
    }

    override fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *>, cols: Array<out CharSequence>): String =
            buildString {
                check(cols.isNotEmpty())

                append("UPDATE ").appendName(table.name).append(" SET ")
                cols.forEach { col -> appendName(col).append(" = ?, ") }
                setLength(length - 2)

                append(" WHERE ").appendName(table.idColName).append(" = ?;")
            }

    override fun deleteRecordQuery(table: Table<*, *>): String =
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

    override fun createTable(table: Table<*, *>, temporary: Boolean): String {
        val managedPk = table.pkField != null
        val sb = StringBuilder("CREATE").append(' ')
            .appendIf(temporary, "TEMP ").append("TABLE").append(' ').appendName(table.name).append(" (")
            .appendName(table.idColName).append(' ').appendPkType(table.idColType, managedPk).append(" PRIMARY KEY")

        val colNames = table.managedColNames
        val colTypes = table.managedColTypes

        val startIndex = if (managedPk) 1 else 0
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
    private inline fun StringBuilder.appendIf(cond: Boolean, what: String): StringBuilder =
        if (cond) append(what) else this
    protected open fun StringBuilder.appendPkType(type: DataType.Simple<*>, managed: Boolean): StringBuilder =
        appendNameOf(type) // used by SQLite, overridden for Postgres

    private fun <T> StringBuilder.appendDefault(type: DataType<T>, default: T) {
        val type = if (type is DataType.Nullable<*, *>) {
            val act = type.actualType
            if (default == null) append("NULL").also { return }
            act as DataType<T/*!!*/>
        } else type

        when (type) {
            is DataType.Nullable<*, *> -> throw AssertionError()
            is DataType.Simple<T> -> type.store(default).let { v ->
                when (type.kind) {
                    DataType.Simple.Kind.Bool -> append(if (v as Boolean) '1' else '0')
                    DataType.Simple.Kind.I32,
                    DataType.Simple.Kind.I64,
                    DataType.Simple.Kind.F32,
                    DataType.Simple.Kind.F64 -> append('\'').append(v.toString()).append('\'')
                    DataType.Simple.Kind.Str -> append('\'').append(v as String).append('\'')
                    DataType.Simple.Kind.Blob -> append("x'").append(TODO("append HEX") as String).append('\'')
                }//.also { }
                Unit
            }
            is DataType.Collect<*, *, *> -> append("x'").append(
                TODO("append HEX" + ByteArrayOutputStream().also { type.write(DataStreams, DataOutputStream(it), default) }.toByteArray()) as String
            ).append('\'')
            is DataType.Partial<*, *> -> throw UnsupportedOperationException()
        }
    }

    protected fun <T> StringBuilder.appendNameOf(dataType: DataType<T>) = apply {
        val act = if (dataType is DataType.Nullable<*, *>) dataType.actualType else dataType
        when (act) {
            is DataType.Nullable<*, *> -> throw AssertionError()
            is DataType.Simple<*> -> append(types[act.kind]!!)
            is DataType.Collect<*, *, *> -> append("BLOB")
            is DataType.Partial<*, *> -> throw UnsupportedOperationException() // column can't be of Partial type at this point
        }
        if (dataType === act) {
            append(" NOT NULL")
        }
    }

    /**
     * {@implNote SQLite does not have TRUNCATE statement}
     */
    override fun truncate(table: Table<*, *>): String =
            buildString(13 + table.name.length) {
                append(truncate).append(' ').appendName(table.name)
            }

}
