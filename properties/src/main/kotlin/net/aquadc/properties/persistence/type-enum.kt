package net.aquadc.properties.persistence

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.NoConstant
import net.aquadc.persistence.type.enum
import net.aquadc.persistence.type.string
import net.aquadc.properties.function.Enumz

/**
 * Special overload for the case when [E] is a real Java [Enum] type.
 * Finds an array of values automatically, represents enum constants as [String]s.
 */
@Suppress("UNCHECKED_CAST") // NoConstant is intentionally erased
inline fun <reified E : Enum<E>> enum(
    noinline fallback: (String) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.NotNull.Simple<E> =
    enum(enumValues(), string, Enumz.Name, fallback)
