package net.aquadc.persistence.sql.dialect

import androidx.annotation.RestrictTo
import net.aquadc.collections.InlineEnumMap
import net.aquadc.collections.get
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.SqlTypeName
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.stream.DataStreams
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

@RestrictTo(RestrictTo.Scope.LIBRARY)
/*wannabe internal*/ abstract class BaseDialect(
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

    override fun <SCH : Schema<SCH>> StringBuilder.selectQuery(
        table: Table<SCH, *>, columns: Array<out CharSequence>
    ): StringBuilder =
        append("SELECT ").appendNames(columns)
            .append(" FROM ").appendName(table.name)

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

    override fun createTable(table: Table<*, *>, temporary: Boolean, ifNotExists: Boolean): String =
        StringBuilder().createTable(
            temporary = temporary, ifNotExists = ifNotExists,
            name = table.name, namePostfix = null,
            idColName = table.idColName, idColTypeName = table.idColTypeName,
            managedPk = table.pkField != null,
            colNames = table.managedColNames, colTypes = table.managedColTypeNames
        ).toString()

    override fun StringBuilder.createTable(
        temporary: Boolean, ifNotExists: Boolean, name: String, namePostfix: String?,
        idColName: CharSequence, idColTypeName: SqlTypeName, managedPk: Boolean,
        colNames: Array<out CharSequence>, colTypes: Array<out SqlTypeName>
    ): StringBuilder {
        append("CREATE").appendIf(temporary, ' ', "TEMP").append(' ').append("TABLE")
            .appendIf(ifNotExists, ' ', "IF NOT EXISTS").append(' ')
            .appendName(name, namePostfix).append(' ').append('(')
            .appendName(idColName).append(' ').let {
                if (idColTypeName is DataType<*>) it.appendPkType(idColTypeName as DataType.NotNull.Simple<*>, managedPk)
                else it.append(idColTypeName as CharSequence)
            }.append(' ').append("PRIMARY KEY")

        val startIndex = if (managedPk) 1 else 0
        val endExclusive = colNames.size
        if (endExclusive != startIndex) {
            append(", ")

            // skip
            for (i in startIndex until endExclusive) {
                appendName(colNames[i]).append(' ')
                    .let {
                        val t = colTypes[i]
                        if (t is DataType<*>) it.appendTwN(t)
                        else it.append(t as CharSequence)
                    }

                /* this is useless since we can store only a full struct with all fields filled:
                if (hasDefault) sb.appendDefault(...) */

                append(", ")
            }
        }
        setLength(length - 2) // trim last comma; schema.fields must not be empty
        return append(");")
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
            is DataType.NotNull.Collect<T, *, *> -> append("x'").append(
                TODO("append HEX" + ByteArrayOutputStream().also { type.write(DataStreams, DataOutputStream(it), default) }.toByteArray()) as String
            ).append('\'')
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

    override fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.prepareChangesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder = this // do nothing for SQLite, overridden in PostgreSQL dialect

    // region utilities, mostly for triggers

    protected inline fun StringBuilder.appendCase(
        predicateExpr: StringBuilder.() -> StringBuilder,
        thenExpr: StringBuilder.() -> StringBuilder,
        elseExpr: StringBuilder.() -> StringBuilder
    ): StringBuilder =
        append("CASE WHEN").append(' ').predicateExpr().append(' ').append("THEN").append(' ').thenExpr().append(' ')
            .append("ELSE").append(' ').elseExpr().append(' ').append("END")

    protected inline fun StringBuilder.appendCoalesce(contents: StringBuilder.() -> StringBuilder): StringBuilder =
        append("COALESCE").append('(').contents().append(')')

    internal fun StringBuilder.appendName(name: CharSequence, postfix: CharSequence?): StringBuilder =
        append('"').appendReplacing(name, '"', "\"\"")
            .also { if (postfix != null) appendReplacing(postfix, '"', "\"\"") }.append('"')
    protected fun StringBuilder.appendName(name: CharSequence, postfix: Int): StringBuilder =
        append('"').appendReplacing(name, '"', "\"\"").append(postfix).append('"')
    protected fun StringBuilder.appendTriggerName(
        onTable: Table<*, *>, afterEvent: TriggerEvent, namePostfix: CharSequence
    ): StringBuilder = // "someTable_INS|UPD|DEL_someSeed"
        append('"').appendReplacing(onTable.name, '"', "\"\"").appendReplacing(namePostfix, '"', "\"\"")
            .append('_').append(afterEvent.name, 0, 3).append('"')
    @Suppress("NOTHING_TO_INLINE")
    protected inline fun StringBuilder.appendQualified(qualifier: CharSequence, name: CharSequence): StringBuilder =
        append(qualifier).append('.').appendName(name)

    // endregion

    override val hasArraySupport: Boolean
        get() = arrayPostfix != null

    override fun nameOf(kind: DataType.NotNull.Simple.Kind): String =
        types[kind]!!

    override fun trimMemory(): String? =
        null

}
