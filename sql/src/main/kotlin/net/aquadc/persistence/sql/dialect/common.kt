package net.aquadc.persistence.sql.dialect


internal fun StringBuilder.appendPlaceholders(count: Int): StringBuilder {
    if (count == 0) return this

    repeat(count) { append("?, ") }
    setLength(length - 2) // trim comma

    return this
}

internal fun StringBuilder.appendReplacing(what: CharSequence, needle: Char, replacement: CharSequence): StringBuilder {
    var start = 0
    var nextNeedle: Int
    while (what.indexOf(needle, start, false).also { nextNeedle = it } >= 0) {
        append(what, start, nextNeedle).append(replacement)
        start = nextNeedle + 1
    }
    return append(what, start, what.length)
}

internal inline fun StringBuilder.appendIf(cond: Boolean, what: String): StringBuilder =
    if (cond) append(what) else this

internal inline fun StringBuilder.appendIf(cond: Boolean, what: Char): StringBuilder =
    if (cond) append(what) else this
