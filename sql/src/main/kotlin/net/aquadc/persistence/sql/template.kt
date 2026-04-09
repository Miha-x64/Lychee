package net.aquadc.persistence.sql

import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

/**
 * A function of unknown arity.
 * Implementors must also ~~implement [Function0]..[Function8]~~
 * **inherit from [FuncXImpl]** __until KT-24067 fixed__.
 */
interface FuncN<T, R> {
    fun invokeUnchecked(vararg arg: T): R
}

interface Fetch<out R> {
    fun fetch(
        from: SqlDatabase,
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        receiverAndArguments: Array<out Any>
    ): R
}

typealias Exec<R> = Fetch<R>

enum class BindBy {
    Name,
    Position,
}
