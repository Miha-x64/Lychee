package net.aquadc.properties.android.persistence


internal fun Int.assertFitsByte(): Byte {
    require(this in Byte.MIN_VALUE..Byte.MAX_VALUE) { "value ${this} cannot be fit into a byte" }
    return toByte()
}

internal fun Int.assertFitsShort(): Short {
    require(this in Short.MIN_VALUE..Short.MAX_VALUE) { "value ${this} cannot be fit into a short" }
    return toShort()
}
