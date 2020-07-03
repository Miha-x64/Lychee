package net.aquadc.lychee.http

import net.aquadc.lychee.http.client.okhttp3.url
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryPresence
import net.aquadc.lychee.http.param.Resp
import org.junit.Assert.assertEquals
import org.junit.Test


class UrlTest {

    @Test fun zero() {
        assertEquals("http://example.com/", GET("http://example.com/", Resp<Nothing>()).url(null))
        assertEquals("http://example.com/qwe", GET("qwe", Resp<Nothing>()).url("http://example.com/"))
        assertEquals("http://example.com/qwe#asd", GET("qwe", Resp<Nothing>()).url("http://example.com/", "asd"))
    }
    @Test fun one() {
        assertEquals("http://example.com/whatever",
            GET("http://example.com/{path}", Path("path"), Resp<Nothing>()).url(null, "whatever")
        )
        assertEquals("http://example.com/whatever",
            GET("{path}", Path("path"), Resp<Nothing>()).url("http://example.com/", "whatever")
        )
        assertEquals("http://example.com/",
            GET("http://example.com/", QueryPresence("q"), Resp<Nothing>()).url(null, false)
        )
        assertEquals("http://example.com/",
            GET("", QueryPresence("q"), Resp<Nothing>()).url("http://example.com/", false)
        )
        assertEquals("http://example.com/?q",
            GET("http://example.com/", QueryPresence("q"), Resp<Nothing>()).url(null, true)
        )
        assertEquals("http://example.com/?q",
            GET("", QueryPresence("q"), Resp<Nothing>()).url("http://example.com/", true)
        )
        assertEquals("http://example.com/?q=w",
            GET("http://example.com/", Query("q"), Resp<Nothing>()).url(null, "w")
        )
        assertEquals("http://example.com/?q=w",
            GET("", Query("q"), Resp<Nothing>()).url("http://example.com/", "w")
        )
    }
    @Test fun two() {
        assertEquals("http://example.com/whatever?q=w",
            GET("http://example.com/{path}", Path("path"), Query("q"), Resp<Nothing>()).url(null, "whatever", "w")
        )
        assertEquals("http://example.com/whatever?q=w",
            GET("{path}", Path("path"), Query("q"), Resp<Nothing>()).url("http://example.com/", "whatever", "w")
        )
        assertEquals("http://example.com/whatever?q=w#asd",
            GET("http://example.com/{path}", Path("path"), Query("q"), Resp<Nothing>()).url(null, "whatever", "w", "asd")
        )
        assertEquals("http://example.com/whatever?q=w#asd",
            GET("{path}", Path("path"), Query("q"), Resp<Nothing>()).url("http://example.com/", "whatever", "w", "asd")
        )
    }
    @Test fun `no proto`() {
        assertEquals("//example.com/", GET("//example.com/", Resp<Nothing>()).url(null))
        assertEquals("//example.com/qwe", GET("qwe", Resp<Nothing>()).url("//example.com/"))
        assertEquals("//example.com/whatever",
            GET("//example.com/{path}", Path("path"), Resp<Nothing>()).url(null, "whatever")
        )
        assertEquals("//example.com/whatever",
            GET("{path}", Path("path"), Resp<Nothing>()).url("//example.com/", "whatever")
        )
        assertEquals("//example.com/whatever?q=w#asd",
            GET("//example.com/{path}", Path("path"), Query("q"), Resp<Nothing>()).url(null, "whatever", "w", "asd")
        )
        assertEquals("//example.com/whatever?q=w#asd",
            GET("{path}", Path("path"), Query("q"), Resp<Nothing>()).url("//example.com/", "whatever", "w", "asd")
        )
    }

}
