@file:[
    JvmName("Sql")
    Suppress("NOTHING_TO_INLINE")
]
package net.aquadc.persistence.sql

import androidx.annotation.CheckResult
import net.aquadc.collections.InlineEnumSet
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
interface Source<CUR> {
    // TODO fun select(where, order by)

    fun sizeHint(cursor: CUR): Int
    fun next(cursor: CUR): Boolean

    fun <T> cellByName(cursor: CUR, name: CharSequence, type: Ilk<T, *>): T
    fun <T> cellAt(cursor: CUR, col: Int, type: Ilk<T, *>): T

    fun rowByName(cursor: CUR, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>): Array<Any?>
    fun rowByPosition(cursor: CUR, offset: Int, types: Array<out Ilk<*, *>>): Array<Any?>

    /**
     * Closes the given cursor.
     * [java.sql.ResultSet] is [AutoCloseable],
     * while [android.database.Cursor] is [java.io.Closeable].
     * [AutoCloseable] is more universal but requires Java 7 / Android SDK 19.
     * Let's support mammoth crap smoothly.
     */
    fun close(cursor: CUR)
}

/**
 * A readable database or transaction supporting free-form SQL queries.
 *
 * [android.content.ContentResolver] does not, that's why we have simpler [Source].
 */
interface FreeSource<CUR> : Source<CUR> {

    fun <T> cell(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        type: Ilk<out T, *>, orElse: () -> T
    ): T

    fun select(
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, sessionAndArguments: Array<out Any>,
        expectedCols: Int
    ): CUR

    fun <ID> execute(
        query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>, retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any?
}

interface ReadableTransaction<CUR> : FreeSource<CUR>, Closeable

/**
 * A writable database or transaction.
 */
interface Exchange<CUR> : Source<CUR> {

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
 * [android.content.ContentResolver] does not, that's why we have simpler [Source].
 */
interface FreeExchange<CUR> : FreeSource<CUR>, Exchange<CUR>

interface MutableTransaction<CUR> : ReadableTransaction<CUR>, FreeExchange<CUR> {
    fun setSuccessful()
}

interface InternalTransaction<SRC> : MutableTransaction<SRC> {
    fun <SCH : Schema<SCH>, ID : IdBound, T> fetchSingle(
        table: Table<SCH, ID>, colName: CharSequence, colType: Ilk<T, *>, id: ID
    ): T

    fun <SCH : Schema<SCH>, ID : IdBound> fetch(
        table: Table<SCH, ID>, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, id: ID
    ): Array<Any?>

    fun addTriggers(newbies: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    fun removeTriggers(victims: Map<Table<*, *>, InlineEnumSet<TriggerEvent>>)
    fun close(deliver: Boolean)
}

/**
 * A gateway into RDBMS.
 */
interface Session<CUR> : FreeExchange<CUR>, Closeable {

    /**
     * Opens a readable transaction.
     */
    fun read(): ReadableTransaction<CUR>

    /**
     * Opens a writable transaction.
     */
    fun mutate(): MutableTransaction<CUR>

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

// TODO: observe(DEFERRED)


inline fun <SCH : Schema<SCH>, ID : IdBound> MutableTransaction<*>.insertAll(table: Table<SCH, ID>, data: Iterable<Struct<SCH>>): Unit =
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
