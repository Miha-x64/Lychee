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
@Deprecated("Record observability is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
typealias SqlProperty<T> = TransactionalProperty<Transaction<*>, T>

@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("Under construction.", RequiresOptIn.Level.WARNING)
annotation class ExperimentalSql

/**
 * A gateway into RDBMS.
 */
interface Session<SRC> : Closeable {

    /**
     * Lazily creates and returns DAO for the given table.
     */
    @Deprecated("Query builder and record observability are poor, use SQL templates (session.query()=>function) instead.",
        level = DeprecationLevel.ERROR)
    operator fun <SCH : Schema<SCH>, ID : IdBound> get(table: Table<SCH, ID>): Nothing = throw AssertionError()

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

}


/**
 * Represents a database session specialized for a certain [Table].
 * {@implNote [Manager] supertype is used by [ManagedProperty] instances}
 */
@Deprecated("Query builder and record observability are poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
typealias Dao<SCH, ID> = Nothing

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

interface Transaction<SRC> : Closeable {

    val mySession: Session<SRC>

    /**
     * Insert [data] into a [table].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>/*todo patch: Partial*/): ID

    /**
     * Insert all the [data] into a table.
     * Iterators over __transient structs__ are welcome.
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<Struct<SCH>>/*todo patch: Partial*/) {
        for (struct in data)
            insert(table, struct)
    }
    // TODO emulate slow storage!

    fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID)

    /**
     * Clear the whole table.
     * This may be implemented either as `DELETE FROM table` or `TRUNCATE table`.
     */
    fun truncate(table: Table<*, *>)

    fun setSuccessful()

}

inline fun <T, DT : DataType<T>> nativeType(name: CharSequence, type: DT): Ilk<T, DT> =
    NativeType(name, type)

inline fun <T, DT : DataType<T>> nativeType(
    name: CharSequence,
    type: DT,
    crossinline store: (T) -> Any?,
    crossinline load: (Any?) -> T
): Ilk<T, DT> =
    object : NativeType<T, DT>(name, type) {
        override fun invoke(p1: T): Any? = store(p1)
        override fun back(p: Any?): T = load(p)
    }
