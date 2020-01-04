package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet


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

}

@Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
internal fun <SCH : Schema<SCH>, ID : IdBound> ThreadLocal<ColCond<Any, Any?>>.pkCond(
    table: Table<SCH, ID, out Record<SCH, ID>>, value: ID
): ColCond<SCH, ID> {
    val condition = (this as ThreadLocal<ColCond<SCH, ID>>).getOrSet {
        ColCond(table.pkColumn as Lens<SCH, Record<SCH, *>, Record<SCH, *>, ID, *>, " = ?", value)
    }
    condition.lens = table.pkColumn as Lens<SCH, Record<SCH, *>, Record<SCH, *>, ID, *> // unchecked: we don't mind actual types
    condition.valueOrValues = value
    return condition
}

internal fun Session.createTransaction(lock: ReentrantReadWriteLock, lowLevel: LowLevelSession<*>): RealTransaction {
    val wLock = lock.writeLock()
    check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in a transaction" }
    wLock.lock()
    return RealTransaction(this, lowLevel)
}
