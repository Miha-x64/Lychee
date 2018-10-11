package net.aquadc.persistence.type


@Suppress("RedundantModalityModifier")
@PublishedApi internal open class EnumType<E : Enum<E>>(
        isNullable: Boolean,
        private val enumType: Class<E>
) : DataType.Str<E>(isNullable, Byte.MAX_VALUE.toInt()) {

    override fun asString(value: E): kotlin.String =
            value.name

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
 * [default] sample: `{ E.UNKNOWN }`
 */
inline fun <reified E : Enum<E>> enum(
        values: Array<E>,
        crossinline nameProp: (E) -> String = Enum<E>::name,
        crossinline default: (String) -> E = {
            throw AssertionError("No enum constant with name $it in type ${E::class.java.name}")
        }
): DataType.Str<E> =
        object : EnumType<E>(false, E::class.java) {

            private val lookup =
                    values.associateByTo(HashMap(values.size), nameProp).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun asT(value: kotlin.String): E =
                    lookup[value] ?: default(value)

            override fun asString(value: E): kotlin.String =
                    nameProp(value)

        }

/**
 * Creates a custom type for [E] enum type.
 * [lookup] sample: `{ name -> E.values().firstOrNull { it.someCustomName == name } ?: E.UNKNOWN }`
 * [asString] sample: `E::someCustomName`
 */
inline fun <reified E : Enum<E>> enum(
        crossinline lookup: (String) -> E,
        crossinline asString: (E) -> String
): DataType.Str<E> =
        object : EnumType<E>(false, E::class.java) {

            override fun asT(value: String): E =
                    lookup.invoke(value)

            override fun asString(value: E): String =
                    asString.invoke(value)

        }
