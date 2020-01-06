@file:UseExperimental(ExperimentalContracts::class)
package net.aquadc.persistence.sql

import kotlinx.coroutines.Deferred
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.FldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.intersectMutable
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleNullable
import net.aquadc.persistence.type.string
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.persistence.PropertyStruct
import org.intellij.lang.annotations.Language
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext


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
interface Session {

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

    fun rawQuery(@Language("SQL") query: String, vararg arguments: Any): Result
    //           ^^^^^^^^^^^^^^^^ add Database Navigator to IntelliJ for SQL highlighting in String literals

}

interface Result {
    fun <T,       V> cell(type: DataType.Simple<T>, fetch: FetchValue<T,  V, *>): V
    fun <T : Any, V> cell(type: SimpleNullable<T>,  fetch: FetchValue<T?, V, *>): V

    fun <T, L>
            col(type: DataType.Simple<T>,           fetch: FetchValue<T,  *, L>): L
    fun <T : Any, L>
            col(type: SimpleNullable<T>,            fetch: FetchValue<T?, *, L>): L

    fun <S : Schema<S>, T>
            row(schema: S,          bindBy: BindBy, fetch: FetchStruct<S, Nothing, FldSet<S>, T, *>): T

    fun <S : Schema<S>, L, ID : IdBound>
            grid(schema: S,         bindBy: BindBy, fetch: FetchStruct<S, ID, ListChanges<S, ID>, *, L>): L
}

// I have absolutely no idea how to represent primitive list changes so there's no D parameter:
interface FetchValue <T, VAL, LST>
interface FetchStruct<S, ID, D, STR, LST>
enum class BindBy {
    Name,
    Position,
}
class ListChanges<SCH : Schema<SCH>, ID : IdBound>(
        val oldIds: List<ID>, // List could wrap IntArray, for example. Array can't
        val newIds: List<ID>,
        val changes: Map<ID, FldSet<SCH>>
)

fun <T> eagerValue(): FetchValue<T, T, List<T>> = TODO()
fun <S : Schema<S>, D> eagerStruct(): FetchStruct<S, Nothing, D, Struct<S>, List<Struct<S>>> = TODO()

fun <T> lazyValue(): FetchValue<T, T, LazyList<T>> = TODO()
fun <S : Schema<S>, D> lazyStruct(): FetchStruct<S, Nothing, D, PropertyStruct<S>, LazyList<PropertyStruct<S>>> = TODO()

fun <T> observableValue(/*todo dependencies*/): FetchValue<T, Property<T>, Property<LazyList<T>>> = TODO()
fun <S : Schema<S>, D, ID : IdBound> observableStruct(idName: String, idType: DataType.Simple<ID>/*todo dependencies*/): FetchStruct<S, ID, D, PropertyStruct<S>, DiffProperty<LazyList<PropertyStruct<S>>, D>> = TODO()

fun <T> CoroutineContext.asyncValue(): FetchValue<T, Deferred<T>, Deferred<List<T>>> = TODO()
fun <S : Schema<S>, D> CoroutineContext.asyncStruct(): FetchStruct<S, Nothing, D, Deferred<Struct<S>>, Deferred<List<Struct<S>>>> = TODO()

fun <T> cellCallback(cb: (T) -> Unit): FetchValue<T, Unit, Nothing> = TODO()
fun <T> colCallback(cb: (List<T>) -> Unit): FetchValue<T, Nothing, Unit> = TODO()
fun <S : Schema<S>, D> rowCallback(cb: (Struct<S>) -> Unit): FetchStruct<S, Nothing, D, Unit, Nothing> = TODO()
fun <S : Schema<S>, D> gridCallback(cb: (List<Struct<S>>) -> Unit): FetchStruct<S, Nothing, D, Nothing, Unit> = TODO()

interface LazyList<out E> : List<E>


object User : Schema<User>() {
    val Name = "nam" let string
    val Email = "email" let string
}
suspend fun CoroutineContext.smpl(s: Session) {
    val name = s
            .rawQuery("SELECT nam FROM users LIMIT 1")
            .cell(string, asyncValue())
            .await()

    val names = s
            .rawQuery("SELECT nam FROM users")
            .col(string, asyncValue())
            .await()

    val explicit = s
            .rawQuery("SELECT nam, email FROM users LIMIT 1")
            .row(User, BindBy.Name, asyncStruct<User, FldSet<User>>())
    //         Remove explicit type arguments? ^^^^^^^^^^^^^^^^^^^^ Alt+Enter:
    val infer = s
            .rawQuery("SELECT nam, email FROM users LIMIT 1")
            .row(User, BindBy.Name, asyncStruct())

    /*val struct = s
            .rawQuery("SELECT nam, email FROM users LIMIT 1")
            .row(user, BindBy.Name, asyncStruct())
            .await()

    val structs = s
            .rawQuery("SELECT nam, email FROM users")
            .grid(user, BindBy.Name, asyncStruct())
            .await()*/
}

/**
 * Represents a database session specialized for a certain [Table].
 * {@implNote [Manager] supertype is used by [ManagedProperty] instances}
 */
interface Dao<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>> : Manager<SCH, Transaction, ID> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<SCH>, order: Array<out Order<SCH>>/* TODO: prefetch */): Property<List<REC>> // TODO FetchStruct | group by | having
    // todo joins
    fun count(condition: WhereCondition<SCH>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition
}

/**
 * Calls [block] within transaction passing [Transaction] which has functionality to create, mutate, remove [Record]s.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
inline fun <R> Session.withTransaction(block: Transaction.() -> R): R {
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
            source.fields.intersectMutable(fields).also { intersect ->
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

