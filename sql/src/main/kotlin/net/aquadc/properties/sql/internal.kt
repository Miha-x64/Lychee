package net.aquadc.properties.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct


internal interface LowLevelSession {
    fun <SCH : Schema<SCH>, ID : IdBound> exists(table: Table<SCH, ID, *>, primaryKey: ID): Boolean
    fun <SCH : Schema<SCH>, ID : IdBound> replace(table: Table<SCH, ID, *>, data: Struct<SCH>): ID
    fun <SCH : Schema<SCH>, ID : IdBound, T> update(table: Table<SCH, ID, *>, id: ID, column: FieldDef<SCH, T>, value: T)
    fun <ID : IdBound> delete(table: Table<*, ID, *>, primaryKey: ID)
    val daos: Map<Table<*, *, *>, RealDao<*, *, *>>
    fun onTransactionEnd(successful: Boolean)

    fun <ID : IdBound, SCH : Schema<SCH>, T> fetchSingle(
            column: FieldDef<SCH, T>, table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>
    ): T

    fun <ID : IdBound, SCH : Schema<SCH>> fetchPrimaryKeys(
            table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>, order: Array<out Order<SCH>>
    ): Array<ID>

    fun <ID : IdBound, SCH : Schema<SCH>> fetchCount(
            table: Table<SCH, ID, *>, condition: WhereCondition<out SCH>
    ): Long

    val transaction: RealTransaction?

    fun <SCH : Schema<SCH>, T : Any> reusableCond(table: Table<SCH, *, *>, colName: String, value: T): ColCond<SCH, T>

}
