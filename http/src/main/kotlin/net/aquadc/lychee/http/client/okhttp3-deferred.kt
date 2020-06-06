@file:JvmName("OkHttp3Deferred")
package net.aquadc.lychee.http.client.okhttp3

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.aquadc.lychee.http.param.Resp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// we're in a separate file/class to avoid warnings if kotlinx.coroutines are not on the classpath

fun <T> async(
    parse: (Resp<T>, Response) -> T
): (OkHttpClient, Request, Resp<T>) -> Deferred<T> =
    { client, request, body ->
        val deferred = CompletableDeferred<T>()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                deferred.complete(parse(body, response))
            }
            override fun onFailure(call: Call, e: IOException) {
                deferred.completeExceptionally(e)
            }
        })
        deferred.invokeOnCompletion {
            if (deferred.isCancelled) call.cancel()
        }
        deferred
    }
