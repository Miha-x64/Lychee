@file:JvmName("Blocking")
package net.aquadc.persistence.sql.blocking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import net.aquadc.persistence.sql.AsyncStruct
import net.aquadc.persistence.sql.FetchStruct
import net.aquadc.persistence.sql.FetchValue
import net.aquadc.persistence.sql.IdBound
import net.aquadc.persistence.sql.LazyList
import net.aquadc.persistence.sql.async.AsyncList
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.Property
import net.aquadc.properties.diff.DiffProperty
import net.aquadc.properties.persistence.PropertyStruct

/**
 * Provides the same functionality as [net.aquadc.persistence.sql.Selection]
 * but tied to blocking API and not tied to any SQL query.
 */
interface BlockingSession {

}

fun <T> eagerValue(): FetchValue<BlockingSession, T, T, List<T>> = TODO()
fun <S : Schema<S>, D> eagerStruct(): FetchStruct<BlockingSession, S, Nothing, D, Struct<S>, List<Struct<S>>> = TODO()

fun <T> lazyValue(): FetchValue<BlockingSession, T, T, LazyList<T>> = TODO()
fun <S : Schema<S>, D> lazyStruct(): FetchStruct<BlockingSession, S, Nothing, D, PropertyStruct<S>, LazyList<PropertyStruct<S>>> = TODO()

fun <T> observableValue(/*todo dependencies*/): FetchValue<BlockingSession, T, Property<T>, Property<LazyList<T>>> = TODO()
fun <S : Schema<S>, D, ID : IdBound> observableStruct(idName: String, idType: DataType.Simple<ID>/*todo dependencies*/): FetchStruct<BlockingSession, S, ID, D, PropertyStruct<S>, DiffProperty<LazyList<PropertyStruct<S>>, D>> = TODO()

fun <T> CoroutineScope.asyncValue(): FetchValue<BlockingSession, T, Deferred<T>, Deferred<List<T>>> = TODO()
fun <S : Schema<S>, D> CoroutineScope.asyncStruct(): FetchStruct<BlockingSession, S, Nothing, D, Deferred<Struct<S>>, Deferred<List<Struct<S>>>> = TODO()

fun <T> CoroutineScope.lazyAsyncValue(): FetchValue<BlockingSession, T, Deferred<T>, AsyncList<T>> = TODO()
fun <S : Schema<S>, D> CoroutineScope.lazyAsyncStruct(): FetchStruct<BlockingSession, S, Nothing, D, AsyncStruct<S>, AsyncList<AsyncStruct<S>>> = TODO()

fun <T> cellCallback(cb: (T) -> Unit): FetchValue<BlockingSession, T, Unit, Nothing> = TODO()
fun <T> colCallback(cb: (List<T>) -> Unit): FetchValue<BlockingSession, T, Nothing, Unit> = TODO()
fun <S : Schema<S>, D> rowCallback(cb: (Struct<S>) -> Unit): FetchStruct<BlockingSession, S, Nothing, D, Unit, Nothing> = TODO()
fun <S : Schema<S>, D> gridCallback(cb: (List<Struct<S>>) -> Unit): FetchStruct<BlockingSession, S, Nothing, D, Nothing, Unit> = TODO()

