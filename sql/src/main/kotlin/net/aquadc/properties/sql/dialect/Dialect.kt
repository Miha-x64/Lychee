package net.aquadc.properties.sql.dialect

import net.aquadc.properties.sql.*
import net.aquadc.persistence.type.DataType
import java.lang.StringBuilder

/**
 * Represents an SQL dialect. Provides functions for building queries.
 */
interface Dialect {

    /**
     * Constructs an SQL query like `INSERT INTO <table> (<col>, <col>, ...) VALUES (?, ?, ...)`
     */
    fun <TBL : Table<TBL, *, *>> insertQuery(table: Table<TBL, *, *>, cols: Array<Col<TBL, *>>): String

    /**
     * Constructs an SQL query like `SELECT <col> from <table> WHERE <condition>`
     */
    fun <TBL : Table<TBL, *, *>> selectFieldQuery(
            columnName: String, table: Table<TBL, *, *>, condition: WhereCondition<out TBL>, order: Array<out Order<out TBL>>
    ): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <TBL : Table<TBL, *, *>> selectCountQuery(table: Table<TBL, *, *>, condition: WhereCondition<out TBL>): String

    /**
     * Appends WHERE clause (without WHERE itself) to [this] builder.
     */
    fun <TBL : Table<TBL, *, *>> StringBuilder.appendWhereClause(condition: WhereCondition<out TBL>): StringBuilder

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    fun <TBL : Table<TBL, *, *>> StringBuilder.appendOrderClause(order: Array<out Order<out TBL>>): StringBuilder

    /**
     *  Construcs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <TBL : Table<TBL, *, *>> updateFieldQuery(table: Table<TBL, *, *>, col: Col<TBL, *>): String

    /**
     * Constructs an SQL query like `DELETE FROM <table> WHERE <idCol> = ?`
     */
    fun deleteRecordQuery(table: Table<*, *, *>): String

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
    fun createTable(table: Table<*, *, *>): String

}
