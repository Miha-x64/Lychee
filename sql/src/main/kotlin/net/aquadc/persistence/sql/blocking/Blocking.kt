@file:Suppress("NOTHING_TO_INLINE")
package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.inflate
import net.aquadc.persistence.sql.row
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleNullable
import java.io.InputStream
import java.sql.ResultSet
import java.sql.SQLFeatureNotSupportedException

/**
 * SQL session tied to blocking API with cursors of type [CUR].
 */
interface Blocking<CUR : AutoCloseable> {
    // Android SQLite API has special methods for single-cell selections
    fun <T> cell(query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>, type: DataType<T>): T

    fun select(query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>, expectedCols: Int): CUR

    fun sizeHint(cursor: CUR): Int
    fun next(cursor: CUR): Boolean

    fun <T> cellAt(cursor: CUR, col: Int, type: DataType<T>): T
    fun rowByName(cursor: CUR, columns: Array<out StoredNamedLens<*, *, *>>): Array<Any?>
    fun rowByPosition(cursor: CUR, columns: Array<out StoredNamedLens<*, *, *>>): Array<Any?>
}

object Eager {
    inline fun <CUR : AutoCloseable, R> cell(returnType: DataType.Simple<R>): Fetch<Blocking<CUR>, R> =
            FetchCellEagerly(returnType)

    inline fun <CUR : AutoCloseable, R : Any> cell(returnType: SimpleNullable<R>): Fetch<Blocking<CUR>, R?> =
            FetchCellEagerly(returnType)

    inline fun <CUR : AutoCloseable, R> col(elementType: DataType.Simple<R>): Fetch<Blocking<CUR>, List<R>> =
            FetchColEagerly(elementType)

    inline fun <CUR : AutoCloseable, R : Any> col(elementType: SimpleNullable<R>): Fetch<Blocking<CUR>, List<R?>> =
            FetchColEagerly(elementType)

    inline fun <CUR : AutoCloseable, SCH : Schema<SCH>> struct(table: Table<SCH, *, *>, bindBy: BindBy): Fetch<Blocking<CUR>, StructSnapshot<SCH>> =
            FetchStructEagerly(table, bindBy)

    inline fun <CUR : AutoCloseable, SCH : Schema<SCH>> structList(table: Table<SCH, *, *>, bindBy: BindBy): Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> =
            FetchStructListEagerly(table, bindBy)

    inline fun cellByteStream(): Fetch<Blocking<ResultSet>, InputStream> =
            InputStreamFromResultSet //         ^^^^^^^^^ JDBC-only. Not supported by Android SQLite
}

@PublishedApi internal class FetchCellEagerly<CUR : AutoCloseable, R>(
        private val rt: DataType<R>
) : Fetch<Blocking<CUR>, R> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): R =
            from.cell(query, argumentTypes, arguments, rt)
}

@PublishedApi internal class FetchColEagerly<CUR : AutoCloseable, R>(
        private val et: DataType<R>
) : Fetch<Blocking<CUR>, List<R>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<R> {
        val cur = from.select(query, argumentTypes, arguments, 1)
        try {
            return if (from.next(cur)) {
                val first = from.cellAt(cur, 0, et)
                if (from.next(cur)) {
                    ArrayList<R>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.cellAt(cur, 0, et)) while (from.next(cur))
                    }
                } else listOf(first)
            } else emptyList()
        } finally {
            cur.close()
        }
    }
}

@PublishedApi internal class FetchStructEagerly<SCH : Schema<SCH>, CUR : AutoCloseable>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, StructSnapshot<SCH>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): StructSnapshot<SCH> {
        val cur = from.select(query, argumentTypes, arguments, table.columns.size)
        try {
            check(from.next(cur))
            val values = from.row(cur, table.columnsMappedToFields, bindBy)
            check(!from.next(cur)) // single row expected
            inflate(table.recipe, values, 0, 0, 0)
            @Suppress("UNCHECKED_CAST")
            return values[0] as StructSnapshot<SCH>
        } finally {
            cur.close()
        }
    }
}

@PublishedApi internal class FetchStructListEagerly<CUR : AutoCloseable, SCH : Schema<SCH>>(
        private val table: Table<SCH, *, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> {

    override fun fetch(
            from: Blocking<CUR>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): List<StructSnapshot<SCH>> {
        val cols = table.columnsMappedToFields
        val recipe = table.recipe

        val cur = from.select(query, argumentTypes, arguments, cols.size)
        try {
            return if (from.next(cur)) {
                val first = from.mapRow(cur, cols, recipe)
                if (from.next(cur)) {
                    ArrayList<StructSnapshot<SCH>>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.mapRow(cur, cols, recipe)) while (from.next(cur))
                    }
                } else listOf(first)
            } else emptyList()
        } finally {
            cur.close()
        }
    }

    private fun Blocking<CUR>.mapRow(cur: CUR, cols: Array<out StoredNamedLens<SCH, *, *>>, recipe: Array<out Table.Nesting>): StructSnapshot<SCH> {
        val firstValues = row(cur, cols, bindBy); inflate(recipe, firstValues, 0, 0, 0)
        @Suppress("UNCHECKED_CAST")
        return firstValues[0] as StructSnapshot<SCH>
    }
}

@PublishedApi internal object InputStreamFromResultSet : Fetch<Blocking<ResultSet>, InputStream> {

    override fun fetch(
            from: Blocking<ResultSet>, query: String, argumentTypes: Array<out DataType.Simple<*>>, arguments: Array<out Any>
    ): InputStream =
            from.select(query, argumentTypes, arguments, 1).let {
                check(it.next())
                try {
                    it.getBlob(0).binaryStream // Postgres-JDBC supports this, SQLite-JDBC doesn't
                } catch (e: SQLFeatureNotSupportedException) {
                    it.getBinaryStream(0) // this is typically just in-memory :'(
                }
            }
}

//fun <T> lazyValue(): FetchValue<BlockingSession, T, T, LazyList<T>> = TODO()
//fun <SCH : Schema<SCH>, D> lazyStruct(): FetchStruct<BlockingSession, SCH, Nothing, D, PropertyStruct<S>, LazyList<PropertyStruct<S>>> = TODO()

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

class ListChanges<SCH : Schema<SCH>, ID : IdBound>(
        val oldIds: List<ID>, // List could wrap IntArray, for example. Array can't
        val newIds: List<ID>,
        val changes: Map<ID, FldSet<SCH>>
)

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
