package net.aquadc.properties.sql


internal interface LowLevelSession {
    fun <TBL : Table<TBL, ID, *>, ID : IdBound> exists(table: Table<TBL, ID, *>, primaryKey: ID): Boolean
    fun <TBL : Table<TBL, ID, *>, ID : IdBound> insert(table: Table<TBL, ID, *>, cols: Array<Col<TBL, *>>, vals: Array<Any?>): ID
    fun <TBL : Table<TBL, ID, *>, ID : IdBound, T> update(table: Table<TBL, ID, *>, id: ID, column: Col<TBL, T>, value: T)
    fun <ID : IdBound> localId(table: Table<*, ID, *>, id: ID): Long
    fun <ID : IdBound> primaryKey(table: Table<*, ID, *>, localId: Long): ID
    fun <ID : IdBound> deleteAndGetLocalId(table: Table<*, ID, *>, primaryKey: ID): Long
    val daos: Map<Table<*, *, *>, RealDao<*, *, *>>
    fun onTransactionEnd(successful: Boolean)

    fun <ID : IdBound, TBL : Table<TBL, ID, *>, T> fetchSingle(
            column: Col<TBL, T>, table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>
    ): T

    fun <ID : IdBound, TBL : Table<TBL, ID, *>> fetchPrimaryKeys(
            table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>, order: Array<out Order<TBL>>
    ): Array<ID>

    fun <ID : IdBound, TBL : Table<TBL, ID, *>> fetchCount(
            table: Table<TBL, ID, *>, condition: WhereCondition<out TBL>
    ): Long

    val transaction: RealTransaction?

    fun <TBL : Table<TBL, *, *>, T : Any> reusableCond(table: Table<TBL, *, *>, colName: String, value: T): ColCond<TBL, T>

}
