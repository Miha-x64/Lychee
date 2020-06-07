@file:Suppress(
    "NOTHING_TO_INLINE", // Kotlin Array.component1..5 functions are InlineOnly, let our 6..8 be inline, too
    "UNCHECKED_CAST" // can do nothing about 'em
)
@file:JvmName("KtorRouting")
package net.aquadc.lychee.http.server.ktor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpMethod.Companion.parse
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.createRouteFromPath
import io.ktor.util.pipeline.PipelineContext
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
//import net.aquadc.lychee.http.param.Url
import net.aquadc.persistence.type.DataType


inline fun <M : HttpMethod<*>, R> Routing.bind(
    endpoint: Endpoint0<M, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.() -> R
): Unit =
    bind(endpoint).handle { _: Unit -> respond(call, handler()) }

inline fun <M : HttpMethod<P>,
    T, P : Param<T>,
    R> Routing.bind(
    endpoint: Endpoint1<M, P, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (arg) ->
                respond(call, handler(arg as T))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>,
    R> Routing.bind(
    endpoint: Endpoint2<M, *, P1, P2, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2) ->
                respond(call, handler(a1 as T1, a2 as T2))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>,
    R> Routing.bind(
    endpoint: Endpoint3<M, *, P1, P2, P3, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>, T4, P4 : Param<T4>,
    R> Routing.bind(
    endpoint: Endpoint4<M, *, P1, P2, P3, P4, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3, T4) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3, a4) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3, a4 as T4))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>, T4, P4 : Param<T4>, T5, P5 : Param<T5>,
    R> Routing.bind(
    endpoint: Endpoint5<M, *, P1, P2, P3, P4, P5, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3, T4, T5) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3, a4, a5) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3, a4 as T4, a5 as T5))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>, T4, P4 : Param<T4>,
    T5, P5 : Param<T5>, T6, P6 : Param<T6>,
    R> Routing.bind(
    endpoint: Endpoint6<M, *, P1, P2, P3, P4, P5, P6, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3, T4, T5, T6) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3, a4, a5, a6) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3, a4 as T4, a5 as T5, a6 as T6))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>, T4, P4 : Param<T4>,
    T5, P5 : Param<T5>, T6, P6 : Param<T6>, T7, P7 : Param<T7>,
    R> Routing.bind(
    endpoint: Endpoint7<M, *, P1, P2, P3, P4, P5, P6, P7, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3, T4, T5, T6, T7) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3, a4, a5, a6, a7) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3, a4 as T4, a5 as T5, a6 as T6, a7 as T7))
            },
            { respondBadRequest(call, it) }
        )
    }

inline fun <M : HttpMethod<*>,
    T1, P1 : Param<T1>, T2, P2 : Param<T2>, T3, P3 : Param<T3>, T4, P4 : Param<T4>,
    T5, P5 : Param<T5>, T6, P6 : Param<T6>, T7, P7 : Param<T7>, T8, P8 : Param<T8>,
    R> Routing.bind(
    endpoint: Endpoint8<M, *, P1, P2, P3, P4, P5, P6, P7, P8, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    noinline respondBadRequest: suspend (ApplicationCall, Throwable) -> Unit = respond400,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T1, T2, T3, T4, T5, T6, T7, T8) -> R
): Unit =
    bind(endpoint).handle { _: Unit ->
        gatherArgs(
            endpoint, call,
            { (a1, a2, a3, a4, a5, a6, a7, a8) ->
                respond(call, handler(a1 as T1, a2 as T2, a3 as T3, a4 as T4, a5 as T5, a6 as T6, a7 as T7, a8 as T8))
            },
            { respondBadRequest(call, it) }
        )
    }

@PublishedApi internal fun <M : HttpMethod<*>, R> Routing.bind(endpoint: Endpoint<M, R>): Route {
    val urlTemplate = endpoint.urlTemplate.let {
        if (it.contains('{')) hashPathParameters(it) else it.toString()
    }
    return createRouteFromPath(urlTemplate).createChild(HttpMethodRouteSelector(parse(endpoint.method.name)))
}

