package net.aquadc.persistence.type


@Suppress("RedundantModalityModifier")
@PublishedApi internal open class EnumType<E/* : Enum<E>*/>(
        isNullable: Boolean,
        private val enumType: Class<E>
) : DataType.Str<E>(isNullable, Byte.MAX_VALUE.toInt()) {

    override fun asString(value: E): String =
            (value as Enum<*>).name

    @Suppress("UPPER_BOUND_VIOLATED")
    override fun asT(value: kotlin.String): E =
            java.lang.Enum.valueOf<E>(enumType, value)

} // TODO: support EnumSet

/**
 * Creates a simple [DataType] representing an [E] enum type.
 * from: Java's `Enum.valueOf` which uses reflection and caches values in a `Map<String, E>`;
 *         throws an exception if there's no such enum constant
 * asString: Java's `Enum::name`
 */
@Deprecated("internally this uses reflection. Should use own type instead",
        ReplaceWith("enum(E.values())"))
inline fun <reified E : Enum<E>> enum(): DataType.Str<E> =
        EnumType(false, E::class.java)

/**
 * Creates a type for [E] enum type.
 * [values] sample: `E.values()`
 * [nameProp] sample: `E::name`
 * [fallback] sample: `{ E.UNSUPPORTED }`
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<E>> enum(
        values: Array<E>,
        crossinline nameProp: (E) -> String = { enum: Enum<E> -> enum.name },
        crossinline fallback: (String) -> E = {
            throw AssertionError("No enum constant with name $it in type ${E::class.java.name}")
        }
): DataType.Str<E> =
        object : DataType.Str<Any?>(false, Byte.MAX_VALUE.toInt()) {

            private val lookup =
                    values.associateByTo(HashMap(values.size), nameProp).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun asT(value: String): Any? =
                    lookup[value] ?: fallback(value)

            override fun asString(value: Any?): String =
                    nameProp(value as E)

        } as DataType.Str<E>

/**
 * Creates a custom type for [E] enum type.
 * [lookup] sample: `{ name -> E.values().firstOrNull { it.someCustomName == name } ?: E.UNKNOWN }`
 * [asString] sample: `E::someCustomName`
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified E : Enum<E>> enum(
        crossinline lookup: (String) -> E,
        crossinline asString: (E) -> String
): DataType.Str<E> =
        object : DataType.Str<Any?>(false, Byte.MAX_VALUE.toInt()) {

            override fun asT(value: String): Any? =
                    lookup.invoke(value)

            override fun asString(value: Any?): String =
                    asString.invoke(value as E)

        } as DataType.Str<E>
