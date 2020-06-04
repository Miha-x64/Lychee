@file:JvmName("OkHttp3Deferred")
package net.aquadc.lychee.http.client.okhttp3

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.aquadc.lychee.http.param.Body
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// we're in a separate file to avoid warnings if kotlinx.coroutines are not on the classpath

fun <T> async(): (OkHttpClient, Request, Body<T>) -> Deferred<T> =
    { client, request, body ->
        val deferred = CompletableDeferred<T>()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val rb = response.body()!!
                deferred.complete(body.fromStream(rb.contentLength(), response.code(), rb.byteStream()))
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
