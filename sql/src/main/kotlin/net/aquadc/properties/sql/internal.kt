package net.aquadc.properties.sql

import net.aquadc.persistence.struct.NamedLens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType


internal interface LowLevelSession {
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID
    fun <SCH : Schema<SCH>, ID : IdBound, T> update(table: Table<SCH, ID, *>, id: ID, column: NamedLens<SCH, Struct<SCH>, T>, value: T)
    fun <ID : IdBound> delete(table: Table<*, ID, *>, primaryKey: ID)
    fun truncate(table: Table<*, *, *>)
    val daos: Map<Table<*, *, *>, RealDao<*, *, *>>
    fun onTransactionEnd(successful: Boolean)

    fun <ID : IdBound, SCH : Schema<SCH>, T> fetchSingle(
            table: Table<SCH, ID, *>, columnName: String, type: DataType<T>, condition: WhereCondition<out SCH>
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