@PublishedApi internal val respond400: suspend (ApplicationCall, Throwable) -> Unit =
    { call, _ -> call.respond(HttpStatusCode.BadRequest, Unit) }

// There's no separation between query and path parameters: https://github.com/ktorio/ktor/issues/1015
// let's do this by ourselves
private fun hashPathParameters(template: CharSequence): String {
    val sb = StringBuilder(template.length)
    var start = 0
    var paramStart: Int
    while (template.indexOf('{', start, false).also { paramStart = it+1 } > 0) {
        val paramEnd = template.indexOf('}', paramStart, false)
        sb.append(template, start, paramEnd).append('-').append(hash(template, paramStart, paramEnd)).append('}')
        // ^^^^^^ /some/url/{segment         ^^^^^^  -   ^^^^^^ hash(segment)                         ^^^^^^  }
        start = paramEnd + 1
    }
    return sb.append(template, start, template.length).toString()
}
private fun hash(source: CharSequence, start: Int, end: Int): String {
    var hash = 0L
    source.forEachCodePoint(start, end) { _, point ->
        hash = 31 * hash + point
    }
    return hash.toString(36)
}
private inline fun CharSequence.forEachCodePoint(
    start: Int = 0, end: Int = length,
    action: (index: Int, codePoint: Int) -> Unit
) {
    var i = start
    var codePoint: Int
    while (i < end) {
        codePoint = Character.codePointAt(this, i);
        action(i, codePoint)
        i += Character.charCount(codePoint)
    }
}

@PublishedApi internal inline fun gatherArgs(
    endpoint: Endpoint<*, *>, call: ApplicationCall,
    success: (Array<Any?>) -> Unit, error: (Throwable) -> Unit
) {
    val args = gatherArgs(endpoint, call)
    if (args is Array<*>) success(args as Array<Any?>)
    else error(args as Throwable)
}
@PublishedApi internal fun gatherArgs(endpoint: Endpoint<*, *>, call: ApplicationCall): Any {
    try {
        val params = endpoint.params
        val args = arrayOfNulls<Any>(params.size)
        val ktorParams = call.parameters.entries().associateByTo(HashMap(), { it.key }, { it.value.toMutableList() })
        var hasQueryName = false
        params.forEachIndexed { idx, param ->
            args[idx] = when (param) {
//            is Url -> TODO() // um… what the heck am I supposed to do here!?
                is Path ->
                    param.type.load(ktorParams.remove(param.name.let { "$it-${hash(it, 0, it.length)}" })!!.single())
                is Query -> {
                    if (param.name == null) hasQueryName = true
                    else parse(param.name!!.toString(), param.type, ktorParams)
                }
                is QueryParams -> TODO()
                is Header -> TODO("this")
                is Headers -> TODO()
                is Field -> TODO()
                is Fields -> TODO()
                is Body -> TODO("this")
                is Part<*, *> -> TODO()
                is Parts<*> -> TODO()
            }
        }
        if (hasQueryName) {
            // TODO parse queryName
        }
        return args
    } catch (e: Exception) {
        return e
    }
}
private fun <T> parse(name: String, type: DataType<T>, ktorParams: MutableMap<String, MutableList<String>>): Any? = when (type) {
    is DataType.Nullable<*, *> -> {
        val values = ktorParams.get(TODO())
    }
    is DataType.NotNull.Simple -> TODO()
    is DataType.NotNull.Collect<*, *, *> -> TODO()
    is DataType.NotNull.Partial<*, *> -> throw AssertionError()
}

// Kotlin provides 1..5
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component6(): T = this[5]
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component7(): T = this[6]
@PublishedApi @JvmSynthetic internal inline operator fun <T> Array<out T>.component8(): T = this[7]
