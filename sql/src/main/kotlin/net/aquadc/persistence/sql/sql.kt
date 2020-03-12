@file:UseExperimental(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.intersect
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import org.intellij.lang.annotations.Language
import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Common supertype for all primary keys.
 */
typealias IdBound = Any // Serializable in some frameworks

/**
 * A shorthand for properties backed by RDBMS column & row.
 */
typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

@Retention(AnnotationRetention.BINARY)
@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalSql

/**
 * A gateway into RDBMS.
 */
interface Session<SRC> {

    /**
     * Lazily creates and returns DAO for the given table.
     */
    operator fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> get(
            table: Table<SCH, ID, REC>
    ): Dao<SCH, ID, REC>

    /**
     * Opens a transaction, allowing mutation of data.
     */
    fun beginTransaction(): Transaction

    fun <R> rawQuery(@Language("SQL") query: String, argumentTypes: Array<out DataType.Simple<*>>, fetch: Fetch<SRC, R>): VarFunc<Any, R>
    //               ^^^^^^^^^^^^^^^^ add Database Navigator to IntelliJ for SQL highlighting in String literals

}

interface LazyList<out E> : List<E>, Closeable // by GreenDAO


/**
 * Represents a database session specialized for a certain [Table].
 * {@implNote [Manager] supertype is used by [ManagedProperty] instances}
 */
interface Dao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> : Manager<SCH, Transaction, ID> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<SCH>, order: Array<out Order<SCH>>/* TODO: prefetch */): Property<List<REC>> // TODO Fetch | group by | having
    // todo joins
    fun count(condition: WhereCondition<SCH>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition
}

/**
 * Calls [block] within transaction passing [Transaction] which has functionality to create, mutate, remove [Record]s.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
inline fun <R> Session<*>.withTransaction(block: Transaction.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }

    val transaction = beginTransaction()
    try {
        val r = block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}

fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.require(id: ID): REC =
        find(id) ?: throw NoSuchElementException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.select(
        condition: WhereCondition<SCH>, vararg order: Order<SCH>/* TODO: prefetch */
): Property<List<REC>> =
        select(condition, order)

fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.selectAll(vararg order: Order<SCH>): Property<List<REC>> =
        select(emptyCondition(), order)

fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> Dao<SCH, ID, REC>.count(): Property<Long> =
        count(emptyCondition())

@JvmField val NoOrder = emptyArray<Order<Nothing>>()


interface Transaction : AutoCloseable {

    /**
     * Insert [data] into a [table].
     */
    fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID, REC>, data: Struct<SCH>): REC
    // TODO insert(Iterator)
    // TODO emulate slow storage!

    fun <SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>, T> update(table: Table<SCH, ID, REC>, id: ID, field: FieldDef.Mutable<SCH, T, *>, previous: T, value: T)
    // TODO: update where

    fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>)
    // TODO: delete where

    /**
     * Clear the whole table.
     * This may be implemented either as `DELETE FROM table` or `TRUNCATE table`.
     */
    fun truncate(table: Table<*, *, *>)

    fun setSuccessful()

    operator fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.set(field: FieldDef.Mutable<SCH, T, *>, new: T) {
        (this prop field).setValue(this@Transaction, new)
    }

    /**
     * Updates field values from [source].
     * @return a set of updated fields
     *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
     */
    fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.setFrom(
            source: PartialStruct<SCH>, fields: FieldSet<SCH, FieldDef.Mutable<SCH, *, *>>
    ): FieldSet<SCH, FieldDef.Mutable<SCH, *, *>> =
            source.fields.intersect(fields).also { intersect ->
                source.schema.forEach(intersect) { field ->
                    mutateFrom(source, field) // capture type
                }
            }
    @Suppress("NOTHING_TO_INLINE")
    private inline fun <REC : Record<SCH, ID>, SCH : Schema<SCH>, ID : IdBound, T> REC.mutateFrom(
            source: PartialStruct<SCH>, field: FieldDef.Mutable<SCH, T, *>
    ) {
        this[field] = source.getOrThrow(field)
    }

}

@Deprecated("moved") typealias JdbcSession = net.aquadc.persistence.sql.blocking.JdbcSession
@Deprecated("moved") typealias SqliteSession = net.aquadc.persistence.sql.blocking.SqliteSession
