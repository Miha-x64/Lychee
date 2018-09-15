// this is still SQLite-specific since it uses "TEXT" to store enum names :)
package net.aquadc.struct.converter

import java.lang.AssertionError


/**
 * Creates a simple converter for [E] enum type.
 * lookup: Java's `Enum.valueOf` which uses reflection and caches values in a `Map<String, E>`;
 *         throws an exception if there's no such enum constant
 * asString: Java's `Enum::name`
 */
inline fun <reified E : Enum<E>> enum(): UniversalConverter<E> =
        EnumConverter(E::class.java, DataTypes.SmallString)

/**
 * Creates a converter for [E] enum type.
 * [values] sample: `E.values()`
 * [nameProp] sample: `E::name`
 * [default] sample: `{ E.UNKNOWN }`
 */
inline fun <reified E : Enum<E>> enum(
        values: Array<E>,
        crossinline nameProp: (E) -> String,
        crossinline default: (String) -> E = {
            throw AssertionError("No enum constant with custom name $it in type ${E::class.java.name}")
        }
): UniversalConverter<E> =
        object : EnumConverter<E>(E::class.java, DataTypes.SmallString) {

            private val lookup =
                    values.associateByTo(HashMap(values.size), nameProp).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun lookup(name: String): E =
                    lookup[name] ?: default(name)

            override fun asString(value: E): String =
                    nameProp(value)

        }

/**
 * Creates a custom converter for [E] enum type.
 * [lookup] sample: `{ name -> E.values().firstOrNull { it.someCustomName == name } ?: E.UNKNOWN }`
 * [asString] sample: `E::someCustomName`
 */
inline fun <reified E : Enum<E>> enum(
        crossinline lookup: (String) -> E,
        crossinline asString: (E) -> String
): UniversalConverter<E> =
        object : EnumConverter<E>(E::class.java, DataTypes.SmallString) {

            override fun lookup(name: String): E =
                    lookup.invoke(name)

            override fun asString(value: E): String =
                    asString.invoke(value)

        }
