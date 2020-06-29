package net.aquadc.lychee.http

import net.aquadc.lychee.http.client.okhttp3.url
import net.aquadc.lychee.http.param.ExtracorpParam
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.Response
import net.aquadc.persistence.type.byteString
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder
import java.util.Base64


class EndpointTest {

    @Test fun `parsing const query params`() =
        assertEquals(
            setOf("id", "blah", "qwe"),
            EndpointN<Get, ExtracorpParam<*>, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(
                GET, "/?id=100500&blah&blah=1&qwe", noParams, Response<Nothing>()
            ).gatherQueryParamNames()
        )

    @Test fun `blob query`() { // wannabe end-to-end
        assertEquals(
            "http://example.com/?q=" + URLEncoder.encode(
                Base64.getEncoder().encodeToString(byteArrayOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89)),
                "UTF-8"
            ),
            GET("http://example.com/", Query("q", byteString), Response<Nothing>())
                .url(null, byteArrayOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89).toByteString(0, 11))
        )
    }

}
