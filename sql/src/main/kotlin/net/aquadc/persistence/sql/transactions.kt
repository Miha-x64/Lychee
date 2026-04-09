@file:JvmName("Transactions")
package net.aquadc.persistence.sql

import androidx.annotation.RequiresApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Calls [block] within transaction passing [SqlDatabase] which has functionality to read data.
 * In future could retry conflicting transaction by calling [block] more than once.
 */
@OptIn(ExperimentalContracts::class)
inline fun <R> Session.read(block: SqlDatabase.() -> R): R {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }

    val transaction = read()
    try {
        return block(transaction)
    } finally {
        transaction.close()
    }
}

@RequiresApi(24) @JvmName("acceptRead")
fun Session.read4j(block: java.util.function.Consumer<SqlDatabase>): Unit =
    read { block.accept(this) }

@RequiresApi(24) @JvmName("applyRead")
fun <R> Session.read4j(block: java.util.function.Function<SqlDatabase, R>): R =
    read { block.apply(this) }


/**
 * Calls [block] within transaction passing [MutableSqlDatabase] which has functionality to create, mutate, remove data.
 * In future could retry conflicting transaction by calling [block] more than once.
 */
@OptIn(ExperimentalContracts::class)
inline fun <R> Session.mutate(block: MutableSqlDatabase.() -> R): R {
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

@RequiresApi(24) @JvmName("acceptMutation")
fun  Session.mutate4j(block: java.util.function.Consumer<MutableSqlDatabase>): Unit =
    mutate { block.accept(this) }

@RequiresApi(24) @JvmName("applyMutation")
fun <R> Session.mutate4j(block: java.util.function.Function<MutableSqlDatabase, R>): R =
    mutate { block.apply(this) }

@Deprecated("renamed, use read{} and mutate{}", ReplaceWith("this.mutate(block)"))
inline fun <R> Session.withTransaction(block: MutableSqlDatabase.() -> R): R =
    mutate(block)
