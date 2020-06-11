package net.aquadc.persistence.sql.dialect

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.type.DataType

/**
 * Represents an SQL dialect. Provides functions for building queries.
 */
interface Dialect {

    /**
     * Constructs an `INSERT INTO <table> (<col>, <col>, ...) VALUES (?, ?, ...)` SQL query
     */
    fun <SCH : Schema<SCH>> insert(table: Table<SCH, *>): String

    /**
     * Constructs an SQL query like `SELECT <col> from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> selectQuery(
            table: Table<SCH, *>, columns: Array<out CharSequence>,
            condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> selectCountQuery(table: Table<SCH, *>, condition: WhereCondition<SCH>): String

    /**
     * Appends WHERE clause (without WHERE itself) to [this] builder.
     */
    @Deprecated("unused by Session", level = DeprecationLevel.ERROR)
    fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(context: Table<SCH, *>, condition: WhereCondition<SCH>): Nothing =
        throw AssertionError()

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(schema: SCH, order: Array<out Order<SCH>>): StringBuilder

    /**
     *  Constructs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *>, cols: Array<out CharSequence>): String

    /**
     * Constructs an SQL query like `DELETE FROM <table> WHERE <idCol> = ?`
     */
    fun deleteRecordQuery(table: Table<*, *>): String

    /**
     * Appends quoted and escaped table or column name.
     */
    fun StringBuilder.appendName(name: CharSequence): StringBuilder

    /**
     * Returns an SQL query to create the given [table].
     */
    fun createTable(table: Table<*, *>, temporary: Boolean = false): String

    /**
     * Returns `TRUNCATE` query to clear the whole table.
     */
    fun truncate(table: Table<*, *>): String

    /**
     * Whether database has support for arrays.
     */
    val hasArraySupport: Boolean

    /**
     * Figures out simple name of a primitive type.
     */
    fun nameOf(kind: DataType.NotNull.Simple.Kind): String

}
