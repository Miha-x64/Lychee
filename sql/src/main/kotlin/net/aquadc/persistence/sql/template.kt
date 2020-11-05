@file:JvmName("SqlTemplate")
@file:Suppress(
        "NOTHING_TO_INLINE",  // just pass-through + unchecked cast functions, nothing to outline
        "UNCHECKED_CAST"
)
package net.aquadc.persistence.sql

import net.aquadc.persistence.FuncXImpl
import net.aquadc.persistence.sql.template.Mutation
import net.aquadc.persistence.sql.template.Query
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import org.intellij.lang.annotations.Language

@Deprecated("It's not a “vararg function”, it's a “N-ary function”", level = DeprecationLevel.ERROR)
typealias VarFunc<T, R> = FuncN<T, R>

/**
 * A function of unknown arity.
 * Implementors must also ~~implement [Function0]..[Function8]~~
 * **inherit from [FuncXImpl]** __until KT-24067 fixed__.
 */
interface FuncN<T, R> {
    fun invokeUnchecked(vararg arg: T): R
}

interface Fetch<in SRC, out R> {
    fun fetch(
        from: SRC,
        query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>,
        receiverAndArguments: Array<out Any>
    ): R
}

typealias Exec<SRC, R> = Fetch<SRC, R>

enum class BindBy {
    Name,
    Position,
}

// Because of KT-24067 I can't just cast Query to (...) -> R, so let's cast to FuncXImpl
@Deprecated("use Query() instead",
    ReplaceWith("Query(query, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, R> Session<SRC>.query(
        @Language("SQL") query: String,
        fetch: Fetch<SRC, R>
): () -> R =
    Query(query, fetch).let { func -> { func(this) } }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    fetch: Fetch<SRC, R>
): (T) -> R =
    Query(query, type, fetch).let { func -> { p0 -> func(this, p0) } }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    fetch: Fetch<SRC, R>
): (T1, T2) -> R =
    Query(query, type1, type2, fetch).let { func -> { p0, p1 -> func(this, p0, p1) } }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3) -> R =
    Query(query, type1, type2, type3, fetch).let { func -> { p0, p1, p2 -> func(this, p0, p1, p2) } }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, type4, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4) -> R =
    Query(query, type1, type2, type3, type4, fetch).let { func -> { p0, p1, p2, p3 -> func(this, p0, p1, p2, p3) } }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, type4, type5, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5) -> R =
    Query(query, type1, type2, type3, type4, type5, fetch).let { func ->
        { p0, p1, p2, p3, p4 -> func(this, p0, p1, p2, p3, p4) }
    }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, type4, type5, type6, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5, T6) -> R =
    Query(query, type1, type2, type3, type4, type5, type6, fetch).let { func ->
        { p0, p1, p2, p3, p4, p5 -> func(this, p0, p1, p2, p3, p4, p5) }
    }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, type4, type5, type6, type7, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Session<SRC>.query(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    type7: Ilk<T7, DataType.NotNull<T7>>,
    fetch: Fetch<SRC, R>
): (T1, T2, T3, T4, T5, T6, T7) -> R =
    Query(query, type1, type2, type3, type4, type5, type6, type7, fetch).let { func ->
        { p0, p1, p2, p3, p4, p5, p6 -> func(this, p0, p1, p2, p3, p4, p5, p6) }
    }

@Deprecated("use Query() instead",
    ReplaceWith("Query(query, type1, type2, type3, type4, type5, type6, type7, type8, fetch)",
        "net.aquadc.persistence.sql.template.Query"))
fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Session<SRC>.query(
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
): (T1, T2, T3, T4, T5, T6, T7, T8) -> R =
    Query(query, type1, type2, type3, type4, type5, type6, type7, type8, fetch).let { func ->
        { p0, p1, p2, p3, p4, p5, p6, p7 -> func(this, p0, p1, p2, p3, p4, p5, p6, p7) }
    }

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    exec: Exec<SRC, R>
): Transaction<SRC>.() -> R =
    Mutation(query, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type: Ilk<T, DataType.NotNull<T>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T) -> R =
    Mutation(query, type, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2) -> R =
    Mutation(query, type1, type2, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3) -> R =
    Mutation(query, type1, type2, type3, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, type4, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4) -> R =
    Mutation(query, type1, type2, type3, type4, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, type4, type5, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5) -> R =
    Mutation(query, type1, type2, type3, type4, type5, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, type4, type5, type6, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, R> Session<SRC>.mutate(
    @Language("SQL") query: String,
    type1: Ilk<T1, DataType.NotNull<T1>>,
    type2: Ilk<T2, DataType.NotNull<T2>>,
    type3: Ilk<T3, DataType.NotNull<T3>>,
    type4: Ilk<T4, DataType.NotNull<T4>>,
    type5: Ilk<T5, DataType.NotNull<T5>>,
    type6: Ilk<T6, DataType.NotNull<T6>>,
    exec: Exec<SRC, R>
): Transaction<SRC>.(T1, T2, T3, T4, T5, T6) -> R =
    Mutation(query, type1, type2, type3, type4, type5, type6, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, type4, type5, type6, type7, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, R> Session<SRC>.mutate(
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
    Mutation(query, type1, type2, type3, type4, type5, type6, type7, exec)

@Deprecated("moved & renamed",
    ReplaceWith("Mutation(query, type1, type2, type3, type4, type5, type6, type7, type8, exec)",
        "net.aquadc.persistence.sql.template.Mutation"))
inline fun <SRC, T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any, R> Session<SRC>.mutate(
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
    Mutation(query, type1, type2, type3, type4, type5, type6, type7, type8, exec)
