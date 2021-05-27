@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
import net.aquadc.persistence.CloseableStruct
import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Exec
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.throwNse
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.SimpleNullable
import net.aquadc.persistence.type.nothing
import net.aquadc.properties.function.just
import java.io.InputStream

/**
 * SQL session tied to blocking API with cursors of type [CUR].
 */
interface Blocking<CUR> {
    // Android SQLite API has special methods for single-cell selections
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
    fun sizeHint(cursor: CUR): Int
    fun next(cursor: CUR): Boolean

    fun <ID> execute(
        query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        transactionAndArguments: Array<out Any>, retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
    ): Any?

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

object Eagerly { // TODO support Ilk everywhere

    @JvmOverloads inline fun <CUR, R> cell(
        returnType: DataType.NotNull.Simple<out R>, noinline orElse: () -> R = throwNse
    ): Fetch<Blocking<CUR>, R> =
            FetchCellEagerly(returnType, orElse)

    @JvmOverloads inline fun <CUR, R : Any> cell(
            returnType: SimpleNullable<out R>, noinline orElse: () -> R = throwNse
    ): Fetch<Blocking<CUR>, R?> =
            FetchCellEagerly(returnType, orElse)

    inline fun <CUR, R> col(elementType: DataType.NotNull.Simple<R>): Fetch<Blocking<CUR>, List<R>> =
            FetchColEagerly(elementType)

    inline fun <CUR, R : Any> col(elementType: SimpleNullable<R>): Fetch<Blocking<CUR>, List<R?>> =
            FetchColEagerly(elementType)

    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> struct(
            table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> StructSnapshot<SCH> = throwNse
    ): Fetch<Blocking<CUR>, StructSnapshot<SCH>> =
            FetchStructEagerly<SCH, CUR>(table, bindBy, orElse) as Fetch<Blocking<CUR>, StructSnapshot<SCH>/*!!*/>
    // unfortunately, I don't know how to hack type inference so that
    // orElse: () -> R, where StructSnapshot<SCH> : R, or R super StructSnapshot<SCH>.
    // Theoretically, R could be Snapshot, Snapshot?, Struct, Struct?, Partial, Partial?, Any, Any?.
    // I hope that supporting Snapshot? is enough.
    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> structNullable(
            table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> StructSnapshot<SCH>? = just(null)
    ): Fetch<Blocking<CUR>, StructSnapshot<SCH>?> =
            FetchStructEagerly(table, bindBy, orElse)

    inline fun <CUR, SCH : Schema<SCH>> structs(
            table: Table<SCH, *>, bindBy: BindBy
    ): Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> =
            FetchStructListEagerly(table, bindBy)

    inline fun execute(): Exec<Blocking<*>, Unit> =
        ExecuteForUnit

    inline fun executeForRowCount(): Exec<Blocking<*>, Int> =
        ExecuteForRowCount

    inline fun <T, DT : DataType.NotNull.Simple<T>> executeForInsertedKey(pkType: Ilk<T, DT>): Exec<Blocking<*>, T> =
        ExecuteEagerlyFor(pkType).also { check(pkType !== nothing) }
            as Exec<Blocking<*>, T>
}

object Lazily {
    @JvmOverloads inline fun <CUR, R> cell(
        returnType: DataType.NotNull.Simple<out R>, noinline orElse: () -> R = throwNse
    ): Fetch<Blocking<CUR>, Lazy<R>> =
            FetchCellLazily(returnType, orElse)

    @JvmOverloads inline fun <CUR, R : Any> cell(
            returnType: SimpleNullable<out R>, noinline orElse: () -> R = throwNse
    ): Fetch<Blocking<CUR>, Lazy<R?>> =
            FetchCellLazily(returnType, orElse)

    inline fun <CUR, R> col(
            elementType: DataType.NotNull.Simple<R>
    ): Fetch<Blocking<CUR>, CloseableIterator<R>> =
            FetchColLazily(elementType)

    inline fun <CUR, R : Any> col(
            elementType: SimpleNullable<R>
    ): Fetch<Blocking<CUR>, CloseableIterator<R?>> =
            FetchColLazily(elementType)

    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> struct(
            table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> Struct<SCH> = throwNse
    ): Fetch<Blocking<CUR>, CloseableStruct<SCH>> =
            FetchStructLazily<SCH, CUR>(table, bindBy, orElse) as Fetch<Blocking<CUR>, CloseableStruct<SCH>/*!!*/>
    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> structNullable(
            table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> Struct<SCH>? = throwNse
    ): Fetch<Blocking<CUR>, CloseableStruct<SCH>?> =
            FetchStructLazily(table, bindBy, orElse)

