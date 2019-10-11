package net.aquadc.properties.sql.dialect

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.properties.sql.Order
import net.aquadc.properties.sql.Table
import net.aquadc.properties.sql.WhereCondition

/**
 * Represents an SQL dialect. Provides functions for building queries.
 */
interface Dialect {

    /**
     * Constructs an `INSERT INTO <table> (<col>, <col>, ...) VALUES (?, ?, ...)` SQL query
     */
    fun <SCH : Schema<SCH>> insert(table: Table<SCH, *, *>): String

    /**
     * Constructs an SQL query like `SELECT <col> from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> selectQuery(
            table: Table<SCH, *, *>, columns: Array<NamedLens<SCH, *, *>>,
            condition: WhereCondition<SCH>, order: Array<out Order<out SCH>>
    ): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> selectCountQuery(table: Table<SCH, *, *>, condition: WhereCondition<SCH>): String

    /**
     * Appends WHERE clause (without WHERE itself) to [this] builder.
     */
    fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(context: Table<SCH, *, *>, condition: WhereCondition<SCH>): StringBuilder

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(order: Array<out Order<out SCH>>): StringBuilder

    /**
     *  Constructs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *, *>, cols: Array<NamedLens<SCH, Struct<SCH>, *>>): String

    /**
     * Constructs an SQL query like `DELETE FROM <table> WHERE <idCol> = ?`
     */
    fun deleteRecordQuery(table: Table<*, *, *>): String

    /**
     * Appends quoted and escaped table or column name.
     */
    fun StringBuilder.appendName(name: String): StringBuilder

    /**
     * Returns an SQL query to create the given [table].
     */
    fun createTable(table: Table<*, *, *>): String

    /**
     * Returns `TRUNCATE` query to clear the whole table.
     */
    fun truncate(table: Table<*, *, *>): String

}
