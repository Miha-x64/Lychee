@file:JvmName("Endpoints")
@file:Suppress("NOTHING_TO_INLINE") // pass-through
package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.Param
import net.aquadc.lychee.http.param.ExtracorpParam
import net.aquadc.lychee.http.param.Resp

@JvmField val GET: Get = Get()
@JvmField val POST: Post = Post()
@JvmField val PUT: HttpMethod<Param<*>> = HttpMethod("PUT")
@JvmField val PATCH: HttpMethod<Param<*>> = HttpMethod("PATCH")
@JvmField val DELETE: HttpMethod<ExtracorpParam<*>> = HttpMethod("DELETE")
// TODO HEAD, OPTIONS? Hmm…

/**
 * HTTP method (verb).
 * @param P allowed parameter type. [Param] for methods with body, [ExtracorpParam] for methods without it.
 */
open class HttpMethod<P : Param<*>>
// you can instantiate custom methods for exotic DELETE with body, OPTIONS, whatever
constructor(
    @JvmField val name: String
)

// we need Get and Post as separate types for type-safe linking and HTTP forms
class Get internal constructor() : HttpMethod<ExtracorpParam<*>>("GET")
class Post internal constructor() : HttpMethod<Param<*>>("POST")

/**
 * Describes Endpoint: an HTTP RPC gateway.
 */
interface Endpoint<M : HttpMethod<*>, R> {
    val method: M
    val urlTemplate: CharSequence/*?*/
    val params: Array<out Param<*>>
    val response: Resp<R>
}
interface Endpoint0<M : HttpMethod<*>, R> : Endpoint<M, R>
interface Endpoint1<M : HttpMethod<P>, P : Param<*>, R> : Endpoint<M, R>
interface Endpoint2<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, R> : Endpoint<M, R>
interface Endpoint3<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, R> : Endpoint<M, R>
interface Endpoint4<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, R> : Endpoint<M, R>
interface Endpoint5<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, R> : Endpoint<M, R>
interface Endpoint6<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, R> : Endpoint<M, R>
interface Endpoint7<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, P7 : P, R> : Endpoint<M, R>
interface Endpoint8<M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, P7 : P, P8 : P, R> : Endpoint<M, R>

@JvmField @PublishedApi internal val noParams: Array<out Param<*>> = emptyArray()

@RequiresOptIn("Very young and not tested enough", RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalHttp

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    response: Resp<R>
): Endpoint0<M, R> =
    EndpointN<M, P, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, noParams, response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param: P, response: Resp<R>
): Endpoint1<M, P, R> =
    EndpointN<M, P, P, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, response: Resp<R>
): Endpoint2<M, P, P1, P2, R> =
    EndpointN<M, P, P1, P2, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, response: Resp<R>
): Endpoint3<M, P, P1, P2, P3, R> =
    EndpointN<M, P, P1, P2, P3, Nothing, Nothing, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, param4: P4, response: Resp<R>
): Endpoint4<M, P, P1, P2, P3, P4, R> =
    EndpointN<M, P, P1, P2, P3, P4, Nothing, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3, param4), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, param4: P4, param5: P5, response: Resp<R>
): Endpoint5<M, P, P1, P2, P3, P4, P5, R> =
    EndpointN<M, P, P1, P2, P3, P4, P5, Nothing, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3, param4, param5), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, param4: P4, param5: P5, param6: P6, response: Resp<R>
): Endpoint6<M, P, P1, P2, P3, P4, P5, P6, R> =
    EndpointN<M, P, P1, P2, P3, P4, P5, P6, Nothing, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3, param4, param5, param6), response
    )

@JvmName("endpoint") @ExperimentalHttp
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, P7 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, param4: P4, param5: P5, param6: P6, param7: P7, response: Resp<R>
): Endpoint7<M, P, P1, P2, P3, P4, P5, P6, P7, R> =
    EndpointN<M, P, P1, P2, P3, P4, P5, P6, P7, Nothing, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3, param4, param5, param6, param7), response
    )

@JvmName("endpoint") @ExperimentalHttp
@Suppress("RemoveExplicitTypeArguments") // EndpointN could grow, let's be prepared for this
inline operator fun <M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, P7 : P, P8 : P, R> M.invoke(
    urlTemplate: CharSequence/*?*/,
    param1: P1, param2: P2, param3: P3, param4: P4, param5: P5, param6: P6, param7: P7, param8: P8, response: Resp<R>
): Endpoint8<M, P, P1, P2, P3, P4, P5, P6, P7, P8, R> =
    EndpointN<M, P, P1, P2, P3, P4, P5, P6, P7, P8, R>(
        this, urlTemplate, arrayOf<Param<*>>(param1, param2, param3, param4, param5, param6, param7, param8), response
    )



@JvmField @PublishedApi internal val noCharSeqs: Array<out CharSequence> = emptyArray()
