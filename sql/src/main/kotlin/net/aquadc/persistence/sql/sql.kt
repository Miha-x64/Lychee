@file:[
    JvmName("Sql")
    OptIn(ExperimentalContracts::class)
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.MutableField
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.intersect
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.persistence.TransactionalPropertyStruct
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
@RequiresOptIn("Under construction.", RequiresOptIn.Level.WARNING)
annotation class ExperimentalSql

/**
 * A gateway into RDBMS.
 */
interface Session<SRC> {

    /**
     * Lazily creates and returns DAO for the given table.
     */
    operator fun <SCH : Schema<SCH>, ID : IdBound> get(table: Table<SCH, ID>): Dao<SCH, ID>

    /**
     * Opens a transaction, allowing mutation of data.
     */
    fun beginTransaction(): Transaction

    fun <R> rawQuery(
        @Language("SQL") query: String,
    //  ^^^^^^^^^^^^^^^^ add Database Navigator to IntelliJ for SQL highlighting in String literals
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        fetch: Fetch<SRC, R>
    ): VarFunc<Any, R>

}


/**
 * Represents a database session specialized for a certain [Table].
 * {@implNote [Manager] supertype is used by [ManagedProperty] instances}
 */
interface Dao<SCH : Schema<SCH>, ID : IdBound> : Manager<SCH, Transaction, ID> {
    fun find(id: ID): Record<SCH, ID>?
    fun select(
        condition: WhereCondition<SCH>, order: Array<out Order<SCH>>
    ): Property<List<Record<SCH, ID>>> // TODO group by | having

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

fun <SCH : Schema<SCH>, ID : IdBound> Dao<SCH, ID>.require(id: ID): Record<SCH, ID> =
        find(id) ?: throw NoSuchElementException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound> Dao<SCH, ID>.select(
        condition: WhereCondition<SCH>, vararg order: Order<SCH>
): Property<List<Record<SCH, ID>>> =
        select(condition, order)

fun <SCH : Schema<SCH>, ID : IdBound> Dao<SCH, ID>.selectAll(vararg order: Order<SCH>): Property<List<Record<SCH, ID>>> =
        select(emptyCondition(), order)

fun <SCH : Schema<SCH>, ID : IdBound> Dao<SCH, ID>.count(): Property<Long> =
        count(emptyCondition())

@JvmField internal val NoOrder = emptyArray<Order<Nothing>>()
internal inline fun <SCH : Schema<SCH>> noOrder(): Array<Order<SCH>> = NoOrder as Array<Order<SCH>>


interface Transaction : Closeable {

    /**
     * Insert [data] into a [table].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: Struct<SCH>/*todo patch: Partial*/): Record<SCH, ID>

    /**
     * Insert all the [data] into a table.
     * Iterators over __transient structs__ are welcome.
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<Struct<SCH>>/*todo patch: Partial*/) {
        for (struct in data)
            insert(table, struct)
    }
    // TODO emulate slow storage!

    // OMG, private API?
    fun <SCH : Schema<SCH>, ID : IdBound, T> update(table: Table<SCH, ID>, id: ID, field: MutableField<SCH, T, *>, previous: T, value: T)
    // TODO: update where

    fun <SCH : Schema<SCH>, ID : IdBound> delete(record: Record<SCH, ID>)
    // TODO: delete where

    /**
     * Clear the whole table.
     * This may be implemented either as `DELETE FROM table` or `TRUNCATE table`.
     */
    fun truncate(table: Table<*, *>)

    fun setSuccessful()

    operator fun <SCH : Schema<SCH>, ID : IdBound, T> Record<SCH, ID>.set(field: MutableField<SCH, T, *>, new: T) {
        (this prop field).setValue(this@Transaction, new)
    }

    /**
     * Updates field values from [source].
     * @return a set of updated fields
     *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
     */
    fun <SCH : Schema<SCH>, ID : IdBound, T> Record<SCH, ID>.setFrom(
            source: PartialStruct<SCH>, fields: FieldSet<SCH, MutableField<SCH, *, *>>
    ): FieldSet<SCH, MutableField<SCH, *, *>> =
            source.fields.intersect(fields).also { intersect ->
                source.schema.forEach(intersect) { field ->
                    mutateFrom(source, field) // capture type
                }
            }
    @Suppress("NOTHING_TO_INLINE")
    private inline fun <SCH : Schema<SCH>, ID : IdBound, T> Record<SCH, ID>.mutateFrom(
            source: PartialStruct<SCH>, field: MutableField<SCH, T, *>
    ) {
        this[field] = source.getOrThrow(field)
    }

}

@Suppress("NOTHING_TO_INLINE")
inline fun <SCH : Schema<SCH>, ID : IdBound>
    Record<SCH, ID>.transactional(): TransactionalPropertyStruct<SCH> =
    RecordTransactionalAdapter(this)
