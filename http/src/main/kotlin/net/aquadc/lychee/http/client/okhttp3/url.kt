@file:JvmName("UrlTemplates")
package net.aquadc.lychee.http.client.okhttp3

import net.aquadc.lychee.http.Endpoint
import net.aquadc.lychee.http.Endpoint0
import net.aquadc.lychee.http.Endpoint1
import net.aquadc.lychee.http.Endpoint2
import net.aquadc.lychee.http.Endpoint3
import net.aquadc.lychee.http.Endpoint4
import net.aquadc.lychee.http.Endpoint5
import net.aquadc.lychee.http.Endpoint6
import net.aquadc.lychee.http.Endpoint7
import net.aquadc.lychee.http.Endpoint8
import net.aquadc.lychee.http.Get
import net.aquadc.lychee.http.param.LinkParam
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.type.DataType
import okhttp3.HttpUrl
import java.net.URLEncoder


fun Endpoint0<Get, *>.url(
    baseUrl: CharSequence?,
    fragment: CharSequence? = null
): CharSequence =
    url(baseUrl, this, null)
        .let { if (fragment == null) it else it.newBuilder().fragment(fragment.toString()).build() }
        .toString()

fun <T> Endpoint1<Get, out LinkParam<T>, *>.url(
    baseUrl: CharSequence?,
    arg: T,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf<Any?>(arg), baseUrl, fragment)

fun <T1, T2> Endpoint2<Get, *, out LinkParam<T1>, out LinkParam<T2>, *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2), baseUrl, fragment)

fun <T1, T2, T3> Endpoint3<Get, *, out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3), baseUrl, fragment)

fun <T1, T2, T3, T4> Endpoint4<Get, *,
    out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, out LinkParam<T4>,
    *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3, arg4: T4,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3, arg4), baseUrl, fragment)

fun <T1, T2, T3, T4, T5> Endpoint5<Get, *,
    out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, out LinkParam<T4>, out LinkParam<T5>,
    *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3, arg4, arg5), baseUrl, fragment)

fun <T1, T2, T3, T4, T5, T6> Endpoint6<Get, *,
    out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, out LinkParam<T4>, out LinkParam<T5>, out LinkParam<T6>,
    *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3, arg4, arg5, arg6), baseUrl, fragment)

fun <T1, T2, T3, T4, T5, T6, T7> Endpoint7<Get, *,
    out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, out LinkParam<T4>,
    out LinkParam<T5>, out LinkParam<T6>, out LinkParam<T7>,
    *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3, arg4, arg5, arg6, arg7), baseUrl, fragment)

fun <T1, T2, T3, T4, T5, T6, T7, T8> Endpoint8<Get, *,
    out LinkParam<T1>, out LinkParam<T2>, out LinkParam<T3>, out LinkParam<T4>,
    out LinkParam<T5>, out LinkParam<T6>, out LinkParam<T7>, out LinkParam<T8>,
    *>.url(
    baseUrl: CharSequence?,
    arg1: T1, arg2: T2, arg3: T3, arg4: T4, arg5: T5, arg6: T6, arg7: T7, arg8: T8,
    fragment: CharSequence? = null
): CharSequence =
    buildN(arrayOf(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8), baseUrl, fragment)

private fun Endpoint<Get, *>.buildN(args: Array<*>, baseUrl: CharSequence?, fragment: CharSequence?): String =
    url(baseUrl, this, args)
        .newBuilder()
        .parameterized(this, args)
        .fragment(fragment?.toString())
        .toString()

// TODO: endpoint.url(baseUrl) => Function (what about fragment)
// TODO: allow relative URLs (so baseUrl could be "//example.com/" or "/")
// TODO: kotlinx.html integration

