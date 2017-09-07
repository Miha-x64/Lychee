package net.aquadc.properties.internal

internal fun checkThread(expected: Thread) {
    if (Thread.currentThread() !== expected)
        throw RuntimeException("Only $expected allowed to touch this property.")
}