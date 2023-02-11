@file:JvmName("Transactions")
package net.aquadc.persistence.sql

import androidx.annotation.RequiresApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Calls [block] within transaction passing [FreeSource] which has functionality to read data.
 * In future could retry conflicting transaction by calling [block] more than once.
 */
@OptIn(ExperimentalContracts::class)
inline fun <SRC, R> Session<SRC>.read(block: FreeSource<SRC>.() -> R): R {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }

    val transaction = read()
    try {
        return block(transaction)
    } finally {
        transaction.close()
    }
}

@RequiresApi(24) @JvmName("read")
fun <SRC> Session<SRC>.read4j(block: java.util.function.Consumer<FreeSource<SRC>>): Unit =
    read().use(block::accept)

@RequiresApi(24) @JvmName("read")
fun <SRC, R> Session<SRC>.read4j(block: java.util.function.Function<FreeSource<SRC>, R>): R =
    read().use(block::apply)


/**
 * Calls [block] within transaction passing [Transaction] which has functionality to create, mutate, remove data.
 * In future could retry conflicting transaction by calling [block] more than once.
 */
@OptIn(ExperimentalContracts::class)
inline fun <SRC, R> Session<SRC>.mutate(block: FreeExchange<SRC>.() -> R): R {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }

    val transaction = mutate()
    try {
        val r = block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}

@RequiresApi(24) @JvmName("mutate")
fun <SRC> Session<SRC>.mutate4j(block: java.util.function.Consumer<FreeExchange<SRC>>): Unit =
    mutate().use(block::accept)

@RequiresApi(24) @JvmName("mutate")
fun <SRC, R> Session<SRC>.mutate4j(block: java.util.function.Function<FreeExchange<SRC>, R>): R =
    mutate().use(block::apply)

@Deprecated("renamed, use read{} and mutate{}", ReplaceWith("this.mutate(block)"))
inline fun <SRC, R> Session<SRC>.withTransaction(block: FreeExchange<SRC>.() -> R): R =
    mutate(block)
