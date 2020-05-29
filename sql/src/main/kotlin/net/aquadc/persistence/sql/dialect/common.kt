package net.aquadc.persistence.sql.dialect

import net.aquadc.persistence.type.DataType


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

internal inline fun StringBuilder.appendIf(cond: Boolean, what1: Char, what2: String): StringBuilder =
    if (cond) append(what1).append(what2) else this

internal inline fun <R> foldArrayType(
    hasArraySupport: Boolean,
    elementType: DataType<*>,
    ifAppropriate: (nullable: Boolean, actualElementType: DataType.NotNull.Simple<*>) -> R,
    ifNot: () -> R
): R {
    val nullable: Boolean = elementType is DataType.Nullable<*, *>
    val actualElementType: DataType.NotNull<*> = // damn. I really miss Java assignment as expression
        if (nullable) (elementType as DataType.Nullable<*, *>).actualType
        else elementType as DataType.NotNull<*>

    // arrays of arrays or structs are still serialized.
    // PostgreSQL multidimensional arrays are actually matrices
    // which is kinda weird surprise and inappropriate constraint.
    if (hasArraySupport && actualElementType is DataType.NotNull.Simple)
        return ifAppropriate(nullable, actualElementType)
    else
        return ifNot()
}
