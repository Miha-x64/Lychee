@file:JvmName("OkioBodies")
package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.Body
import okio.Buffer
import okio.ByteString
import java.io.InputStream

fun ByteString(mediaType: CharSequence? = null): Body<ByteString> =
    object : Body<ByteString>(mediaType) {
        override fun contentLength(value: ByteString): Long =
            value.size().toLong()
        override fun stream(value: ByteString): InputStream =
            Buffer().write(value).inputStream() // https://github.com/square/okio/issues/774#issuecomment-703315013
        override fun fromStream(estimateSize: Long, stream: InputStream): ByteString =
            stream.use {
                if (estimateSize < 0 || estimateSize > Int.MAX_VALUE) {
                    val b = stream.readBytes()
                    ByteString.of(b, 0, b.size)
                } else {
                    ByteString.read(stream, estimateSize.toInt())
                }
            }
    }
