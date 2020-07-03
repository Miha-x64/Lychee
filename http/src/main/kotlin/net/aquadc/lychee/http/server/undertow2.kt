@file:JvmName("Undertow2")
package net.aquadc.lychee.http.server.undertow2

import io.undertow.predicate.Predicate
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.form.FormData
import io.undertow.server.handlers.form.FormDataParser
import io.undertow.server.handlers.form.FormParserFactory
import io.undertow.util.HeaderMap
import io.undertow.util.PathTemplateMatch
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
import net.aquadc.lychee.http.HttpMethod
import net.aquadc.lychee.http.param.Body
import net.aquadc.lychee.http.param.Field
import net.aquadc.lychee.http.param.Fields
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Headers
import net.aquadc.lychee.http.param.Param
import net.aquadc.lychee.http.param.Part
import net.aquadc.lychee.http.param.Parts
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.persistence.each
import net.aquadc.persistence.fromBase64
import net.aquadc.persistence.type.DataType
import java.net.URLDecoder
import java.util.Deque


inline fun <R> RoutingHandler.add(
    endpoint: Endpoint0<*, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.() -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        exchange.respond(exchange.handler())
        exchange.endExchange()
    }, addHandler)

inline fun <T, R>
    RoutingHandler.add(
    endpoint: Endpoint1<*, out Param<T>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg) ->
            exchange.handler(arg as T)
        }
    }, addHandler)

inline fun <T1, T2, R>
    RoutingHandler.add(
    endpoint: Endpoint2<*, *, out Param<T1>, out Param<T2>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2) ->
            exchange.handler(arg1 as T1, arg2 as T2)
        }
    }, addHandler)

inline fun <T1, T2, T3, R>
    RoutingHandler.add(
    endpoint: Endpoint3<*, *, out Param<T1>, out Param<T2>, out Param<T3>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3)
        }
    }, addHandler)

inline fun <T1, T2, T3, T4, R>
    RoutingHandler.add(
    endpoint: Endpoint4<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3, T4) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3, arg4) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4)
        }
    }, addHandler)

inline fun <T1, T2, T3, T4, T5, R>
    RoutingHandler.add(
    endpoint: Endpoint5<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3, T4, T5) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3, arg4, arg5) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5)
        }
    }, addHandler)

inline fun <T1, T2, T3, T4, T5, T6, R>
    RoutingHandler.add(
    endpoint: Endpoint6<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3, T4, T5, T6) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3, arg4, arg5, arg6) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5, arg6 as T6)
        }
    }, addHandler)

inline fun <T1, T2, T3, T4, T5, T6, T7, R>
    RoutingHandler.add(
    endpoint: Endpoint7<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, out Param<T7>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3, T4, T5, T6, T7) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3, arg4, arg5, arg6, arg7) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5, arg6 as T6, arg7 as T7)
        }
    }, addHandler)

inline fun <T1, T2, T3, T4, T5, T6, T7, T8, R>
    RoutingHandler.add(
    endpoint: Endpoint8<*, *, out Param<T1>, out Param<T2>, out Param<T3>, out Param<T4>, out Param<T5>, out Param<T6>, out Param<T7>, out Param<T8>, R>,
    noinline respond: HttpServerExchange.(R) -> Unit,
    noinline respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory = DefaultFormParserFactory,
    noinline addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit = addHndlr,
    crossinline handler: HttpServerExchange.(T1, T2, T3, T4, T5, T6, T7, T8) -> R
): RoutingHandler =
    add(endpoint, HttpHandler { exchange ->
        gatherArgsAndInvoke(exchange, endpoint, respond, respondBadRequest, formParserFactory) {
            (arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) ->
            exchange.handler(arg1 as T1, arg2 as T2, arg3 as T3, arg4 as T4, arg5 as T5, arg6 as T6, arg7 as T7, arg8 as T8)
        }
    }, addHandler)

@PublishedApi @JvmField internal val DefaultFormParserFactory: FormParserFactory = FormParserFactory.builder().build()
@PublishedApi internal fun <M : HttpMethod<*>, R> RoutingHandler.add(
    endpoint: Endpoint<M, R>,
    httpHandler: HttpHandler,
    addHandler: RoutingHandler.(Predicate?, String, String, HttpHandler) -> Unit
): RoutingHandler {
    val method = endpoint.method.name
    val templateAndQuery = endpoint.urlTemplate.split(delimiters = *charArrayOf('?'), limit = 2, ignoreCase = false)
    val template = templateAndQuery[0]

    // constant query parameters can not be handled by Undertow RoutingHandler.
    // But they are part of routing, let's trigger 404-like response with Predicate when they are absent
    val predicate = templateAndQuery.getOrNull(1)
        ?.takeIf { it.isNotEmpty() }
        ?.let(::splitQuery)
        ?.takeIf { it.isNotEmpty() }
        ?.let(::QueryPredicate)

    addHandler(predicate, method, template, httpHandler)
    return this
}

