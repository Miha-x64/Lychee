package net.aquadc.persistence.sql.blocking

import net.aquadc.collections.InlineEnumSet
import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.FuncN
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import java.util.concurrent.locks.ReentrantReadWriteLock


internal class BlockingQuery<CUR, R>(
    private val session: Blocking<CUR>,
    private val query: String,
    private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
    private val fetch: Fetch<Blocking<CUR>, R>
) : FuncXImpl<Any, R>(), FuncN<Any, R> {

    override fun invokeUnchecked(vararg args: Any): R =
            fetch.fetch(session, query, argumentTypes, args)

    // for debugging
    override fun toString(): String =
        fetch.javaClass.simpleName + '(' + query + ')'

}

internal abstract class LowLevelSession<STMT, CUR> : Blocking<CUR> {
    val statements = ThreadLocal<MutableMap<Any, STMT>>()

    abstract fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: Struct<SCH>): ID

    abstract fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, primaryKey: ID)

    abstract fun truncate(table: Table<*, *>)

    abstract fun onTransactionEnd(successful: Boolean)

    abstract fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
        table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
    ): T

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetch(
        table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
    ): Array<Any?>

    abstract val transaction: RealTransaction?

    abstract fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    abstract fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
}

internal fun createTransaction(lock: ReentrantReadWriteLock, lowLevel: LowLevelSession<*, *>): RealTransaction {
    val wLock = lock.writeLock()
    check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in a transaction" }
    wLock.lock()
    return RealTransaction(lowLevel)
}
