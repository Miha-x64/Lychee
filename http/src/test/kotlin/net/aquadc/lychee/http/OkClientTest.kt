package net.aquadc.lychee.http

import io.undertow.Undertow
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.FormParserFactory
import net.aquadc.lychee.http.client.okhttp3.blocking
import net.aquadc.lychee.http.client.okhttp3.template
import net.aquadc.lychee.http.param.Field
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.lychee.http.param.QueryPresence
import net.aquadc.lychee.http.param.Response
import net.aquadc.persistence.extended.uuid
import net.aquadc.persistence.type.i32
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID


class OkClientTest {

    val host = "127.0.0.1"
    val port = 8182
    val baseUrl = "http://$host:$port/"

    val client = OkHttpClient()
    var server: Undertow? = null
    @After fun cleanup() {
        server?.let { it.stop(); server = null }
    }

    @Test fun `get string with header, path, and query`() {
        val getUser = GET("/user/{role}/", Header("X-Token"), Path("role"), Query("id", uuid), Response<String>())

        lateinit var rqMethod: String
        lateinit var rqToken: String
        lateinit var rqRole: String
        lateinit var rqId: UUID
        server = undertow { x ->
            rqMethod = x.requestMethod.toString()
            rqToken = x.requestHeaders.get("X-Token").first
            rqRole = x.requestPath.split("/").let { path -> path[path.indexOf("user")+1] }
            rqId = UUID.fromString(x.queryParameters["id"]!!.first)
            x.responseSender.send("Access granted.")
        }
        val func = client.template(baseUrl, getUser, blocking { body!!.string() })

        val id = UUID.randomUUID()
        val authorizedFunc = func("sooper secred")
        val response = authorizedFunc("admin", id)

        assertEquals("GET", rqMethod)
        assertEquals("sooper secred", rqToken)
        assertEquals("admin", rqRole)
        assertEquals(id, rqId)
        assertEquals("Access granted.", response)
    }

    @Test fun `post with int queryPresence, queryParams, and form-data`() {
        val updateUser = POST("/",
            QueryPresence("admin"), QueryParams, Field("name"), Field("email"), Field("birth", i32),
            Response<UUID>())

        lateinit var rqMethod: String
        var rqAdmin: Boolean? = null
        lateinit var rqParams: Map<String, Collection<String>>
        lateinit var rqName: String
        lateinit var rqEmail: String
        var rqBirth: Int? = null
        lateinit var rsId: UUID
        server = undertow { x ->
            rqMethod = x.requestMethod.toString()
            rqAdmin = x.queryParameters.remove("admin")?.remove("") == true
            rqParams = x.queryParameters.mapValues { (_, v) -> v.toList() }
            val form = FormParserFactory.builder().build().createParser(x).parseBlocking()
            rqName = form["name"].first.value
            rqEmail = form["email"].first.value
            rqBirth = form["birth"].first.value.toInt()

            x.responseSender.send(UUID.randomUUID().also { rsId = it }.toString())
        }

        val doUpdateUser = client.template(baseUrl, updateUser, blocking { UUID.fromString(body!!.string()) })
        var id = doUpdateUser(true, listOf("whatever" to "brrrr"), "John", "john@", 1736)
        assertEquals("POST", rqMethod)
        assertEquals(true, rqAdmin)
        assertEquals(mapOf("whatever" to listOf("brrrr")), rqParams)
        assertEquals("John", rqName)
        assertEquals("john@", rqEmail)
        assertEquals(1736, rqBirth)
        assertEquals(rsId, id)

        id = doUpdateUser(false, listOf<Nothing>(), "Jane", "jane@", 1824)
        assertEquals("POST", rqMethod)
        assertEquals(false, rqAdmin)
        assertEquals(mapOf<Nothing, Nothing>(), rqParams)
        assertEquals("Jane", rqName)
        assertEquals("jane@", rqEmail)
        assertEquals(1824, rqBirth)
        assertEquals(rsId, id)
    }

    private fun undertow(handler: (HttpServerExchange) -> Unit): Undertow =
        Undertow.builder().addHttpListener(port, host, handler).build().also { it.start() }

}

operator fun <T, U, V, R> ((T, U, V) -> R).invoke(t: T): (U, V) -> R =
    { u, v -> this(t, u, v) }
