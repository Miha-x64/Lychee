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

interface SqlInvocation<in DB : SqlDatabase, out R> {
    fun fetch(
        from: DB,
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        receiverAndArguments: Array<out Any>
    ): R
}

typealias Fetch<R> = SqlInvocation<SqlDatabase, R>

typealias Exec<R> = SqlInvocation<MutableSqlDatabase, R>

enum class BindBy {
    Name,
    Position,
}
