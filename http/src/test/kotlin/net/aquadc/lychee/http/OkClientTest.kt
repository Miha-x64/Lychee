package net.aquadc.lychee.http

import net.aquadc.lychee.http.client.okhttp3.template
import net.aquadc.lychee.http.param.Body
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.persistence.extended.uuid
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID


class OkClientTest {

    val whatever = GET("/user/{role}/", Header("X-Token"), Path("role"), Query("id", uuid), Bytes("*/*"))
    val client = OkHttpClient()
    @Test fun testTemplate() {
        lateinit var req: Request
        val peekAdapter = { client: OkHttpClient, request: Request, body: Body<ByteArray> ->
            req = request
            byteArrayOf()
        }
        val func = client.template("https://example.com/", whatever, peekAdapter)

        val id = UUID.randomUUID()
        val authorizedFunc = func("super secret")
        authorizedFunc("admin", id)

        assertEquals(
            "Request{method=GET, url=https://example.com/user/admin/?id=$id, headers=[X-Token:super secret]}",
            req.toString()
        )
    }

}

operator fun <T, U, V, R> ((T, U, V) -> R).invoke(t: T): (U, V) -> R =
    { u, v -> this(t, u, v) }
