@file:[JvmName("SqlTemplate") Suppress("NOTHING_TO_INLINE")]
package net.aquadc.persistence.sql.template

import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.sql.MutableSqlDatabase
import net.aquadc.persistence.sql.SqlDatabase
import net.aquadc.persistence.sql.SqlInvocation
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import org.intellij.lang.annotations.Language


/*
It looks tempting to make two objects, Query and Mutation,
and implement templates as {Query | Mutation}.invoke()
to get away from having two copies of templates.

It compiles, it works, but SQL highlighting in IDEA
via `@Language("SQL")` doesn't.
So OK, I'm gonna sacrifice brevity in favor of IDE convenience.
 */

inline fun <DB : SqlDatabase, R> Query(
    @Language("SQL") query: String,
    fetch: SqlInvocation<DB, R>,
): DB.() -> R =
    Template(query, emptyArray(), fetch)

inline fun <DB : SqlDatabase, T : Any, R> Query(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T) -> R =
    Template(query, arrayOf(type), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6, T7) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7), fetch)

inline fun <DB : SqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    type8: Ilk<T8, DataType.NotNull<T8>>,
    fetch: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6, T7, T8) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7, type8), fetch)


inline fun <DB : MutableSqlDatabase, R> Mutation(
    @Language("SQL") query: String,
    exec: SqlInvocation<DB, R>,
): DB.() -> R =
    Template(query, emptyArray(), exec)

inline fun <DB : MutableSqlDatabase, T : Any, R> Mutation(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    exec: SqlInvocation<DB, R>,
): DB.(T) -> R =
    Template(query, arrayOf(type), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6, T7) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7), exec)

inline fun <DB : MutableSqlDatabase, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    type8: Ilk<T8, DataType.NotNull<T8>>,
    exec: SqlInvocation<DB, R>,
): DB.(T1, T2, T3, T4, T5, T6, T7, T8) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7, type8), exec)


@PublishedApi internal class Template<DB : SqlDatabase, R>(
    private val query: String,
    private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
    private val fetch: SqlInvocation<DB, R>
) : FuncXImpl<DB, Any, R>() {

    override fun invokeUnchecked(receiver: DB, vararg args: Any): R =
        fetch.fetch(receiver, query, argumentTypes, args)

    // for debugging
    override fun toString(): String =
        fetch.javaClass.simpleName + '(' + query + ')'

}


// TODO named placeholders