@PublishedApi internal val addHndlr:
    RoutingHandler.(predicate: Predicate?, method: String, template: String, httpHandler: HttpHandler) -> Unit =
    { predicate, method, template, httpHandler ->
        if (predicate == null) add(method, template, httpHandler) else add(method, template, predicate, httpHandler)
    }

private class QueryPredicate(
    private val constQuery: Map<String, List<String>>
) : Predicate {
    override fun resolve(value: HttpServerExchange): Boolean {
        val queryParams = value.queryParameters
        if (constQuery.any { (key, values) -> queryParams[key]?.containsAll(values) != true }) return false

        // we're fine but let's remove constant query parameters now:
        constQuery.forEach { key, values -> check(queryParams[key]!!.removeAll(values)) }
        return true
    }
}

// https://stackoverflow.com/a/13592567/3050249
private fun splitQuery(query: String): Map<String, List<String>> =
    query.split('&')
        .map(::splitQueryParameter)
        .groupBy({ it.first }, { it.second })

private fun splitQueryParameter(it: String): Pair<String, String> {
    val idx = it.indexOf('=')
    val key = if (idx > 0) it.substring(0, idx) else it
    val value = if (idx > 0 && it.length > idx + 1) it.substring(idx + 1) else ""
    // should be `null` but we use `""` to be consistent with Undertow    ^^^^^^^
    return Pair(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8"))
}

@PublishedApi internal fun <R> gatherArgsAndInvoke(
    exchange: HttpServerExchange,
    endpoint: Endpoint<*, *>,
    respond: HttpServerExchange.(R) -> Unit,
    respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit,
    formParserFactory: FormParserFactory,
    handler: HttpServerExchange.(Array<Any?>) -> R
) {
    val params = endpoint.params
    val args = arrayOfNulls<Any>(params.size)
    var hasQParamsOrHeaders = false
    var hasField = false
    var hasFields = false
    var hasBody = false
    var hasPart = false
    var hasParts = false
    params.forEachIndexed { index, param ->
        try {
            args[index] = when (param) {
                is Path -> param.type.loadFromStr(
                    exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY).parameters[param.name.toString()]
                        ?: throw NoSuchElementException()
                )
                is Query -> parse(exchange.queryParameters[param.name.toString()], param.type)
                is QueryParams -> null.also { hasQParamsOrHeaders = true }
                is Header -> parse(exchange.requestHeaders[param.name.toString()], param.type)
                is Headers -> null.also { hasQParamsOrHeaders = true }
                is Field -> null.also { hasField = true }
                is Fields -> null.also { hasFields = true }
                is Body -> null.also { hasBody = true }
                is Part -> null.also { hasPart = true }
                is Parts<*> -> null.also { hasParts = true }
            }
        } catch (e: Exception) {
            return exchange.badEnd(respondBadRequest, param, e)
        }
    }
    if (hasQParamsOrHeaders) params.forEachIndexed { index, param ->
        try {
            when (param) {
                is QueryParams -> args[index] = gather(exchange.queryParameters)
                is Headers -> args[index] = gather(exchange.requestHeaders)
            }
        } catch (e: Exception) {
            return exchange.badEnd(respondBadRequest, param, e)
        }
    }

    if (hasField || hasFields || hasPart || hasParts) {
        check(!hasBody)
        formParserFactory.createParser(exchange).parse(fun(exchange: HttpServerExchange) {
            val formData = exchange.getAttachment(FormDataParser.FORM_DATA)
            if (hasField || hasPart) params.forEachIndexed { index, param ->
                try {
                    when (param) {
                        is Field<*> -> args[index] = parseField(formData.get(param.name.toString()), param.type)
                        is Part<*> -> args[index] =
                            (formData.get(param.name.toString())?.poll()?.fileItem ?: throw NoSuchElementException())
                                .let { param.body.fromStream(it.fileSize, it.inputStream) }
                    }
                } catch (e: Exception) {
                    return exchange.badEnd(respondBadRequest, param, e)
                }
            }
            if (hasFields || hasParts) params.forEachIndexed { index, param ->
                try {
                    when (param) {
                        is Fields -> args[index] = gather(formData)
                        is Parts<*> -> args[index] = gather(formData, param.body)
                    }
                } catch (e: Exception) {
                    return exchange.badEnd(respondBadRequest, param, e)
                }
            }
            exchange.respond(exchange.handler(args))
            exchange.endExchange()
        })
    } else if (hasBody) {
        exchange.requestReceiver.receiveFullBytes(fun(exchange: HttpServerExchange, message: ByteArray) {
            params.forEachIndexed { index, param ->
                try {
                    when (param) {
                        is Body<*> -> args[index] = param.fromStream(message.size.toLong(), message.inputStream())
                    }
                } catch (e: Exception) {
                    return exchange.badEnd(respondBadRequest, param, e)
                }
            }
            exchange.respond(exchange.handler(args))
            exchange.endExchange()
        })
    } else {
        exchange.respond(exchange.handler(args))
        exchange.endExchange()
    }
}

