package net.aquadc.persistence.sql.dialect


private val hexChars = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
internal fun StringBuilder.appendHex(bytes: ByteArray): StringBuilder {
    for (b in bytes) {
        val v = b.toInt() and 0xFF
        append(hexChars[v ushr 4])
        append(hexChars[v and 0x0F])
    }
    return this
}

internal fun StringBuilder.appendPlaceholders(count: Int): StringBuilder {
    if (count == 0) return this

    repeat(count) { append("?, ") }
    setLength(length - 2) // trim comma

    return this
}
