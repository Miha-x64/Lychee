@file:JvmName("SqliteDialect")
package net.aquadc.persistence.sql.dialect.sqlite

import net.aquadc.collections.enumMapOf
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.sql.dialect.BaseDialect
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.wordCountForCols
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.DataType
import kotlin.math.min

/**
 * Implements SQLite [Dialect].
 */
// wannabe `@JvmField val` but this breaks compilation. Dafuq?
object SqliteDialect : BaseDialect(
    enumMapOf(
        DataType.NotNull.Simple.Kind.Bool, "INTEGER",
        DataType.NotNull.Simple.Kind.I32, "INTEGER",
        DataType.NotNull.Simple.Kind.I64, "INTEGER",
        DataType.NotNull.Simple.Kind.F32, "REAL",
        DataType.NotNull.Simple.Kind.F64, "REAL",
        DataType.NotNull.Simple.Kind.Str, "TEXT",
        DataType.NotNull.Simple.Kind.Blob, "BLOB"
    ),
    truncate = "DELETE FROM", // SQLite does not have TRUNCATE statement
    arrayPostfix = null // no array support
) {

    override fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.changesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder {
        append(if (create) "CREATE TEMP" else "DROP")
            .append(' ').append("TRIGGER")
            .append(' ').appendTriggerName(onTable, afterEvent, namePostfix)

        return if (!create) this else append(' ')
            .append("AFTER").append(' ').append(afterEvent.name).append(' ')
            .append('O').append('N').append(' ').appendName(onTable.name).append('\n')
            .append("BEGIN").append('\n')
            .let { when (afterEvent) {
                TriggerEvent.INSERT -> it.appendStructuralTrg(namePostfix, "new", onTable, '+')
                TriggerEvent.UPDATE -> it.appendUpdTrg(namePostfix, onTable)
                TriggerEvent.DELETE -> it.appendStructuralTrg(namePostfix, "old", onTable, '-')
            } }
            .append(";\nEND")
    }

    // replace (pk, previous±1, -1, …)
    private fun StringBuilder.appendStructuralTrg(
        changesTablePostfix: CharSequence, recordReference: String, onTable: Table<*, *>, balance: Char
    ): StringBuilder =
        append("REPLACE").append(' ').append("INTO").append(' ').appendName(onTable.name, changesTablePostfix).append(' ')
            .append("VALUES").append(' ').append('(')

            // change.id = alteredRecord.id
            .appendQualified(recordReference, onTable.idColName)

            .append(',')

            // change.what = coalesce(previousChange.what, 0) ± 1
            .appendCoalesce {
                append('(').append("SELECT").append(' ').appendName("what").append(' ')
                    .append("FROM").append(' ').appendName(onTable.name, changesTablePostfix).append(' ')
                    .append("WHERE").append(' ').appendName("id").append('=').appendQualified(recordReference, onTable.idColName)
                    .append(')').append(',')
                    .append('0')
            }
            .append(balance).append('1')

            // fill remaining fields with -1s. They'll be ignored for nonzero balance,
            // but if the record was removed and then a record with same PK was inserted (balance=0-1+1),
            // we will think that all columns were changed.
            .also { sb ->
                repeat(wordCountForCols(onTable.managedColumns.size)) { _ ->
                    sb.append(',').append('-').append('1')
                }
            }
            .append(')')

    // insert/update (pk, previous, oldMask | newMask)
    private fun <SCH : Schema<SCH>> StringBuilder.appendUpdTrg(
        changesTablePostfix: CharSequence, onTable: Table<SCH, *>
    ): StringBuilder =
        append("REPLACE").append(' ').append("INTO").append(' ').appendName(onTable.name, changesTablePostfix).append(' ')
            .append("VALUES").append(' ').append('(')

            // change.id = alteredRecord.id
            .appendQualified("new"/*whatever*/, onTable.idColName)

            .append(',')

            // change.what = 0, neither updated nor removed
            .append(0)

            .also { sb ->
                val cols = onTable.managedColumns
                repeat(wordCountForCols(cols.size)) { index ->
                    sb.append(',')
                    val offset = index * 64
                    val size = min(64, cols.size - offset)

                    // chN = (if (old.field = new.field) 1 shl index else 0) | (if …) | coalesce(previous value, 0)

                    for (i in 0 until size) {
                        val colName = cols[offset + i].name(onTable.schema)
                        sb.append('(').appendCase(
                            { appendQualified("old", colName).append('=').appendQualified("new", colName) },
                            { append('0') },
                            { append(1 shl i) }
                        ).append(')').append('|')
                    }
                    appendCoalesce { append('(')
                        .append("SELECT").append(' ').appendName("ch", index).append(' ')
                        .append("FROM").append(' ').appendName(onTable.name, changesTablePostfix).append(' ')
                        .append("WHERE").append(' ').appendName("id").append('=').appendQualified("new"/*whatever*/, onTable.idColName)
                        .append(')')
                        .append(',')
                        .append('0')
                    }
                }
            }
            .append(')')

    override fun trimMemory(): String =
        "PRAGMA shrink_memory"

}
