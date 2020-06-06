package net.aquadc.lychee.http

import io.ktor.application.ApplicationCall
import io.ktor.response.respondText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.aquadc.lychee.http.client.okhttp3.blocking
import net.aquadc.lychee.http.client.okhttp3.template
import net.aquadc.lychee.http.param.Resp
import net.aquadc.lychee.http.param.Response
import net.aquadc.lychee.http.server.ktor.route
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test


class KtorOkHttpE2E {

    @Test fun `trivial get`() {
        val test = GET("/test", Response<String>())
        val respondText: suspend (ApplicationCall, CharSequence) -> Unit = { call: ApplicationCall, text: CharSequence ->
            call.respondText(text.toString())
        }

        val server = embeddedServer(Netty, 8182, "127.0.0.1") {
            routing {
                route(test, respondText) {
                    "Hello OkHttp! We're Ktor and Netty."
                }
            }
        }.start()

        assertEquals(
            "Hello OkHttp! We're Ktor and Netty.",
            OkHttpClient().template("http://127.0.0.1:8182/", test, blocking { _, response -> response.body!!.string() })()
        )
        server.stop(100, 100)
    }

}
