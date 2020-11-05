package net.aquadc.persistence.sql.blocking

import net.aquadc.collections.InlineEnumSet
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.TriggerEvent
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.Ilk
import java.util.concurrent.locks.ReentrantReadWriteLock


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

    abstract val transaction: RealTransaction<Blocking<CUR>>?

    abstract fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    abstract fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
}

internal fun <SRC> Session<SRC>.createTransaction(lock: ReentrantReadWriteLock, lowLevel: LowLevelSession<*, *>): RealTransaction<SRC> {
    val wLock = lock.writeLock()
    check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in a transaction" }
    wLock.lock()
    return RealTransaction(this, lowLevel)
}
