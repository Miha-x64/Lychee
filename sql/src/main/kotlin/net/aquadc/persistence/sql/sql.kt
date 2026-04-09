@file:[
    JvmName("Sql")
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.sql

import androidx.annotation.CheckResult
import net.aquadc.collections.InlineEnumSet
import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.SizedIterator
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import java.io.Closeable


/**
 * Common supertype for all primary keys.
 */
typealias IdBound = Any // Serializable in some frameworks


@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("Under construction.", RequiresOptIn.Level.WARNING)
annotation class ExperimentalSql

/**
 * A readable database or transaction.
 */
interface Database {

//    fun <SCH : Schema<SCH>, ID : IdBound> TODO
//        select(from: Table<SCH, ID>, what: FieldSet<SCH, *>, /*where: String?, whereArgs: Array<Any>?, orderBy: String?*/): CUR

    // fun observe()
}

/**
 * A readable database or transaction supporting free-form SQL queries.
 *
 * [android.content.ContentResolver] does not, that's why we have simpler [Database].
 */
interface SqlDatabase : Database {

    fun <T> cell(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        type: Ilk<out T, *>, orElse: () -> T,
    ): T {
        val iter = column(query, argumentTypes, sessionAndArguments, type)
        try {
            return if (iter.hasNext()) {
                val value = iter.next()
                check(!iter.hasNext()) {
                    "cursor returned ${if (iter is SizedIterator<*>) iter.size.toString() else ">1"} rows, 1 required"
                }
                value
            } else orElse()
        } finally {
            iter.close()
        }
    }

    fun <T> column(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        type: Ilk<out T, *>,
    ): CloseableIterator<T>

    fun <SCH : Schema<SCH>> rows(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        table: Table<SCH, *>, bindBy: BindBy, transient: Boolean,
    ): CloseableIterator<Struct<SCH>>

    fun <ID> execute(
        query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>, retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any?
}

interface SqlTransaction : SqlDatabase, Closeable

/**
 * A writable database or transaction.
 */
interface MutableDatabase : Database {

    /**
     * Insert [data] into a [table].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insert(table: Table<SCH, ID>, data: PartialStruct<SCH>): ID

    /**
     * Insert all the [data] into a table.
     * Iterators over __transient structs__ are welcome.
     */
    fun <SCH : Schema<SCH>, ID : IdBound> insertAll(table: Table<SCH, ID>, data: Iterator<PartialStruct<SCH>>) {
        for (struct in data)
            insert(table, struct)
        // overridden in Sessions, left as is in Transactions
    }

    /**
     * Patch [table] row #[id] with [patch].
     */
    fun <SCH : Schema<SCH>, ID : IdBound> update(table: Table<SCH, ID>, id: ID, patch: PartialStruct<SCH>)

    // TODO fun update(where)

    fun <SCH : Schema<SCH>, ID : IdBound> delete(table: Table<SCH, ID>, id: ID)

    // TODO fun delete(where)
}

/**
 * A writable database or transaction supporting free-form SQL queries.
 *
 * [android.content.ContentResolver] does not, that's why we have simpler [Database].
 */
interface MutableSqlDatabase : SqlDatabase, MutableDatabase

interface MutableSqlTransaction : SqlTransaction, MutableSqlDatabase {
    fun setSuccessful()
}

internal interface InternalTransaction : MutableSqlTransaction {
    fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    fun close(deliver: Boolean)
}

/**
 * A gateway into RDBMS.
 */
interface Session : MutableSqlDatabase, MemoryTrimmable, Closeable {

    /**
     * Opens a readable transaction.
     */
    fun read(): SqlTransaction

    /**
     * Opens a mutable transaction.
     */
    fun mutate(): MutableSqlTransaction

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
     * @return subscription handle which removes [listener] when [closed][Closeable.close].
     */
    @CheckResult fun observe(vararg subject: TriggerSubject, listener: (TriggerReport) -> Unit): Closeable

    override fun close() // rm 'throws IOException`

}

interface MemoryTrimmable {
    /**
     * Trim memory usage, e.g. wipe some caches.
     * The [level] is according to [android.content.ComponentCallbacks2.TrimMemoryLevel]:
     * >= 20 ⇒ background task, not important
     * >= 40 ⇒ cached, not running
     * @return estimated number of bytes freed
     */
    fun trimMemory(level: Int): Int
}

// TODO: observe(DEFERRED)


inline fun <SCH : Schema<SCH>, ID : IdBound> MutableSqlTransaction.insertAll(table: Table<SCH, ID>, data: Iterable<Struct<SCH>>): Unit =
    insertAll(table, data.iterator())

inline fun <T, DT : DataType<T>> nativeType(name: CharSequence, type: DT): Ilk<T, DT> =
    NativeType(name, type)

inline fun <T, DT : DataType<T>> nativeType(name: CharSequence, type: DT, sqlType: Class<T>): Ilk<T, DT> =
    TODO()

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