@JvmSynthetic internal fun url(baseUrl: CharSequence?, endpoint: Endpoint<*, *>, args: Array<out Any?>?): HttpUrl {
//  var urlArg: CharSequence? = null
    val urlTemplate = endpoint.urlTemplate/*?*/.let(::StringBuilder)
    endpoint.params.forEachIndexed { index, param ->
        val value = args!![index]
        when (param) {
//          Url -> urlArg = value as CharSequence
            is Path -> {
                val type = param.type as DataType.NotNull.Simple<Any?>
                urlTemplate.replacePathSegm(param.name, type.storeAsStr(value))
            }
        }
    }

    var url = baseUrl?.toString()?.let { HttpUrl.parse(it)!! }
    urlTemplate/*?*/.toString()/*?*/.let { url = (if (url == null) HttpUrl.parse(it) else url!!.resolve(it))!! }
//  could save some allocations by replacing `url.resolve(it)` with `url.newBuilder(it)` but the code becomes horrible
//  urlArg?.toString()?.let { url = (if (url == null) HttpUrl.parse(it) else url!!.resolve(it))!! }
    return url!!
}

private fun StringBuilder.replacePathSegm(path: CharSequence, with: String) {
    val what = "{$path}"
    val start = indexOf(what)
    if (start < 0) throw IllegalArgumentException("$this does not contain $what")
    replace(start, start + what.length, URLEncoder.encode(with, "UTF-8").replace("+", "%20"))
    // there's also a hard way to do this ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^:
    // https://github.com/square/retrofit/commit/a9c0512fa6f88933702bf0e12243f5a584c01f66
}

private val nullUrl = HttpUrl.parse("http://q/") // unused with non-null builder
private fun HttpUrl.Builder.parameterized(endpoint: Endpoint<*, *>, args: Array<out Any?>): HttpUrl.Builder {
    endpoint.params.forEachIndexed { index, param ->
        param as LinkParam<*>
        val value = args[index]
        when (param) {
            /*is Url, */is Path -> {} // already handled
            is Query ->
                addQuery(this, nullUrl, param, value)
            is QueryParams ->
                addQueryParams(this, nullUrl, value as Collection<Pair<CharSequence, CharSequence?>>)
        }!!
    }
    return this
}

@JvmSynthetic internal fun addQuery(dest: HttpUrl.Builder?, url: HttpUrl, param: Query<*>, value: Any?): HttpUrl.Builder? =
    when (val type = param.type) {
        null -> if (value == true) (dest ?: url.newBuilder()).also { builder ->
            builder.addQueryParameter(param.name.toString(), null)
        } else dest
        is DataType.Nullable<*, *> ->
            if (value == null) dest // value == null, return builder unchanged
            else (dest ?: url.newBuilder()).also { builder ->
                builder.addQueryParameter(param.name.toString(), (type as DataType.NotNull.Simple<Any?>).storeAsStr(value))
            }
        is DataType.NotNull.Simple<*> ->
            (dest ?: url.newBuilder()).also { builder ->
                builder.addQueryParameter(param.name.toString(), (type as DataType.NotNull.Simple<Any?>).storeAsStr(value))
            }
        is DataType.NotNull.Collect<*, *, *> ->
            (type as DataType.NotNull.Collect<Any?, *, *>)
                .store(value)
                .fatAsList()
                .takeIf(List<*>::isNotEmpty) // don't instantiate builder and iterator if not necessary
                ?.let { values ->
                    (dest ?: url.newBuilder()).also { builder ->
                        values.forEach { value ->
                            builder.addQueryParameter(param.name.toString(), (type as DataType.NotNull.Simple<Any?>).storeAsStr(value))
                        }
                    }
                } ?: dest // empty collection, return builder as is
        is DataType.NotNull.Partial<*, *> ->
            throw AssertionError()
    }

@JvmSynthetic internal fun addQueryParams(dest: HttpUrl.Builder?, url: HttpUrl, values: Collection<Pair<CharSequence, CharSequence?>>): HttpUrl.Builder? {
    if (values.isEmpty()) return dest
    val builder = dest ?: url.newBuilder()
    values.forEach { (key, value) ->
        builder.addQueryParameter(key.toString(), value?.toString())
    }
    return builder
}

@JvmSynthetic internal fun <T> DataType.NotNull.Simple<T>.storeAsStr(value: T): String =
    (if (hasStringRepresentation) storeAsString(value!!) else store(value!!)).toString()
