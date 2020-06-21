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
/*wannabe internal*/ open class BaseDialect(
    private val types: InlineEnumMap<DataType.NotNull.Simple.Kind, String>,
    private val truncate: String,
    private val arrayPostfix: String?
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
            .appendName(table.idColName).append(' ').let {
                val t = table.idColTypeName
                if (t is DataType<*>) it.appendPkType(t as DataType.NotNull.Simple<*>, managedPk)
                else it.append(t as CharSequence)
            }.append(" PRIMARY KEY")

        val colNames = table.managedColNames
        val colTypes = table.managedColTypeNames

        val startIndex = if (managedPk) 1 else 0
        val endExclusive = colNames.size
        if (endExclusive != startIndex) {
            sb.append(", ")

            // skip
            for (i in startIndex until endExclusive) {
                sb.appendName(colNames[i]).append(' ')
                    .let {
                        val t = colTypes[i]
                        if (t is DataType<*>) it.appendTwN(t)
                        else it.append(t as CharSequence)
                    }

                /* this is useless since we can store only a full struct with all fields filled:
                if (hasDefault) sb.appendDefault(...) */

                    .append(", ")
            }
        }
        sb.setLength(sb.length - 2) // trim last comma; schema.fields must not be empty
        return sb.append(");").toString()
    }
    protected open fun StringBuilder.appendPkType(type: DataType.NotNull.Simple<*>, managed: Boolean): StringBuilder =
        appendTwN(type) // used by SQLite, overridden for Postgres

    private fun <T> StringBuilder.appendDefault(type: DataType<T>, default: T) {
        val type = if (type is DataType.Nullable<*, *>) {
            val act = type.actualType
            if (default == null) append("NULL").also { return }
            act as DataType<T/*!!*/>
        } else type

        when (type) {
            is DataType.Nullable<*, *> -> throw AssertionError()
            is DataType.NotNull.Simple<T> -> type.store(default).let { v ->
                when (type.kind) {
                    DataType.NotNull.Simple.Kind.Bool -> append(if (v as Boolean) '1' else '0')
                    DataType.NotNull.Simple.Kind.I32,
                    DataType.NotNull.Simple.Kind.I64,
                    DataType.NotNull.Simple.Kind.F32,
                    DataType.NotNull.Simple.Kind.F64 -> append('\'').append(v.toString()).append('\'')
                    DataType.NotNull.Simple.Kind.Str -> append('\'').append(v as String).append('\'')
                    DataType.NotNull.Simple.Kind.Blob -> append("x'").append(TODO("append HEX") as String).append('\'')
                }//.also { }
                Unit
            }
            is DataType.NotNull.Collect<*, *, *> -> append("x'").append(TODO("append HEX") as String).append('\'')
            is DataType.NotNull.Partial<*, *> -> throw UnsupportedOperationException()
        }
    }

    /** Appends type along with its non-nullability */
    protected fun StringBuilder.appendTwN(dataType: DataType<*>): StringBuilder {
        val nn = dataType is DataType.NotNull<*>
        return appendTnN(if (nn) dataType as DataType.NotNull else (dataType as DataType.Nullable<*, *>).actualType)
            .appendIf(nn, ' ', "NOT NULL")
    }

    /** Appends type without its nullability info, i. e. like it is nullable. */
    private fun StringBuilder.appendTnN(dataType: DataType.NotNull<*>): StringBuilder = when (dataType) {
        is DataType.NotNull.Simple<*> -> append(nameOf(dataType.kind))
        is DataType.NotNull.Collect<*, *, *> -> appendTArray(dataType.elementType)
        is DataType.NotNull.Partial<*, *> -> throw UnsupportedOperationException() // column can't be of Partial type at this point
    }
    private fun StringBuilder.appendTArray(elementType: DataType<*>): StringBuilder =
        foldArrayType(arrayPostfix != null, elementType,
            { _, elT -> appendTnN(elT).append(arrayPostfix) },
            //^ all array elements are nullable in Postgres, there's nothing we can do about it.
            // Are there any databases which work another way?
            { append(nameOf(DataType.NotNull.Simple.Kind.Blob)) }
        )


    override fun truncate(table: Table<*, *>): String =
            buildString(13 + table.name.length) {
                append(truncate).append(' ').appendName(table.name)
            }

    override val hasArraySupport: Boolean
        get() = arrayPostfix != null

    override fun nameOf(kind: DataType.NotNull.Simple.Kind): String =
        types[kind]!!

}
