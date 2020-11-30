package net.aquadc.persistence.sql.dialect

import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.sql.SqlTypeName
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.type.DataType

/**
 * Represents an SQL dialect. Provides functions for building queries.
 */
interface Dialect {

    /**
     * Constructs an `INSERT INTO <table> (<col>, <col>, ...) VALUES (?, ?, ...)` SQL query
     */
    fun <SCH : Schema<SCH>> StringBuilder.insert(table: Table<SCH, *>, fields: FieldSet<SCH, out FieldDef<SCH, *, *>>): StringBuilder

    /**
     * Constructs an SQL query like `SELECT <col> from <table> WHERE <condition>`
     */
    fun <SCH : Schema<SCH>> StringBuilder.selectQuery(table: Table<SCH, *>, columns: Array<out CharSequence>): StringBuilder

    /**
     * Constructs an SQL query like `SELECT COUNT(*) from <table> WHERE <condition>`
     */
    @Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
    fun <SCH : Schema<SCH>> selectCountQuery(table: Table<SCH, *>, condition: Nothing): String =
        throw AssertionError()

    /**
     * Appends ORDER clause (without ORDER BY itself) to [this] builder.
     * @param order must be non-empty
     */
    @Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
    fun <SCH : Schema<SCH>> StringBuilder.appendOrderClause(schema: SCH, order: Nothing): StringBuilder =
        throw AssertionError()

    /**
     *  Constructs an SQL query like `UPDATE <table> SET <col> = ?`
     */
    @Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
    fun <SCH : Schema<SCH>> updateQuery(table: Table<SCH, *>, cols: Array<out CharSequence>): String =
        throw AssertionError()

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
    fun createTable(table: Table<*, *>, temporary: Boolean = false, ifNotExists: Boolean = false): String

    fun StringBuilder.createTable(
        temporary: Boolean, ifNotExists: Boolean, name: String, namePostfix: String?, idColName: CharSequence,
        idColTypeName: SqlTypeName, managedPk: Boolean,
        colNames: Array<out CharSequence>, colTypes: Array<out SqlTypeName>
    ): StringBuilder

    /**
     * Returns `TRUNCATE` query to clear the whole table.
     */
    fun truncate(table: Table<*, *>): String

    /**
     * Optionally builds CREATE|DROP FUNCTION which will be subscribed to the trigger.
     * Unused by SQLite where trigger function is anonymous;
     * used with PostgreSQL where trigger listener is a named function.
     * @param create CREATE FUNCTION if true, DROP FUNCTION otherwise
     */
    fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.prepareChangesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder

    /**
     * Builds `CREATE|DROP TRIGGER` query to observe changes.
     * @param create CREATE TRIGGER if true, DROP TRIGGER otherwise
     */
    fun <SCH : Schema<SCH>, ID : IdBound> StringBuilder.changesTrigger(
        namePostfix: CharSequence, afterEvent: TriggerEvent, onTable: Table<SCH, ID>, create: Boolean
    ): StringBuilder

    /**
     * Whether database has support for arrays.
     */
    val hasArraySupport: Boolean

    /**
     * Figures out simple name of a primitive type.
     */
    fun nameOf(kind: DataType.NotNull.Simple.Kind): String

    /**
     * Returns a query to reduce memory usage, if supported.
     */
    fun trimMemory(): String?

}
