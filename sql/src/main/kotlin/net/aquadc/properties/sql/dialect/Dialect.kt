package net.aquadc.properties.sql.dialect

import net.aquadc.properties.sql.*
import net.aquadc.persistence.converter.DataType
import java.lang.StringBuilder

/**
 * Represents an SQL dialect. Provides functions for building queries.
 */
interface Dialect {

    /**
     * Constructs an SQL query like `INSERT INTO <table> (<col>, <col>, ...) VALUES (?, ?, ...)`
     */
    fun <REC : Record<REC, *>> insertQuery(table: Table<REC, *>, cols: Array<Col<REC, *>>): String

    /**
     * Constructs an SQL query like `SELECT <col> from <table> WHERE <condition>`
     */
    fun <REC : Record<REC, *>> selectFieldQuery(
            columnName: String, table: Table<REC, *>, condition: WhereCondition<out REC>, order: Array<out Order<out REC>>
    ): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <REC : Record<REC, *>> selectCountQuery(table: Table<REC, *>, condition: WhereCondition<out REC>): String

    /**
     * Appends WHERE clause (without WHERE itself) to [this] builder.
     */
    fun <REC : Record<REC, *>> StringBuilder.appendWhereClause(condition: WhereCondition<out REC>): StringBuilder

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    fun <REC : Record<REC, *>> StringBuilder.appendOrderClause(order: Array<out Order<out REC>>): StringBuilder

    /**
     *  Construcs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <REC : Record<REC, *>> updateFieldQuery(table: Table<REC, *>, col: Col<REC, *>): String

    /**
     * Constructs an SQL query like `DELETE FROM <table> WHERE <idCol> = ?`
     */
    fun deleteRecordQuery(table: Table<*, *>): String

    /**
     * Appends quoted and escaped table or column name.
     */
    fun StringBuilder.appendName(name: String): StringBuilder

    /**
     * Returns SQL data type for the given [DataType] instance.
     */
    fun nameOf(dataType: DataType): String

    /**
     * Returns an SQL query to create the given [table].
     */
    fun createTable(table: Table<*, *>): String

}
