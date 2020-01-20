package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.ColCond
import net.aquadc.persistence.sql.FetchStruct
import net.aquadc.persistence.sql.FetchValue
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.ListChanges
import net.aquadc.persistence.sql.Order
import net.aquadc.persistence.sql.RealDao
import net.aquadc.persistence.sql.RealTransaction
import net.aquadc.persistence.sql.Record
import net.aquadc.persistence.sql.Selection
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.WhereCondition
import net.aquadc.persistence.struct.FldSet
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleNullable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.getOrSet


internal class BlockingSelection(
        private val session: BlockingSession,
        private val query: String,
        private val arguments: Array<out Any>
) : Selection<BlockingSession> {

    override fun <T, R> cell(type: DataType.Simple<T>, fetch: FetchValue<BlockingSession, T, R, *>): R =
            fetch.cell(session, query, arguments, type)

    override fun <T : Any, R> cell(type: SimpleNullable<T>, fetch: FetchValue<BlockingSession, T?, R, *>): R =
            fetch.cell(session, query, arguments, type)

    override fun <T, R> col(type: DataType.Simple<T>, fetch: FetchValue<BlockingSession, T, *, R>): R =
            fetch.col(session, query, arguments, type)

    override fun <T : Any, R> col(type: SimpleNullable<T>, fetch: FetchValue<BlockingSession, T?, *, R>): R =
            fetch.col(session, query, arguments, type)

    override fun <S : Schema<S>, R> row(schema: S, bindBy: BindBy, fetch: FetchStruct<BlockingSession, S, Nothing, FldSet<S>, R, *>): R =
            fetch.row(session, query, arguments, schema, bindBy)

    override fun <S : Schema<S>, R, ID : IdBound> grid(schema: S, bindBy: BindBy, fetch: FetchStruct<BlockingSession, S, ID, ListChanges<S, ID>, *, R>): R =
            fetch.grid(session, query, arguments, schema, bindBy)

}

internal abstract class LowLevelSession<STMT> : BlockingSession {
    abstract fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, *>, data: Struct<SCH>): ID

    /** [columns] : [values] is a map */
    abstract fun <SCH : Schema<SCH>, ID : IdBound> update(
            table: Table<SCH, ID, *>, id: ID,
            columns: Any /* = [array of] StoredNamedLens<SCH, Struct<SCH>, *>> */, values: Any? /* = [array of] Any? */
    )

    abstract fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID, *>, primaryKey: ID)

    abstract fun truncate(table: Table<*, *, *>)

    abstract fun onTransactionEnd(successful: Boolean)

    abstract fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
            table: Table<SCH, ID, *>, column: StoredNamedLens<SCH, T, *>, id: ID
    ): T

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetchPrimaryKeys(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): Array<ID> // TODO: should return primitive arrays, too

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetchCount(
            table: Table<SCH, ID, *>, condition: WhereCondition<SCH>
    ): Long

    abstract fun <SCH : Schema<SCH>, ID : IdBound> fetch(
            table: Table<SCH, ID, *>, columns: Array<out StoredNamedLens<SCH, *, *>>, id: ID
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

internal fun Session<*>.createTransaction(lock: ReentrantReadWriteLock, lowLevel: LowLevelSession<*>): RealTransaction {
    val wLock = lock.writeLock()
    check(!wLock.isHeldByCurrentThread) { "Thread ${Thread.currentThread()} is already in a transaction" }
    wLock.lock()
    return RealTransaction(this, lowLevel)
}
