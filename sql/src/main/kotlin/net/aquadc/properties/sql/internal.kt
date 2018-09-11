package net.aquadc.properties.sql


internal interface LowLevelSession {
    fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, cols: Array<Col<REC, *>>, vals: Array<Any?>): ID
    fun <REC : Record<REC, ID>, ID : IdBound, T> update(table: Table<REC, ID>, id: ID, column: Col<REC, T>, value: T)
    fun <ID : IdBound> localId(table: Table<*, ID>, id: ID): Long
    fun <ID : IdBound> primaryKey(table: Table<*, ID>, id: Long): ID
    fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID>, primaryKey: ID): Long
    val daos: Map<Table<*, *>, RealDao<*, *>>
    fun onTransactionEnd(successful: Boolean)

    fun <ID : IdBound, REC : Record<REC, ID>, T> fetchSingle(
            column: Col<REC, T>, table: Table<REC, ID>, condition: WhereCondition<out REC>
    ): T

    fun <ID : IdBound, REC : Record<REC, ID>> fetchPrimaryKeys(
            table: Table<REC, ID>, condition: WhereCondition<out REC> /* TODO order */
    ): Array<ID>

    fun <ID : IdBound, REC : Record<REC, ID>> fetchCount(
            table: Table<REC, ID>, condition: WhereCondition<out REC>
    ): Long

    val transaction: RealTransaction?
}
