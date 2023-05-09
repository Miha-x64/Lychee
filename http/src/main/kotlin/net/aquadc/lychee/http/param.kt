@file:JvmName("Params")
@file:Suppress("NOTHING_TO_INLINE") // pass-through
package net.aquadc.lychee.http.param
// Many declarations, let them ^^^^^ be in a separate package

import net.aquadc.lychee.http.Get
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.string
import java.io.InputStream

/**
 * Parameter role in HTTP request.
 */
sealed class Param<T>

/**
 * “Extracorporeal” parameter is sent not within request body.
 * Some methods such as [Get] require all parameters to be “extracorporeal”.
 */
sealed class ExtracorpParam<T> : Param<T>()

/**
 * “Link” parameters can be expressed through HTML links. No headers or bodies allowed.
 * @see Path
 * @see Query
 * @see QueryParams
 */
sealed class LinkParam<T> : ExtracorpParam<T>()

/**
 * HTML forms can send fields and multipart
 * but cannot send headers and request body.
 */
//interface HtmlFormParam TODO HTML forms

// copy-paste of Retrofit2 parameters https://square.github.io/retrofit/2.x/retrofit/

// URL

// unclear semantics: should it be relative to base? Or how to parse it at server-side?
//object Url : ExtracorpParam<CharSequence>()

/**
 * Path parameter. In `/user/{id}/profile`, {id} represents path parameter named "id".
 * @param name must be unique within an endpoint
 * @param type describes how to encode/decode the value
 */
class Path<T>(@JvmField val name: CharSequence, @JvmField val type: DataType.NotNull.Simple<T>) : LinkParam<T>()
inline fun Path(name: CharSequence): Path<String> = Path(name, string)

// Query string

/**
 * A query parameter (?name=value&name=value&justName)
 * @param name name of query parameter. Must be unique within an endpoint
 * @param type type of value. If `null`, means “send key with no value for `true`, don't send for `false`”
 */
class Query<T> @PublishedApi internal constructor(
    @JvmField val name: CharSequence,
    @JvmField val type: DataType<T>? //= simple | nullable(simple) | collection(simple)
) : LinkParam<T>()
inline fun Query(name: CharSequence): Query<String> = Query(name, string)
inline fun <T> Query(name: CharSequence, valueType: DataType.NotNull.Simple<T>): Query<T> = Query(name, valueType as DataType<T>)
inline fun <T : Any> Query(name: CharSequence, valueType: DataType.Nullable<T, DataType.NotNull.Simple<T>>): Query<T?> = Query(name, valueType as DataType<T?>)
inline fun <C, E, DE : DataType.NotNull.Simple<E>> Query(name: CharSequence, valueType: DataType.NotNull.Collect<C, E, DE>): Query<C> = Query(name, valueType as DataType<C>)
// note: just pass empty collection instead of nullable one; just don't add element to a collection instead of adding null
inline fun QueryPresence(name: CharSequence): Query<Boolean> = Query(name, null as DataType<Boolean>?)

/**
 * Flexible but ugly way to pass anything which cannot be described with normal [Query] params.
 */
object QueryParams : LinkParam<Collection<Pair<CharSequence, CharSequence?>>>()

// TODO class QueryStruct<T, SCH : Schema<SCH>>(@JvmField val type: DataType.NotNull.Partial<T, SCH>, naming: NamingConvention) : UrlParam<T, T>()

// form-data fields (copy-paste of Query except it extends Param, not LinkParam)

/**
 * A form-data parameter (name=value&name=value&justName)
 */
class Field<T> @PublishedApi internal constructor(
    @JvmField val name: CharSequence,
    @JvmField val type: DataType<T>//= simple | nullable(simple) | collection(simple)
) : Param<T>()
inline fun Field(name: CharSequence): Field<String> = Field(name, string)
inline fun <T> Field(name: CharSequence, valueType: DataType.NotNull.Simple<T>): Field<T> = Field(name, valueType as DataType<T>)
inline fun <T : Any> Field(name: CharSequence, valueType: DataType.Nullable<T, DataType.NotNull.Simple<T>>): Field<T?> = Field(name, valueType as DataType<T?>)
inline fun <C, E, DE : DataType.NotNull.Simple<E>> Field(name: CharSequence, valueType: DataType.NotNull.Collect<C, E, DE>): Field<C> = Field(name, valueType as DataType<C>)
// note: just pass empty collection instead of nullable one; just don't add element to a collection instead of adding null

/**
 * Flexible but ugly way to pass anything which cannot be described with normal [Field] params.
 */
object Fields : Param<Collection<Pair<CharSequence, CharSequence>>>()

// TODO class FieldStruct<T, SCH : Schema<SCH>>(@JvmField val type: DataType.NotNull.Partial<T, SCH>, naming: NamingConvention) : Param<T, T>()

// headers

class Header<T> @PublishedApi internal constructor(
    @JvmField val name: CharSequence,
    @JvmField val type: DataType<T> //= Simple | Nullable<Simple>
) : ExtracorpParam<T>()
inline fun Header(name: CharSequence): Header<String> = Header(name, string)
inline fun <T> Header(name: CharSequence, type: DataType.NotNull.Simple<T>): Header<T> = Header(name, type as DataType<T>)
inline fun <T : Any> Header(name: CharSequence, type: DataType.Nullable<T, DataType.NotNull.Simple<T>>): Header<T?> = Header(name, type as DataType<T?>)

object Headers : ExtracorpParam<Collection<Pair<CharSequence, CharSequence>>>()

// Request body

abstract class Body<T>(
    @JvmField val mediaType: CharSequence?
) : Param<T>() {
    open fun contentLength(value: T): Long = -1
    abstract fun stream(value: T): InputStream
    abstract fun fromStream(estimateSize: Long, stream: InputStream): T
}

class Part<T>(
    @JvmField val name: CharSequence,
    @JvmField val transferEncoding: CharSequence,
    @JvmField val body: Body<T>,
    @JvmField val filename: (T) -> CharSequence?
) : Param<T>()
inline fun <T> Part(name: CharSequence, body: Body<T>, noinline filename: (T) -> CharSequence?): Part<T> =
    Part<T>(name, "binary", body, filename)

class Parts<T>(
    @JvmField val transferEncoding: CharSequence = "binary",
    @JvmField val body: Body<T>,
    @JvmField val filename: (T) -> CharSequence?
) : Param<Collection<Pair<CharSequence, T>>>()
inline fun <T> Parts(body: Body<T>, noinline filename: (T) -> CharSequence?): Parts<T> =
    Parts<T>("binary", body, filename)

// strictly speaking, not a 'classic' parameter, but used together:

class Resp<T> internal constructor()
@PublishedApi @JvmField internal val resp = Resp<Nothing>()
inline fun <T> Response(): Resp<T> = resp as Resp<T>
