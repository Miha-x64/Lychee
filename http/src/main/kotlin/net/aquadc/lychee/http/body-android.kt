@file:JvmName("AndroidBodies")
package net.aquadc.lychee.http

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import java.io.FileInputStream
import java.io.InputStream

fun (() -> AssetFileDescriptor).input(): SizedInput = object : SizedInput {
    private var _size: Long = Long.MIN_VALUE
    private var _stream: FileInputStream? = null
    override val size: Long
        get() = _size.takeIf { it != Long.MIN_VALUE } ?: get().let { _size }
    override val stream: InputStream
        get() = (_stream ?: get().let { _stream!! }).also { _stream = null }

    private fun get() {
        val afd = invoke()
        _size = afd.length
        _stream = afd.createInputStream()
    }
}
fun ContentResolver.input(uri: Uri): SizedInput =
    { openAssetFileDescriptor(uri, "r") }.input()