    inline fun <CUR, SCH : Schema<SCH>> structs(
            table: Table<SCH, *>, bindBy: BindBy
    ): Fetch<Blocking<CUR>, CloseableIterator<Struct<SCH>>> =
            FetchStructListLazily<CUR, SCH>(table, bindBy, false)

    /**
     * A view on ResultSet/Cursor as an iterator over __transient Structs__.
     * A [Struct] is __transient__ when it is owned by an [Iterator].
     * Such a [Struct] is valid only until [Iterator.next] or [CloseableIterator.close] call.
     * Never store, collect, or let them escape the for-loop.
     * Sorting, finding min, max, distinct also won't work because
     * these operations require looking back at previous [Struct]s.
     * (Flat)mapping and filtering i.e. stateless intermediate operations are still OK.
     * Limiting, skipping, folding, reducing, counting,
     * and other stateful one-pass operations are also OK.
     * (But consider doing as much work as possible in SQL instead.)
     */
    inline fun <CUR, SCH : Schema<SCH>> transientStructs(
            table: Table<SCH, *>, bindBy: BindBy
    ): Fetch<Blocking<CUR>, CloseableIterator<Struct<SCH>>> =
            FetchStructListLazily<CUR, SCH>(table, bindBy, true)

    inline fun cellByteStream(): Fetch<Blocking<java.sql.ResultSet>, InputStream> =
            InputStreamFromResultSet //         ^^^^^^^^^ JDBC-only. Not supported by Android SQLite
}

//fun <T> observableValue(/*todo dependencies*/): FetchValue<BlockingSession, T, Property<T>, Property<LazyList<T>>> = TODO()
//fun <SCH : Schema<SCH>, D, ID : IdBound> observableStruct(idName: String, idType: DataType.Simple<ID>/*todo dependencies*/): FetchStruct<BlockingSession, SCH, ID, D, PropertyStruct<S>, DiffProperty<LazyList<PropertyStruct<S>>, D>> = TODO()

/*fun <T> CoroutineScope.asyncValue(): FetchValue<BlockingSession, T, Deferred<T>, Deferred<List<T>>> {
    launch {  }
}
fun <SCH : Schema<SCH>, D> CoroutineScope.asyncStruct(): FetchStruct<BlockingSession, SCH, Nothing, D, Deferred<Struct<S>>, Deferred<List<Struct<S>>>> = TODO()*/

/*fun <T> cellCallback(cb: (T) -> Unit): FetchValue<BlockingSession, T, Unit, Nothing> = TODO()
fun <T> colCallback(cb: (List<T>) -> Unit): FetchValue<BlockingSession, T, Nothing, Unit> = TODO()
fun <SCH : Schema<SCH>, D> rowCallback(cb: (Struct<S>) -> Unit): FetchStruct<BlockingSession, SCH, Nothing, D, Unit, Nothing> = TODO()
fun <SCH : Schema<SCH>, D> gridCallback(cb: (List<Struct<S>>) -> Unit): FetchStruct<BlockingSession, SCH, Nothing, D, Nothing, Unit> = TODO()*/

/*
fun <T> CoroutineScope.lazyAsyncValue(): FetchValue<BlockingSession, T, Deferred<T>, AsyncList<T>> = TODO()
fun <SCH : Schema<SCH>, D> CoroutineScope.lazyAsyncStruct(): FetchStruct<BlockingSession, SCH, Nothing, D, AsyncStruct<S>, AsyncList<AsyncStruct<S>>> = TODO()

interface AsyncStruct<SCH : Schema<SCH>>

interface AsyncIterator<out T> {
    suspend operator fun next(): T
    suspend operator fun hasNext(): Boolean
}
interface AsyncIterable<out T> {
    operator fun iterator(): AsyncIterator<T>
}
interface AsyncCollection<out E> : Iterable<E> {
    suspend /*val*/ fun size(): Int
    suspend /*operator*/ fun contains(element: @UnsafeVariance E): Boolean
    suspend fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
}
interface AsyncList<out E> {
    suspend /*operator*/ fun get(index: Int): E
    suspend fun indexOf(element: @UnsafeVariance E): Int
    suspend fun lastIndexOf(element: @UnsafeVariance E): Int
//    fun listIterator(): ListIterator<E>
//    fun listIterator(index: Int): ListIterator<E>
//    suspend fun subList(fromIndex: Int, toIndex: Int): List<E>
}
 */
