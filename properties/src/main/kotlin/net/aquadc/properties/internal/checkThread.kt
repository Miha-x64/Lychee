package net.aquadc.properties.internal


internal fun checkThread(expected: Thread) {
    if (Thread.currentThread() !== expected)
        throw RuntimeException("${Thread.currentThread()} is not allowed to touch this property since it was created in $expected.")
}
