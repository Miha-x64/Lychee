package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.Field
import net.aquadc.lychee.http.param.Fields
import net.aquadc.lychee.http.param.Header
import net.aquadc.lychee.http.param.Headers
import net.aquadc.lychee.http.param.Param
import net.aquadc.lychee.http.param.Part
import net.aquadc.lychee.http.param.Parts
import net.aquadc.lychee.http.param.Path
import net.aquadc.lychee.http.param.Query
import net.aquadc.lychee.http.param.QueryParams
import net.aquadc.lychee.http.param.Body
import net.aquadc.lychee.http.param.Url
import net.aquadc.persistence.newSet


@PublishedApi internal class EndpointN<
    M : HttpMethod<P>, P : Param<*>, P1 : P, P2 : P, P3 : P, P4 : P, P5 : P, P6 : P, P7 : P, P8 : P, R
    >(
    override val method: M,
    override val urlTemplate: CharSequence?,
    override val params: Array<out Param<*>>,
    override val response: Body<R>
) : Endpoint0<M, R>
  , Endpoint1<M, P, R>
  , Endpoint2<M, P, P1, P2, R>
  , Endpoint3<M, P, P1, P2, P3, R>
  , Endpoint4<M, P, P1, P2, P3, P4, R>
  , Endpoint5<M, P, P1, P2, P3, P4, P5, R>
  , Endpoint6<M, P, P1, P2, P3, P4, P5, P6, R>
  , Endpoint7<M, P, P1, P2, P3, P4, P5, P6, P7, R>
  , Endpoint8<M, P, P1, P2, P3, P4, P5, P6, P7, P8, R>
{
    init {
        var urlIdx = -1

        var hasBody = false
        var hasPart = false
        var hasField = false

        var paths: MutableSet<String>? = null
        var headers: MutableSet<String>? = null
        params.forEachIndexed { idx, it ->
            when (val p = it as Param<*>) { // this 'useless' cast helps compiler understand that `when` is exhaustive
                is Url -> {
                    if (urlIdx != -1) throw IllegalArgumentException()
                    urlIdx = idx
                }
                is Path<*> -> {
                    if (!(paths ?: newSet<String>(4).also { paths = it }).add(p.name.toString())) throw IllegalArgumentException()
                    Unit
                }
                is Query<*> -> { } // duplicate queries are rather questionable but totally OK for HTTP, don't check
                is QueryParams -> { }
                is Field<*>, is Fields -> {
                    if (hasPart || hasBody) throw IllegalArgumentException()
                    hasField = true
                }
                is Header<*> -> {
                    if (!(headers ?: newSet<String>(4).also { headers = it }).add(p.name.toString())) throw IllegalArgumentException()
                    Unit
                }
                is Headers -> {}
                is Body -> {
                    if (hasPart || hasBody || hasField) throw IllegalArgumentException()
                    hasBody = true
                }
                is Part<*, *>, is Parts<*> -> {
                    if (hasBody || hasField) throw IllegalArgumentException()
                    hasPart = true
                }
            }!!
        }
        paths?.let { paths ->
            if (urlTemplate == null) throw NoSuchElementException()
            paths.forEach { check("{$it}" in urlTemplate) }
        } ?: if (urlTemplate == null && urlIdx == -1) {
            throw NoSuchElementException() // neither urlTemplate nor Url parameter
        }
    }
}
