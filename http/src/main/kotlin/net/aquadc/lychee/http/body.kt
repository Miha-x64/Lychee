@file:JvmName("Bodies")
package net.aquadc.lychee.http

import net.aquadc.lychee.http.param.Body
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream


fun Stream(mediaType: CharSequence? = null): Body<() -> InputStream> =
    object : Body<() -> InputStream>(mediaType) {
        override fun stream(value: () -> InputStream): InputStream =
            value.invoke()
        override fun fromStream(estimateSize: Long, stream: InputStream): () -> InputStream =
            object : () -> InputStream {
                var consumed = false
                override fun invoke(): InputStream {
                    if (consumed) throw IllegalStateException("The stream is already consumed")
                    consumed = true
                    return stream
                }
            }
    }

fun Bytes(mediaType: CharSequence? = null): Body<ByteArray> =
    object : Body<ByteArray>(mediaType) {
        override fun contentLength(value: ByteArray): Long =
            value.size.toLong()
        override fun stream(value: ByteArray): InputStream =
            ByteArrayInputStream(value)
        override fun fromStream(estimateSize: Long, stream: InputStream): ByteArray =
            stream.use(InputStream::readBytes)
    }

fun Text(mediaType: CharSequence? = null): Body<CharSequence> =
    object : Body<CharSequence>(mediaType) {
        // fixme find a way to give Content-Length and InputStream without double .toString().toByteArray()
        override fun stream(value: CharSequence): InputStream =
            ByteArrayInputStream(value.toString().toByteArray())
        override fun fromStream(estimateSize: Long, stream: InputStream): CharSequence =
            stream.use { input ->
                input.reader().readText() // cool. IO is a pleasure in Kotlin
            }
    }

fun SizedInput(mediaType: CharSequence? = null): Body<SizedInput> =
    object : Body<SizedInput>(mediaType) {
        override fun contentLength(value: SizedInput): Long =
            value.size
        override fun stream(value: SizedInput): InputStream =
            value.stream
        override fun fromStream(estimateSize: Long, stream: InputStream): SizedInput =
            object : SizedInput {
                override val size: Long
                    get() = estimateSize
                private var consumed = false
                override val stream: InputStream
                    get() {
                        if (consumed) throw IllegalStateException("The stream is already consumed")
                        consumed = true
                        return stream
                    }

            }
    }

/**
 * Represents any sized [InputStream] source like files, including virtual ones.
 */
interface SizedInput {
    val size: Long
    val stream: InputStream
}

fun File.input(): SizedInput = object : SizedInput {
    override val size: Long
        get() = this@input.length()
    override val stream: InputStream
        get() = FileInputStream(this@input)
}
