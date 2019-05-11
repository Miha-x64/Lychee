package net.aquadc.properties.android.persistence


internal fun Int.assertFitsByte(): Byte {
    require(this in Byte.MIN_VALUE..Byte.MAX_VALUE) {
        "value $this cannot be fit into ${Byte::class.java.simpleName}"
    }
    return toByte()
}

internal fun Int.assertFitsShort(): Short {
    require(this in Short.MIN_VALUE..Short.MAX_VALUE) {
        "value $this cannot be fit into ${Short::class.java.simpleName}"
    }
    return toShort()
}
