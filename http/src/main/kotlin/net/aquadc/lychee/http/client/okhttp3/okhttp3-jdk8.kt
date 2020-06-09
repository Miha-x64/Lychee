@file:JvmName("OkHttp3Jdk8")
package net.aquadc.lychee.http.client.okhttp3

import net.aquadc.lychee.http.param.Resp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage


fun <T> completable(
    parse: Response.(Resp<T>) -> T
): (OkHttpClient, Request, Resp<T>) -> CompletionStage<T> =
    { client, request, body ->
        val future = CompletableFuture<T>()
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                future.complete(response.parse(body))
            }
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }
        })
        future.whenComplete { _, _ ->
            if (future.isCancelled) call.cancel()
        }
        future
    }
