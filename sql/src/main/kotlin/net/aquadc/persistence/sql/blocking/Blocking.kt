@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.CloseableIterator
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
import net.aquadc.persistence.type.nothing
import net.aquadc.properties.function.just


object Eagerly : ProhibitCellsAndColsOfCollectionAndPartialTypes() {

    @JvmOverloads inline fun <CUR, R> cell(
        returnType: Ilk<out R, *>, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> =
        FetchCellEagerly(returnType, orElse)

    fun <CUR, R> col(
        elementType: Ilk<out R, *>,
    ): Fetch<CUR, List<R>> =
        FetchColLazily<CUR, _>(elementType).collect()

    @Suppress(
        "ONLY_ONE_CLASS_BOUND_ALLOWED", "INCONSISTENT_TYPE_PARAMETER_BOUNDS", // https://youtrack.jetbrains.com/issue/KT-209/
        "UNCHECKED_CAST", // (FetchStructEagerly as Fetch)::R = Struct<SCH> | orElse::R
    )
    @JvmOverloads inline fun <CUR, SCH, R> struct(
        table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> where
            SCH : Schema<SCH>, SCH : DataType.NotNull.Partial<out R, SCH> =
        FetchStructEagerly<SCH, CUR>(table, bindBy, orElse) as Fetch<CUR, R>

    @Deprecated("Type inference was hacked successfully.", ReplaceWith("this.struct(table, bindBy, orElse)"))
    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> structNullable(
        table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> Struct<SCH>? = just(null),
    ): Fetch<CUR, Struct<SCH>?> =
        struct(table, bindBy, orElse)

    fun <CUR, SCH : Schema<SCH>> structs(
        table: Table<SCH, *>, bindBy: BindBy
    ): Fetch<CUR, List<StructSnapshot<SCH>>> =
        @Suppress("UNCHECKED_CAST") // FetchStructsLazily::R::T = StructSnapshot when transient=false
        (FetchStructsLazily<CUR, _>(table, bindBy, false) as Fetch<CUR, CloseableIterator<StructSnapshot<SCH>>>)
            .collect()

    inline fun <CUR> execute(): Exec<CUR, Unit> =
        ExecuteForUnit as Exec<CUR, Unit>

    inline fun <CUR> executeForRowCount(): Exec<CUR, Int> =
        ExecuteForRowCount as Exec<CUR, Int>

    inline fun <CUR, T, DT : DataType.NotNull.Simple<T>> executeForInsertedKey(pkType: Ilk<T, DT>): Exec<CUR, T> =
        ExecuteEagerlyFor(pkType.also { check(it !== nothing) })
            as Exec<CUR, T>
}

object Lazily : ProhibitCellsAndColsOfCollectionAndPartialTypes() {

    @JvmOverloads fun <CUR, R> cell(
        returnType: Ilk<out R, *>, orElse: () -> R = throwNse,
    ): Fetch<CUR, Lazy<R>> =
        FetchCellEagerly<CUR, R>(returnType, orElse).lazy()

    inline fun <CUR, R> col(
        elementType: Ilk<out R, *>,
    ): Fetch<CUR, CloseableIterator<R>> =
        FetchColLazily(elementType)

    @Suppress(
        "ONLY_ONE_CLASS_BOUND_ALLOWED", "INCONSISTENT_TYPE_PARAMETER_BOUNDS", // https://youtrack.jetbrains.com/issue/KT-209/
        "UNCHECKED_CAST", // (FetchStructEagerly as Fetch)::R = Struct<SCH> | orElse::R
    )
    @JvmOverloads fun <CUR, SCH, R> struct(
        table: Table<SCH, *>, bindBy: BindBy, orElse: () -> R = throwNse,
    ): Fetch<CUR, Lazy<R>> where
            SCH : Schema<SCH>, SCH : DataType.NotNull.Partial<out R, SCH> =
        Eagerly.struct<CUR, SCH, R>(table, bindBy, orElse)
            .lazy()

    @Deprecated("Type inference was hacked successfully.", ReplaceWith("this.struct(table, bindBy, orElse)"))
    @JvmOverloads inline fun <CUR, SCH : Schema<SCH>> structNullable(
        table: Table<SCH, *>, bindBy: BindBy, noinline orElse: () -> Struct<SCH>? = just(null),
    ): Fetch<CUR, Lazy<Struct<SCH>?>> =
        struct(table, bindBy, orElse)

    inline fun <CUR, SCH : Schema<SCH>> structs(
        table: Table<SCH, *>, bindBy: BindBy
    ): Fetch<CUR, CloseableIterator<Struct<SCH>>> =
        FetchStructsLazily(table, bindBy, false)

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
        table: Table<SCH, *>, bindBy: BindBy,
    ): Fetch<CUR, CloseableIterator<Struct<SCH>>> =
        FetchStructsLazily(table, bindBy, true)
}

@Suppress("unused") // all parameters are used to maintain matching signature
abstract class ProhibitCellsAndColsOfCollectionAndPartialTypes internal constructor() {

    @Deprecated("single cell can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    @JvmOverloads inline fun <CUR, R> cell(
        returnType: DataType.NotNull.Collect<out R, *, *>, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> = throw AssertionError()

    @Deprecated("single cell can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    @JvmOverloads inline fun <CUR, R> cell(
        returnType: DataType.NotNull.Partial<out R, *>, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> = throw AssertionError()

    @Deprecated("single cell can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    @JvmOverloads inline fun <CUR, R : Any> cell(
        returnType: DataType.Nullable<out R, DataType.NotNull.Collect<out R, *, *>>, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> = throw AssertionError()

    @JvmName("nsCell")
    @Deprecated("single cell can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    @JvmOverloads inline fun <CUR, R : Any> cell(
        returnType: DataType.Nullable<out R, DataType.NotNull.Partial<out R, *>>, noinline orElse: () -> R = throwNse,
    ): Fetch<CUR, R> = throw AssertionError()

    @Deprecated("single column can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    inline fun <CUR, R> col(elementType: DataType.NotNull.Collect<out R, *, *>): Fetch<CUR, List<R>> =
        throw AssertionError()

    @Deprecated("single column can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    inline fun <CUR, R> col(elementType: DataType.NotNull.Partial<out R, *>): Fetch<CUR, List<R>> =
        throw AssertionError()

    @Deprecated("single column can't hold a Collection unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    inline fun <CUR, R : Any> col(elementType: DataType.Nullable<out R, DataType.NotNull.Collect<out R, *, *>>): Fetch<CUR, List<R>> =
        throw AssertionError()

    @JvmName("nsCol")
    @Deprecated("single column can't hold a Partial/Struct unless nativeType: Ilk is used", level = DeprecationLevel.ERROR)
    inline fun <CUR, R : Any> col(elementType: DataType.Nullable<out R, DataType.NotNull.Partial<out R, *>>): Fetch<CUR, List<R>> =
        throw AssertionError()

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
