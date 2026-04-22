package net.aquadc.persistence.sql

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk

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
