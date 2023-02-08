@file:[
    JvmName("Sql")
    OptIn(ExperimentalContracts::class)
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.sql

import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import org.intellij.lang.annotations.Language
import java.io.Closeable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Common supertype for all primary keys.
 */
typealias IdBound = Any // Serializable in some frameworks


@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("Under construction.", RequiresOptIn.Level.WARNING)
annotation class ExperimentalSql

/**
 * A gateway into RDBMS.
 */
interface Session<SRC> : Closeable {

    /**
     * Opens a transaction, allowing mutation of data.
     */
    fun beginTransaction(): Transaction<SRC>

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // effectively private and type-unsafe API
    fun <R> rawQuery(
        @Language("SQL") query: String,
    //  ^^^^^^^^^^^^^^^^ add Database Navigator to IntelliJ for SQL highlighting in String literals
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        argumentValues: Array<out Any>,
        fetch: Fetch<SRC, R>
    ): R

    /**
     * Registers trigger listener for all [subject]s.
     * A Session aims to deliver as few false-positives as possible but still:
     * * if the record was removed and another record with same primary key was inserted,
     *   [TriggerReport] will show that all columns were modified,
     * * if an UPDATE statement changes a column value and another UPDATE changes it back,
     *   [TriggerReport] will show this column as modified.
     * Assuming that several applications can share a single database
     * (even SQLite can have multiple processes or connections), this method adds listeners eagerly,
     * blocking until all current transactions finish, if any.
     * The thread which calls [listener] is not defined.
     * @return subscription handle which removes [listener] when [Closeable.close]d
     */
    @CheckResult fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable

    fun trimMemory()

    override fun close() // rm 'throws IOException`

}


/**
 * Calls [block] within transaction passing [Transaction] which has functionality to create, mutate, remove [Record]s.
 * In future will retry conflicting transaction by calling [block] more than once.
 */
inline fun <SRC, R> Session<SRC>.withTransaction(block: Transaction<SRC>.() -> R): R {
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
@RequiresApi(24) @JvmName("withTransaction")
fun <SRC> Session<SRC>.withTransaction4j(block: java.util.function.Consumer<Transaction<SRC>>) {
    val transaction = beginTransaction()
    try {
        block.accept(transaction)
        transaction.setSuccessful()
    } finally {
        transaction.close()
    }
}

interface Transaction<SRC> : Session<SRC>, Closeable {

    @Deprecated("Transaction implements Session now", ReplaceWith("this"))
    val mySession: Session<SRC>

    /**
     * Insert [data] into a [table].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID

    /**
     * Insert all the [data] into a table.
     * Iterators over __transient structs__ are welcome.
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<Struct<SCH>>/*todo patch: Partial*/) {
        for (struct in data)
            insert(table, struct)
    }

    /**
     * Patch [table] row #[id] with [patch].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>)

    // TODO emulate slow storage!

    fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID)

    /**
     * Clear the whole table.
     * This may be implemented either as `DELETE FROM table` or `TRUNCATE table`.
     */
    @Deprecated("TRUNCATE is not a CRUD operation. Also, it is not guaranteed to invoke triggers in some DBMSes. " +
        "You can TRUNCATE using Session.mutate() query at your own risk.", level = DeprecationLevel.ERROR)
    fun truncate(table: Table<*, *>): Unit = throw AssertionError()

    fun setSuccessful()

    override fun close() // rm 'throws IOException`

}

inline fun <SCH : Schema<SCH>, ID : IdBound> Transaction<*>.insertAll(table: Table<SCH, ID>, data: Iterable<Struct<SCH>>): Unit =
    insertAll(table, data.iterator())

inline fun <T, DT : DataType<T>> nativeType(name: CharSequence, type: DT): Ilk<T, DT> =
    NativeType(name, type)

inline fun <T, DT : DataType<T>, S> nativeType(
    name: CharSequence,
    type: DT,
    crossinline store: (T) -> S,
    crossinline load: (S) -> T
): Ilk<T, DT> =
    nativeType<Any?, T, DT, S>(name, type, { _, v -> store(v) }, { _, v -> load(v) })

inline fun <PL, T, DT : DataType<T>, S> nativeType(
    name: CharSequence,
    type: DT,
    crossinline store: (PL, T) -> S,
    crossinline load: (PL, S) -> T
): Ilk<T, DT> =
    @Suppress("UNCHECKED_CAST")
    object : NativeType<T, DT>(name, type) {
        override fun store(payload: Any?, value: T): Any? = store.invoke(payload as PL, value)
        override fun load(payload: Any?, value: Any?): T = load.invoke(payload as PL, value as S)
    }
