package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.VarFuncImpl
import net.aquadc.persistence.sql.ColCond
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.VarFunc
import net.aquadc.persistence.sql.RealDao
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Record
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet


internal class BlockingQuery<CUR : AutoCloseable, R>(
        private val session: Blocking<CUR>,
        private val query: String,
        private val argumentTypes: Array<out DataType.Simple<*>>,
        private val fetch: Fetch<Blocking<CUR>, R>
) : VarFuncImpl<Any, R>(), VarFunc<Any, R> {

    override fun invokeUnchecked(vararg arg: Any): R =
            fetch.fetch(session, query, argumentTypes, arg)

}

internal abstract class LowLevelSession<STMT, CUR : AutoCloseable> : Blocking<CUR> {
    abstract fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID

    /** [columnNames] : [values] is a map */
    abstract fun <SCH : Schema<SCH>, ID : IdBound> update(
            table: Table<SCH, ID, *>, id: ID,
            columnNames: Any/*=[arrayOf]CharSequence*/, columnTypes: Any/*=[arrayOf]DataType*/, values: Any?/*=[arrayOf]Any?*/
    )

    abstract fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID, *>, primaryKey: ID)

    abstract fun truncate(table: Table<*, *, *>)

    abstract fun onTransactionEnd(successful: Boolean)

    abstract fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID, *>, colName: CharSequence, colType: DataType<T>, id: ID
    ): T

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): Array<ID> // TODO: should return primitive arrays, too

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>
    ): Long

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID, *>, columnNames: Array<out CharSequence>, columnTypes: Array<out DataType<*>>, id: ID
    ): Array<Any?>

    abstract val transaction: RealTransaction?

    @JvmField val daos: ConcurrentHashMap<Table<*, *, *>, RealDao<*, *, *, STMT>> = ConcurrentHashMap()

    @Suppress("UPPER_BOUND_VIOLATED")
    private val localReusableCond = ThreadLocal<ColCond<Any, Any?>>()

    @Suppress("UNCHECKED_CAST", "UPPER_BOUND_VIOLATED")
    internal fun <SCH : Schema<SCH>, ID : IdBound> pkCond(
            table: Table<SCH, ID, out Record<SCH, ID>>, value: ID
    ): ColCond<SCH, ID> {
        val condition = (localReusableCond as ThreadLocal<ColCond<SCH, ID>>).getOrSet {
            ColCond(table.pkColumn as Lens<SCH, Record<SCH, *>, Record<SCH, *>, ID, *>, " = ?", value)
        }
        condition.lens = table.pkColumn as Lens<SCH, Record<SCH, *>, Record<SCH, *>, ID, *> // unchecked: we don't mind actual types
        condition.valueOrValues = value
        return condition
    }

}

internal fun Session<*>.createTransaction(lock: ReentrantReadWriteLock, lowLevel: LowLevelSession<*, *>): RealTransaction {
    val wLock = lock.writeLock()
    check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in a transaction" }
    wLock.lock()
    return RealTransaction(this, lowLevel)
}
