@file:JvmName("SqlTemplate")
@file:Suppress(
        "NOTHING_TO_INLINE",  // just pass-through + unchecked cast functions, nothing to outline
        "UNCHECKED_CAST"
)
package net.aquadc.persistence.sql

import net.aquadc.persistence.VarFuncImpl
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import org.intellij.lang.annotations.Language

/**
 * A function of unknown arity.
 * Implementors must also ~~implement [Function0]..[Function8]~~
 * **inherit from [VarFuncImpl]** __until KT-24067 fixed__.
 */
interface VarFunc<T, R> {
    fun invokeUnchecked(vararg arg: T): R
}

interface Fetch<SRC, R> {
    fun fetch(from: SRC, query: String, argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, arguments: Array<out Any>): R
}

enum class BindBy {
    Name,
    Position,
}

// Because of KT-24067 I can't just cast Query to (...) -> R, so let's cast to VarFuncImpl
inline fun <SRC, R> Session<SRC>.query(
        @Language("SQL") query: String,
        fetch: Fetch<SRC, R>
): () -> R =
        rawQuery(query, emptyArray(), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    fetch: Fetch<SRC, R>
): (T) -> R =
        rawQuery(query, arrayOf(type), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    fetch: Fetch<SRC, R>
): (T1, T2) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5, T7) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7), fetch) as VarFuncImpl<Any, R>

inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Session<SRC>.query(
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
): (T1, T2, T3, T4, T5, T7, T8) -> R =
        rawQuery(query, arrayOf<Ilk<*, DataType.NotNull<*>>>(type1, type2, type3, type4, type5, type6, type7, type8), fetch) as VarFuncImpl<Any, R>
