@file:JvmName("KtorRouting")
package net.aquadc.lychee.http.server.ktor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.HttpMethodRouteSelector
import io.ktor.routing.Routing
import io.ktor.routing.createRouteFromPath
import io.ktor.util.pipeline.PipelineContext
import net.aquadc.lychee.http.Endpoint0
import net.aquadc.lychee.http.HttpMethod


inline fun <M : HttpMethod<*>, R> Routing.route(
    endpoint: Endpoint0<M, R>,
    noinline respond: suspend (ApplicationCall, R) -> Unit,
    crossinline handler: suspend PipelineContext<Unit, ApplicationCall>.() -> R
): Unit =
    route(endpoint).handle { _ /* drop Unit */ -> respond(call, handler()) }

@PublishedApi internal fun <M : HttpMethod<*>, R> Routing.route(endpoint: Endpoint0<M, R>) =
    createRouteFromPath(endpoint.urlTemplate?.toString() ?: "/")
        .createChild(HttpMethodRouteSelector(io.ktor.http.HttpMethod.parse(endpoint.method.name)))
