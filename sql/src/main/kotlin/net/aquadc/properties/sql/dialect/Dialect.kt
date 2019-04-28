package net.aquadc.properties.sql.dialect

import net.aquadc.persistence.struct.Schema
import net.aquadc.properties.sql.*

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
    fun <SCH : Schema<SCH>> selectFieldQuery(
            columnName: String, table: Table<SCH, *, *>,
            condition: WhereCondition<out SCH>, order: Array<out Order<out SCH>>
    ): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> selectCountQuery(table: Table<SCH, *, *>, condition: WhereCondition<out SCH>): String

    /**
     * Appends WHERE clause (without WHERE itself) to [this] builder.
     */
    fun <SCH : Schema<SCH>> StringBuilder.appendWhereClause(condition: WhereCondition<out SCH>): StringBuilder

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(order: Array<out Order<out SCH>>): StringBuilder

    /**
     *  Constructs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <SCH : Schema<SCH>> updateFieldQuery(table: Table<SCH, *, *>, colName: String): String

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
