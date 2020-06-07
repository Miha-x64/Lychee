package net.aquadc.lychee.http

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import net.aquadc.lychee.http.client.okhttp3.blocking
import net.aquadc.lychee.http.client.okhttp3.template
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.Response
import net.aquadc.lychee.http.server.ktor.bind
import net.aquadc.persistence.extended.uuid
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.UUID


class KtorOkHttpE2E {

    private val host = "127.0.0.1"
    private val port = 8182
    private val httpHostPort = "http://$host:$port/"

    private val respondText: suspend (ApplicationCall, CharSequence) -> Unit =
        { call: ApplicationCall, text: CharSequence ->
            call.respondText(text.toString())
        }
    private val respond204: suspend (ApplicationCall, Unit) -> Unit =
        { call: ApplicationCall, _ ->
            call.respond(HttpStatusCode.NoContent, Unit)
        }

    var server: NettyApplicationEngine? = null
    @After fun fin() { server?.let { it.stop(100, 100); server = null } }

    @Test fun `trivial get`() {
        val test = GET("/test", Response<String>())

        server = embeddedServer(Netty, port, host) {
            routing {
                bind(test, respondText) {
                    "Hello OkHttp! We're Ktor and Netty."
                }
            }
        }.start()

        assertEquals(
            "Hello OkHttp! We're Ktor and Netty.",
            OkHttpClient().template(httpHostPort, test, blocking { _, response -> response.body!!.string() })()
        )
    }

    @Ignore @Test fun mixed() {
        val updatePhoto = POST("/user/{role}/photo",
            Header("X-Token"), Path("role"), Query("id", uuid), Stream("image/*"), Response<Unit>())

        lateinit var token: String
        lateinit var role: String
        lateinit var id: UUID
        lateinit var image: ByteArray

        server = embeddedServer(Netty, port, host) {
            routing {
                bind(updatePhoto, respond204) { token0, role0, id0, image0 ->
                    token = token0
                    role = role0
                    id = id0
                    image = image0().readBytes()
                }
            }
        }.start()

        val doUpdatePhoto = OkHttpClient().template(httpHostPort, updatePhoto, blocking { _, response -> check(response.isSuccessful) })
        val uuid = UUID.randomUUID()
        doUpdatePhoto("seecred", "adm1n", uuid, { ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 2, 5, 1)) })

        assertEquals("seecred", token)
        assertEquals("admin", role)
        assertEquals(uuid, id)
        assertArrayEquals(byteArrayOf(0, 1, 2, 3, 2, 5, 1), image)
    }

    @Test fun hashPathParameters() {
        val path = Class.forName("net.aquadc.lychee.http.server.ktor.KtorRouting")
            .getDeclaredMethod("hashPathParameters", CharSequence::class.java)
            .also { it.isAccessible = true }
            .invoke(null, "/user/{name}/friend?name=lmao")

        assertEquals(
            "/user/{name-" + (31*(31*(31*'n'.toLong() + 'a'.toLong()) + 'm'.toLong()) + 'e'.toLong()).toString(36) + "}/friend?name=lmao",
            path
        )
    }

    @Test fun `query name`() {
        server = embeddedServer(Netty, port, host) {
            routing {
                get("") {
                    // Parameters [q=[, ]] — how the fuck am I supposed to differ `q=` and `q`?!
                    println(call.parameters.toString())
                }
            }
        }.start()
        OkHttpClient().newCall(Request.Builder().get().url("$httpHostPort?q=&q").build()).execute()
    }

}