private fun HttpServerExchange.badEnd(
    respondBadRequest: HttpServerExchange.(failedParam: Param<*>, Throwable) -> Unit, param: Param<*>, e: Exception
) {
    respondBadRequest(param, e)
    endExchange()
}

private fun gather(params: Map<String, Deque<String>>): List<Pair<String, String>> {
    if (params.isEmpty()) return emptyList()

    val iter = params.entries.iterator()
    var next: Map.Entry<String, Deque<String>>
    while (true) {
        if (!iter.hasNext()) return emptyList()
        next = iter.next()
        if (next.value.isNotEmpty()) break
    }

    val out = ArrayList<Pair<String, String>>()
    while (true) {
        next.value.takeIf { it.isNotEmpty() }?.forEach { out.add(next.key to it) }
        if (!iter.hasNext()) break
        next = iter.next()
    }

    return out
}
private fun gather(params: HeaderMap): List<Pair<String, String>> {
    var cookie = params.fastIterateNonEmpty()
    if (cookie == -1L) return emptyList()

    val out = ArrayList<Pair<String, String>>()
    do {
        val current = params.fiCurrent(cookie)
        val name = current.headerName.toString()
        current.each { out.add(name to it) }
    } while (params.fiNext(cookie).also { cookie = it } != -1L)

    return out
}
private fun gather(params: FormData): List<Pair<String, String>> {
    val keys = params.iterator()
    var key: String
    var next: Deque<FormData.FormValue>
    while (true) {
        if (!keys.hasNext()) return emptyList()
        key = keys.next()
        next = params.get(key)
        if (next.isNotEmpty()) break
    }

    val out = ArrayList<Pair<String, String>>()
    while (true) {
        next.takeIf { it.isNotEmpty() }?.forEach { formValue ->
            if (!formValue.isFileItem) {
                out.add(key to formValue.value)
            }
        }
        if (!keys.hasNext()) break
        key = keys.next()
        next = params.get(key)
    }

    return out
}
private fun <T> gather(params: FormData, body: Body<T>): List<Pair<String, T>> {
    val keys = params.iterator()
    var key: String
    var next: Deque<FormData.FormValue>
    while (true) {
        if (!keys.hasNext()) return emptyList()
        key = keys.next()
        next = params.get(key)
        if (next.isNotEmpty()) break
    }

    val out = ArrayList<Pair<String, T>>()
    while (true) {
        next.takeIf { it.isNotEmpty() }?.forEach { formValue ->
            if (formValue.isFileItem) {
                out.add(key to formValue.fileItem.let { body.fromStream(it.fileSize, it.inputStream) })
            }
        }
        if (!keys.hasNext()) break
        key = keys.next()
        next = params.get(key)
    }

    return out
}

private fun parse(values: Deque<String>?, type: DataType<*>?): Any? = when (type) {
    null -> // interpret empty query parameter presence as boolean
        values?.remove("") == true // (unfortunately, ?q and ?q= are indistinguishable here)
    is DataType.NotNull.Simple<*> ->
        type.loadFromStr((values ?: throw NoSuchElementException()).remove())
    is DataType.Nullable<*, *> ->
        values?.poll()?.let((type.actualType as DataType.NotNull.Simple<*>)::loadFromStr)
    is DataType.NotNull.Collect<*, *, *> ->
        type.load(values?.map((type.elementType as DataType.NotNull.Simple<*>)::loadFromStr) ?: emptyList<Any?>())
    else ->
        throw AssertionError()
}
private fun parseField(values: Deque<FormData.FormValue>?, type: DataType<*>): Any? = when (type) {
    is DataType.NotNull.Simple<*> ->
        type.loadFromStr((values ?: throw NoSuchElementException()).remove().value)
    is DataType.Nullable<*, *> ->
        values?.poll()?.value?.let((type.actualType as DataType.NotNull.Simple<*>)::loadFromStr)
    is DataType.NotNull.Collect<*, *, *> ->
        type.load(values?.map { (type.elementType as DataType.NotNull.Simple<*>).loadFromStr(it.value) } ?: emptyList<Any?>())
    else ->
        throw AssertionError()
}

private fun <T> DataType.NotNull.Simple<T>.loadFromStr(value: String): T =
    if (hasStringRepresentation) load(value) else load(when (kind) {
        DataType.NotNull.Simple.Kind.Bool -> value.toBoolean() // == value.equalsIgnoreCase("true"), unfailable
        DataType.NotNull.Simple.Kind.I32 -> value.toInt()
        DataType.NotNull.Simple.Kind.I64 -> value.toLong()
        DataType.NotNull.Simple.Kind.F32 -> value.toFloat()
        DataType.NotNull.Simple.Kind.F64 -> value.toDouble()
        DataType.NotNull.Simple.Kind.Str -> value
        DataType.NotNull.Simple.Kind.Blob -> fromBase64(value)
    })

// Kotlin provides 1..5
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component6(): T = this[5]
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component7(): T = this[6]
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component8(): T = this[7]
