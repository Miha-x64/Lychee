package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.ExtracorpParam
import net.aquadc.lychee.http.param.Response
import org.junit.Assert.assertEquals
import org.junit.Test


class EndpointTest {

    @Test fun `parsing const query params`() =
        assertEquals(
            setOf("id", "blah", "qwe"),
            EndpointN<Get, ExtracorpParam<*>, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing, Nothing>(
                GET, "/?id=100500&blah&blah=1&qwe", noParams, Response<Nothing>()
            ).gatherQueryParamNames()
        )

}
