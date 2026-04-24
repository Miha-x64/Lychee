@file:OptIn(ExperimentalHttp::class)
package net.aquadc.lychee.http

import io.undertow.Undertow
import io.undertow.server.RoutingHandler
import kotlinx.coroutines.runBlocking
import net.aquadc.lychee.http.client.okhttp3.blocking
import net.aquadc.lychee.http.client.okhttp3.completable
import net.aquadc.lychee.http.client.okhttp3.defer
import net.aquadc.lychee.http.client.okhttp3.future
import net.aquadc.lychee.http.client.okhttp3.on
import net.aquadc.lychee.http.param.Field
import net.aquadc.lychee.http.param.Fields
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Part
import net.aquadc.lychee.http.param.Parts
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.lychee.http.param.QueryPresence
import net.aquadc.lychee.http.param.Response
import net.aquadc.lychee.http.server.undertow2.add
import net.aquadc.persistence.extended.uuid
import net.aquadc.persistence.type.byteArray
import net.aquadc.persistence.type.i32
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.Executors


class UndertowOkHttpE2E {

    private val host = "127.0.0.1"
    private val port = 8182
    private val baseUrl = "http://$host:$port/"

    private val client = OkHttpClient()
    private var server: Undertow? = null
    @After fun cleanup() {
        server?.let { it.stop(); server = null }
    }

    @Test fun `get string with header, path, and query`() {
        val getUser = GET("/user/{role}/", Header("X-Token"), Path("role"), Query("id", uuid), Query("xxx", byteArray), Response<String>())

        lateinit var rqToken: String
        lateinit var rqRole: String
        lateinit var rqId: UUID
        lateinit var rqXxx: ByteArray
        server = undertow { add(getUser, { responseSender.send(it) }, { _, e -> throw e }) { token, role, id, xxx ->
            rqToken = token
            rqRole = role
            rqId = id
            rqXxx = xxx
            "Access granted."
        } }
        val func = getUser.on(baseUrl, blocking { body!!.use { it.string() } })

        val id = UUID.randomUUID()
        val authorizedFunc = func("sooper secred")
        val response = client.authorizedFunc("admin", id, byteArrayOf(1, 1, 2, 3, 5))

        assertEquals("sooper secred", rqToken)
        assertEquals("admin", rqRole)
        assertEquals(id, rqId)
        assertArrayEquals(byteArrayOf(1, 1, 2, 3, 5), rqXxx)
        assertEquals("Access granted.", response)
    }

    @Test fun `post with int queryPresence, queryParams, and form-data`() {
        val updateUser = POST("/",
            QueryPresence("admin"), QueryParams, Field("name"), Field("email"), Field("birth", i32), Fields,
            Response<UUID>())

        var rqAdmin: Boolean? = null
        lateinit var rqParams: Collection<Pair<CharSequence, CharSequence?>>
        lateinit var rqName: String
        lateinit var rqEmail: String
        var rqBirth: Int? = null
        lateinit var rqRest: Collection<Pair<CharSequence, CharSequence>>
        lateinit var rsId: UUID
        server = undertow {
            add(
                updateUser,
                { rsId = it; responseSender.send(it.toString()) }, { _, e -> throw e }
            ) { isAdmin, params, name, email, birth, rest ->
                rqAdmin = isAdmin
                rqParams = params
                rqName = name
                rqEmail = email
                rqBirth = birth
                rqRest = rest
                UUID.randomUUID()
            }
        }

        val doUpdateUser = updateUser.on(baseUrl, blocking { UUID.fromString(body!!.use { it.string() }) })
        var id = client.doUpdateUser(true, listOf("whatever" to "brrrr"), "John", "john@", 1736,
            listOf("blah" to "whatever1", "blah" to "whatever2", "zzz" to "xxx"))
        assertEquals(true, rqAdmin)
        assertEquals(listOf("whatever" to "brrrr"), rqParams)
        assertEquals("John", rqName)
        assertEquals("john@", rqEmail)
        assertEquals(1736, rqBirth)
        assertEquals(listOf("blah" to "whatever1", "blah" to "whatever2", "zzz" to "xxx"), rqRest)
        assertEquals(rsId, id)

        id = client.doUpdateUser(false, listOf<Nothing>(), "Jane", "jane@", 1824, listOf())
        assertEquals(false, rqAdmin)
        assertEquals(listOf<Nothing>(), rqParams)
        assertEquals("Jane", rqName)
        assertEquals("jane@", rqEmail)
        assertEquals(1824, rqBirth)
        assertEquals(listOf<Nothing>(), rqRest)
        assertEquals(rsId, id)
    }

    @Test fun upload() {
        val upload = POST("/updatePhoto", Stream("image/*"), Response<Int>())

        lateinit var rqBytes: ByteArray
        server = undertow { add(upload, { statusCode = it }, { _, e -> throw e}) { image ->
            rqBytes = image().readBytes()
            204
        } }

        val doUpload = upload.on(baseUrl, blocking { body?.close(); code })
        assertEquals(204, client.doUpload { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 4, 3, 2, 1)) })
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 4, 3, 2, 1), rqBytes)
    }

    @Test fun multipart() {
        val upload = POST("/updatePhoto", Field("name"), Field("id", uuid),
            Part("photo", Stream("image/*"), { "photo.jpg" }), Parts(Text("*/*"), { "whatever.bin" }), Response<Int>())

        lateinit var rqName: String
        lateinit var rqId: UUID
        lateinit var rqBytes: ByteArray
        lateinit var rqFiles: Collection<Pair<CharSequence, CharSequence>>
        server = undertow { add(upload, { statusCode = it }, { _, e -> throw e }) {
            name, id, photo, files ->

            rqName = name
            rqId = id
            rqBytes = photo().readBytes()
            rqFiles = files

            204
        } }

        val doUpload = upload.on(baseUrl, blocking { body?.close(); code })
        val id = UUID.randomUUID()
        assertEquals(204, client.doUpload("Unnamed", id, { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5, 4, 3, 2, 1)) }, listOf("qwe" to "asd")))
        assertEquals("Unnamed", rqName)
        assertEquals(id, rqId)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 4, 3, 2, 1), rqBytes)
        assertEquals(listOf("qwe" to "asd"), rqFiles)
    }

    @Test fun future() {
        val get = GET("/", Response<Unit>())
        server = undertow { add(get, { responseSender.send("ok") }) { } }
        val executor = Executors.newSingleThreadExecutor()
        val doGet = get.on(baseUrl, future(executor) { body?.close(); })
        client.doGet().get()
        executor.shutdown()
    }

    @Test fun cf() {
        val get = GET("/", Response<Unit>())
        server = undertow { add(get, { responseSender.send("ok") }) { } }
        val doGet = get.on(baseUrl, completable { body?.close(); })
        client.doGet().get()
    }

    @Test fun coroutine() {
        val get = GET("/", Response<Unit>())
        server = undertow { add(get, { responseSender.send("ok") }) { } }
        val doGet = get.on(baseUrl, defer { body?.close(); })
        runBlocking {
            client.doGet().await()
        }
    }

    private inline fun undertow(register: RoutingHandler.() -> RoutingHandler): Undertow =
        Undertow.builder().addHttpListener(port, host, RoutingHandler().register()).build().also { it.start() }

    operator fun <S, T, U, V, W, R> (S.(T, U, V, W) -> R).invoke(t: T): S.(U, V, W) -> R =
        { u, v, w -> this@invoke(t, u, v, w) }

}
