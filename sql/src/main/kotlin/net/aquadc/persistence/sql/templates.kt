@file:[JvmName("SqlTemplate") Suppress("NOTHING_TO_INLINE")]
package net.aquadc.persistence.sql.template

import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.sql.Exec
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.FuncN
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.Transaction
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import org.intellij.lang.annotations.Language


inline fun <SRC, R> Query(
    @Language("SQL") query: String,
    fetch: Fetch<SRC, R>
): Session<SRC>.() -> R =
    Template(query, emptyArray(), fetch)

inline fun <SRC, T : Any, R> Query(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T) -> R =
    Template(query, arrayOf(type), fetch)

inline fun <SRC, T1 : Any, T2 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3, T4) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3, T4, T5) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3, T4, T5, T6) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3, T4, T5, T6, T7) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7), fetch)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    type8: Ilk<T8, DataType.NotNull<T8>>,
    fetch: Fetch<SRC, R>
): Session<SRC>.(T1, T2, T3, T4, T5, T6, T7, T8) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7, type8), fetch)



inline fun <SRC, R> Mutation(
    @Language("SQL") query: String,
    exec: Exec<SRC, R>
): Transaction<SRC>.() -> R =
    Template(query, emptyArray(), exec)

inline fun <SRC, T : Any, R> Mutation(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T) -> R =
    Template(query, arrayOf(type), exec)

inline fun <SRC, T1 : Any, T2 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5, T6) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5, T6, T7) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type7), exec)

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Mutation(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    type8: Ilk<T8, DataType.NotNull<T8>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5, T6, T7, T8) -> R =
    Template(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7, type8), exec)




@PublishedApi internal class Template<SRC, R>(
    private val query: String,
    private val argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
    private val fetch: Fetch<SRC, R>
) : FuncXImpl<Any, R>(), FuncN<Any, R> {

    override fun invokeUnchecked(vararg args: Any): R =
        (args[0] as? Session<SRC> ?: (args[0] as Transaction<SRC>).mySession).rawQuery(query, argumentTypes, args, fetch)

    // for debugging
    override fun toString(): String =
        fetch.javaClass.simpleName + '(' + query + ')'

}


// TODO named placeholders
