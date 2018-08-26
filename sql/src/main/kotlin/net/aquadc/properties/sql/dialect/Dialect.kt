package net.aquadc.properties.sql.dialect

import net.aquadc.properties.sql.Col
import net.aquadc.properties.sql.Record
import net.aquadc.properties.sql.Table
import net.aquadc.properties.sql.WhereCondition
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
    fun <REC : Record<REC, *>> selectFieldQuery(column: Col<REC, *>, table: Table<REC, *>, condition: WhereCondition<out REC>): String

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    fun <REC : Record<REC, *>> selectCountQuery(table: Table<REC, *>, condition: WhereCondition<out REC>): String

    /**
     *  Construcs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    fun <REC : Record<REC, *>> updateFieldQuery(table: Table<REC, *>, col: Col<REC, *>): String

    /**
     * Constructs an SQL query like `DELETE FROM <table> WHERE <idCol> = ?`
     */
    fun <REC : Record<REC, *>> deleteRecordQuery(table: Table<REC, *>): String

    /**
     * Appends quoted and escaped table or column name.
     */
    fun StringBuilder.appendName(name: String): StringBuilder

}
