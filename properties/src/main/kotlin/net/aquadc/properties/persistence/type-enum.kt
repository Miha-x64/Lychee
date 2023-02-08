package net.aquadc.properties.persistence

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.NoConstant
import net.aquadc.persistence.type.enum
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.string
import net.aquadc.properties.function.Enumz

/**
 * Special overload for the case when [E] is a real Java [Enum] type.
 * Finds an array of values automatically, represents enum constants as [String]s.
 */
@Suppress("UNCHECKED_CAST") // NoConstant is intentionally erased
inline fun <reified E : Enum<E>> enumByName(
    noinline fallback: (String) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.NotNull.Simple<E> =
    enum(enumValues(), string, Enumz.Name, fallback)

@Deprecated("renamed", ReplaceWith("enumByName<E>()", "net.aquadc.properties.persistence.enum"))
//       I'm sorry for fucking up your fallback. ^^ https://youtrack.jetbrains.com/issue/KT-36767
inline fun <reified E : Enum<E>> enum(
    noinline fallback: (String) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.NotNull.Simple<E> =
    enumByName(fallback)

inline fun <reified E : Enum<E>> enumByOrdinal(
    noinline fallback: (Int) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.NotNull.Simple<E> =
    enum(enumValues(), i32, Enumz.Ordinal, fallback)
