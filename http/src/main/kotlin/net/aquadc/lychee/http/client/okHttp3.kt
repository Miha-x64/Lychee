@file:JvmName("OkHttp3Templates")
@file:Suppress("NOTHING_TO_INLINE")
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
import net.aquadc.lychee.http.noCharSeqs
import net.aquadc.lychee.http.param.Field
import net.aquadc.lychee.http.param.Fields
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Part
import net.aquadc.lychee.http.param.Parts
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.lychee.http.param.Headers
import net.aquadc.lychee.http.param.Param
import net.aquadc.lychee.http.param.Body
import net.aquadc.lychee.http.param.Resp
//import net.aquadc.lychee.http.param.Url
import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.type.DataType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.Okio
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.FutureTask


inline fun <B, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint0<*, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): () -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint1<*, out Param<T>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint2<*, *, out Param<T1>, out Param<T2>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint3<*, *, out Param<T1>, out Param<T2>, out Param<T3>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, T4, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint4<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3, T4) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, T4, T5, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint5<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3, T4, T5) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, T4, T5, T6, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint6<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3, T4, T5, T6) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, T4, T5, T6, T7, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint7<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, out Param<T7>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3, T4, T5, T7) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)

inline fun <B, T1, T2, T3, T4, T5, T6, T7, T8, R> OkHttpClient.template(
    baseUrl: CharSequence?,
    endpoint: Endpoint8<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, out Param<T7>, out Param<T8>, B>,
    noinline execute: (OkHttpClient, Request, Resp<B>) -> R,
    headers: Array<out CharSequence> = noCharSeqs
): (T1, T2, T3, T4, T5, T7, T8) -> R =
    OkHttpMethod(this, baseUrl, endpoint, headers, execute)


