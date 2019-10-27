package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import java.util.concurrent.ConcurrentHashMap


internal interface LowLevelSession<STMT> {
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID

    /** [columns] : [values] is a map */
    fun <SCH : Schema<SCH>, ID : IdBound> update(
            table: Table<SCH, ID, *>, id: ID,
            columns: Any /* = [array of] StoredNamedLens<SCH, Struct<SCH>, *>> */, values: Any? /* = [array of] Any? */
    )

    fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID, *>, primaryKey: ID)

    fun truncate(table: Table<*, *, *>)

    val daos: ConcurrentHashMap<Table<*, *, *>, RealDao<*, *, *, STMT>>

    fun onTransactionEnd(successful: Boolean)

    fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID, *>, column: StoredNamedLens<SCH, T, *>, id: ID
    ): T

    fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): Array<ID> // TODO: should return primitive arrays, too

    fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>
    ): Long

    fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID, *>, columns: Array<out StoredNamedLens<SCH, *, *>>, id: ID
    ): Array<Any?>

    val transaction: RealTransaction?

    fun <SCH : Schema<SCH>, ID : IdBound> pkCond(table: Table<SCH, ID, out Record<SCH, ID>>, value: ID): ColCond<SCH, ID>

}
