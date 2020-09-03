@file:JvmName("PostgresDialect")
package net.aquadc.persistence.sql.dialect.postgres

import net.aquadc.collections.contains
import net.aquadc.collections.enumMapOf
import net.aquadc.collections.plus
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.sql.dialect.BaseDialect
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.appendIf
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import kotlin.math.min

/**
 * Implements PostgreSQL [Dialect].
 */
@JvmField
val PostgresDialect: Dialect = object : BaseDialect(
    enumMapOf(
        DataType.NotNull.Simple.Kind.Bool, "bool",
        DataType.NotNull.Simple.Kind.I32, "int",
        DataType.NotNull.Simple.Kind.I64, "int8",
        DataType.NotNull.Simple.Kind.F32, "real",
        DataType.NotNull.Simple.Kind.F64, "float8",
        DataType.NotNull.Simple.Kind.Str, "text",
        DataType.NotNull.Simple.Kind.Blob, "bytea"
    ),
    truncate = "TRUNCATE TABLE",
    arrayPostfix = "[]"
) {
    private val serial = DataType.NotNull.Simple.Kind.I32 + DataType.NotNull.Simple.Kind.I64
    override fun StringBuilder.appendPkType(type: DataType.NotNull.Simple<*>, managed: Boolean): StringBuilder =
        // If PK column is 'managed', we just take `structToInsert[pkField]`. todo unique constraint
        if (managed || type.kind !in serial) appendTwN(type)
        // Otherwise its our responsibility to make PK auto-generated
        else append("serial")
            .appendIf(type.kind == DataType.NotNull.Simple.Kind.I64, '8')
            .append(' ')
            .append("NOT NULL")

    override fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.prepareChangesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder {
        append(if (create) "CREATE" else "DROP")
            .append(' ').append("FUNCTION")
            .append(' ').appendTriggerName(onTable, afterEvent, namePostfix.toString() + "_func")
            .append('(').append(')')

        return if (!create) this else append(" RETURNS TRIGGER SET SCHEMA 'public' LANGUAGE plpgsql AS \$\$ BEGIN\n")
            .let { when (afterEvent) {
                TriggerEvent.INSERT -> it.appendStructuralTrg(namePostfix, "NEW", onTable, '+')
                TriggerEvent.UPDATE -> it.appendUpdTrg(namePostfix, onTable)
                TriggerEvent.DELETE -> it.appendStructuralTrg(namePostfix, "OLD", onTable, '-')
            } }
            .append(';')
            .append("RETURN").append(' ').append(if (afterEvent == TriggerEvent.DELETE) "OLD" else "NEW")
            .append(";END;\$\$;")
    }

    private fun StringBuilder.appendStructuralTrg(
        changesTablePostfix: CharSequence, recordReference: String, onTable: Table<*, *>, balance: Char
    ): StringBuilder {
        val wordCount = wordCountForCols(onTable.managedColumns.size)
        return append("INSERT").append(' ').append("INTO").append(' ').appendName(onTable.name, changesTablePostfix)
            .append(' ')
            .append("VALUES").append(' ').append('(')
            .appendQualified(recordReference, onTable.idColName)
            .append(',')
            .append(balance).append('1')
            .also { sb -> repeat(wordCount) { _ ->
                sb.append(',').append('-').append('1')
            } }
            .append(')')
            .append("\nON CONFLICT (id) DO UPDATE SET ")
            .append("what").append('=')
                .appendName(onTable.name, changesTablePostfix).append('.').append("what")
                .append('+')
                .append("EXCLUDED").append('.').append("what")
            .also { sb -> repeat(wordCount) { n ->
                sb.append(',').append("ch").append(n).append('=').append('-').append('1')
            } }
    }

    // insert/update (pk, previous, oldMask | newMask)
    private fun <SCH : Schema<SCH>> StringBuilder.appendUpdTrg(
        changesTablePostfix: CharSequence, onTable: Table<SCH, *>
    ): StringBuilder {
        val cols = onTable.managedColumns
        val wordCount = wordCountForCols(cols.size)

        return append("INSERT").append(' ').append("INTO").append(' ').appendName(onTable.name, changesTablePostfix)
            .append(' ')
            .append("VALUES").append(' ').append('(')
            .appendQualified("NEW"/*whatever*/, onTable.idColName)
            .append(',')
            .append(0)
            .also { sb ->
                repeat(wordCount) { index ->
                    sb.append(',')
                    val offset = index * 64
                    val size = min(64, cols.size - offset)

                    // chN = (if (old.field = new.field) 1 shl index else 0) | (if …)

                    for (i in 0 until size) {
                        val colName = cols[offset + i].name(onTable.schema)
                        sb.append('(').appendCase(
                            { appendQualified("OLD", colName).append('=').appendQualified("NEW", colName) },
                            { append('0') },
                            { append(1 shl i) }
                        ).append(')').append('|')
                    }
                    setLength(length - 1) // rm last |
                }
            }
            .append(')')
            .append("\nON CONFLICT (id) DO UPDATE SET ")
            .also { sb ->
                repeat(wordCount) { index ->
                    sb.append("ch").append(index).append('=')
                        .appendName(onTable.name, changesTablePostfix).append('.').append("ch").append(index)
                        .append('|')
                        .append("EXCLUDED").append('.').append("ch").append(index).append(',')
                }
                setLength(length - 1) // rm last |
            }
    }

    override fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.changesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder {
        append(if (create) "CREATE" else "DROP")
            .append(' ').append("TRIGGER")
            .append(' ').appendTriggerName(onTable, afterEvent, namePostfix)
        if (create) append(' ').append("AFTER").append(' ').append(afterEvent.name)
        append(' ').append('O').append('N').append(' ').appendName(onTable.name)
        if (create) append(' ').append("FOR EACH ROW EXECUTE").append(' ')
            .append("PROCEDURE") // historical keyword for backwards compatibility
            .append(' ').appendTriggerName(onTable, afterEvent, namePostfix.toString() + "_func").append("()")
        return this
    }
}