@PublishedApi internal class OkHttpMethod<B, R>(
    private val client: OkHttpClient,
    private val baseUrl: CharSequence?,
    private val endpoint: Endpoint<*, B>,
    private val headers: Array<out CharSequence>,
    private val execute: (OkHttpClient, Request, Resp<B>) -> R
) : FuncXImpl<Any?, R>() {
    override fun invokeUnchecked(vararg arg: Any?): R {
//        var urlArg: CharSequence? = null
        val urlTemplate = endpoint.urlTemplate/*?*/.let(::StringBuilder)
        val params = endpoint.params
        params.forEachIndexed { index, param ->
            val value = arg[index]
            when (param) {
//                Url -> urlArg = value as CharSequence
                is Path -> {
                    val type = param.type as DataType.NotNull.Simple<Any?>
                    urlTemplate.replacePathSegm(param.name, type.storeAsStr(value))
                }
            }
        }

        var url = baseUrl?.toString()?.let { HttpUrl.parse(it)!! }
        urlTemplate/*?*/.toString()/*?*/.let { url = (if (url == null) HttpUrl.parse(it) else url!!.resolve(it))!! }
//        urlArg?.toString()?.let { url = (if (url == null) HttpUrl.parse(it) else url!!.resolve(it))!! }
        var urlBldr: HttpUrl.Builder? = null

        val request = Request.Builder()
        var body: RequestBody? = null
        var fields: FormBody.Builder? = null
        var multipart: MultipartBody.Builder? = null

        params.forEachIndexed { index, param ->
            val value = arg[index]
            when (param) {
                /*is Url, */is Path -> {} // already handled
                is Query ->
                    urlBldr = addQuery(urlBldr, url!!, param, value)
                is QueryParams ->
                    urlBldr = addQueryParams(urlBldr, url!!, value as Collection<Pair<CharSequence, CharSequence?>>)
                is Header ->
                    addHeader(request, param as Header<Any?>, value)
                is Headers ->
                    addHeaders(request, value as Collection<Pair<CharSequence, CharSequence>>)
                is Field ->
                    addField(fields ?: FormBody.Builder().also { fields = it }, param, value)
                is Fields ->
                    addFields(fields ?: FormBody.Builder().also { fields = it }, value as Collection<Pair<CharSequence, CharSequence>>)
                is Body ->
                    body = bodyFor(param as Body<Any?>, value)
                is Part<*, *> ->
                    addPart(multipart ?: MultipartBody.Builder().also { multipart = it }, param as Part<Any?, Any?>, value)
                is Parts<*> ->
                    addParts(multipart ?: MultipartBody.Builder().also { multipart = it }, param as Parts<Any?>, value as Collection<Pair<CharSequence, Any?>>)
            }!!
        }

        if (fields != null) {
            check(body == null)
            check(multipart == null) // todo lift restriction, MultipartBody.Builder#addFormDataPart
            body = fields!!.build()
            fields = null
        } else if (multipart != null) {
            check(body == null)
            body = multipart!!.build()
            multipart = null
        }

        for (i in headers.indices step 2) {
            request.addHeader(headers[i].toString(), headers[i + 1].toString())
        }

        return execute(
            client,
            request
                .method(endpoint.method.name, body)
                .url(urlBldr?.let { it.build()!! } ?: url!!)
                .build(),
            endpoint.response
        )
    }

    private fun addQuery(dest: HttpUrl.Builder?, url: HttpUrl, param: Query<*>, value: Any?): HttpUrl.Builder? =
        when (val type = param.type) {
            is DataType.Nullable<*, *> ->
                if (value == null) dest // value == null, return builder unchanged
                else (dest ?: url.newBuilder()).also { builder ->
                    addQuery(builder, param.name, type as DataType.NotNull.Simple<Any?>, value)
                }
            is DataType.NotNull.Simple<*> ->
                (dest ?: url.newBuilder()).also { builder ->
                    addQuery(builder, param.name, type as DataType.NotNull.Simple<Any?>, value)
                }
            is DataType.NotNull.Collect<*, *, *> ->
                (type as DataType.NotNull.Collect<Any?, *, *>)
                    .store(value)
                    .fatAsList()
                    .takeIf(List<*>::isNotEmpty) // don't instantiate builder and iterator if not necessary
                    ?.let { values ->
                        (dest ?: url.newBuilder()).also { builder ->
                            values.forEach { value ->
                                addQuery(builder, param.name, type as DataType.NotNull.Simple<Any?>, value)
                            }
                        }
                    } ?: dest // empty collection, return builder as is
            is DataType.NotNull.Partial<*, *> ->
                throw AssertionError()
        }
    private fun <T> addQuery(dest: HttpUrl.Builder, nameOrNull: CharSequence?, type: DataType.NotNull.Simple<T>, arg: T) {
        val valueOrName = type.storeAsStr(arg)
        if (nameOrNull != null) dest.addQueryParameter(nameOrNull.toString(), valueOrName)
        else dest.addQueryParameter(valueOrName, null)
    }
    private fun addQueryParams(dest: HttpUrl.Builder?, url: HttpUrl, values: Collection<Pair<CharSequence, CharSequence?>>): HttpUrl.Builder? {
        if (values.isEmpty()) return dest
        val builder = dest ?: url.newBuilder()
        values.forEach { (key, value) ->
            builder.addQueryParameter(key.toString(), value?.toString())
        }
        return builder
    }

    private fun <T> addHeader(dest: Request.Builder, param: Header<T>, value: T) {
        val type = param.type
        val unwrappedType = if (type is DataType.Nullable<*, *>) {
            if (value == null) return
            type.actualType as DataType.NotNull.Simple<T>
        } else type as DataType.NotNull.Simple<T>
        dest.addHeader(param.name.toString(), unwrappedType.storeAsStr(value))
    }
    private fun addHeaders(dest: Request.Builder, value: Collection<Pair<CharSequence, CharSequence>>) {
        // many collections are dumb enough to create iterator even for empty contents
        if (value.isNotEmpty())
            value.forEach { (name, value) ->
                dest.addHeader(name.toString(), value.toString())
            }
    }

    private fun addField(dest: FormBody.Builder, param: Field<*>, value: Any?) {
        when (val type = param.type) {
            is DataType.Nullable<*, *> ->
                if (value != null) addField(dest, param.name, type as DataType.NotNull.Simple<Any?>, value)
            is DataType.NotNull.Simple<*> ->
                addField(dest, param.name, type as DataType.NotNull.Simple<Any?>, value)
            is DataType.NotNull.Collect<*, *, *> ->
                (type as DataType.NotNull.Collect<Any?, *, *>)
                    .store(value)
                    .fatAsList()
                    .takeIf(List<*>::isNotEmpty) // don't instantiate iterator if not necessary
                    ?.let { values ->
                        values.forEach { value ->
                            addField(dest, param.name, type as DataType.NotNull.Simple<Any?>, value)
                        }
                    }
            is DataType.NotNull.Partial<*, *> ->
                throw AssertionError()
        }
    }
    private fun <T> addField(dest: FormBody.Builder, name: CharSequence, type: DataType.NotNull.Simple<T>, value: T) {
        dest.add(name.toString(), type.storeAsStr(value))
    }
    private fun addFields(dest: FormBody.Builder, fields: Collection<Pair<CharSequence, CharSequence>>) {
        if (fields.isNotEmpty()) fields.forEach { (name, value) ->
            dest.add(name.toString(), value.toString())
        }
    }

    private fun <T> bodyFor(param: Body<T>, value: T): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType =
                MediaType.parse(param.mediaType.toString())
            override fun contentLength(): Long =
                param.contentLength(value)
            override fun writeTo(sink: BufferedSink) {
                Okio.source(param.stream(value)).use(sink::writeAll)
            }
        }
    }

    private fun <T, B> addPart(dest: MultipartBody.Builder, param: Part<T, B>, value: T) {
        val name: CharSequence
        val body: B
        if (param.name == null) {
            value as Pair<CharSequence, B>
            name = value.first
            body = value.second
        } else {
            name = param.name
            body = value as B
        }
        addPart<B>(dest, name, param.transferEncoding.toString(), param.body, body)
    }
    private fun <T> addParts(dest: MultipartBody.Builder, param: Parts<T>, value: Collection<Pair<CharSequence, T>>) {
        if (value.isNotEmpty()) {
            val enc = param.transferEncoding.toString()
            value.forEach { (name, value) ->
                addPart(dest, name, enc, param.body, value)
            }
        }
    }
    private fun <B> addPart(dest: MultipartBody.Builder, name: CharSequence, enc: String, body: Body<B>, value: B) {
        dest.addPart(MultipartBody.Part.create(
            okhttp3.Headers.of( // accurate copy-paste of https://github.com/square/retrofit/blob/108fe23964b986107aed352ba467cd2007d15208/retrofit/src/main/java/retrofit2/RequestFactory.java#L675-L679
                "Content-Disposition", "form-data; name=\"$name$\"", // fixme encode? also could share StringBuilder
                "Content-Transfer-Encoding", enc),
            bodyFor(body, value)
        ))
    }

    private fun <T> DataType.NotNull.Simple<T>.storeAsStr(value: T): String =
        (if (hasStringRepresentation) storeAsString(value!!) else store(value!!)).toString()

    private fun StringBuilder.replacePathSegm(path: CharSequence, with: String) {
        val what = "{$path}"
        val start = indexOf(what)
        if (start < 0) throw IllegalArgumentException("$this does not contain $what")
        replace(start, start + what.length, URLEncoder.encode(with, "UTF-8").replace("+", "%20"))
        // there's also a hard way to do this ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^:
        // https://github.com/square/retrofit/commit/a9c0512fa6f88933702bf0e12243f5a584c01f66
    }
}

fun <T> blocking(
    parse: (Resp<T>, Response) -> T
): (OkHttpClient, Request, Resp<T>) -> T =
    { client, request, body ->
        parse(body, client.newCall(request).execute()) // just pass IOException through
    }

fun <T> future(
    executor: Executor, parse: (Resp<T>, Response) -> T
): (OkHttpClient, Request, Resp<T>) -> Future<T> =
    { client, request, body ->
        FutureTask(Callable { parse(body, client.newCall(request).execute()) }).also(executor::execute)
    }

fun <T> callback(
    parse: (Resp<T>, Response) -> T, callbackExecutor: Executor, callback: (T?, IOException?) -> Unit
): (OkHttpClient, Request, Resp<T>) -> Call =
    { client, request, body ->
        client.newCall(request).also { it.enqueue(object : Callback {
            override fun onResponse(call: Call?, response: Response): Unit =
                call(parse(body, response), null)
            override fun onFailure(call: Call?, e: IOException): Unit =
                call(null, e)
            private fun call(t: T?, e: IOException?): Unit =
                callbackExecutor.execute { callback(t, e) }
        }) }
    }
