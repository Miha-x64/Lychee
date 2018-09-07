// this is still SQLite-specific since it uses "TEXT" to store enum names :)
package net.aquadc.properties.sql.dialect.sqlite

import net.aquadc.properties.sql.Converter
import java.lang.AssertionError


/**
 * Creates a simple converter for [E] enum type.
 * lookup: Java's `Enum.valueOf` which uses reflection and caches values in a `Map<String, E>`;
 *         throws an exception if there's no such enum constant
 * asString: Java's `Enum::name`
 */
inline fun <reified E : Enum<E>> enum(): Converter<E> =
        EnumConverter(E::class.java, "TEXT")

/**
 * Creates a converter for [E] enum type.
 * [values] sample: `E.values()`
 * [prop] sample: `E::name`
 * [default] sample: `{ E.UNKNOWN }`
 */
inline fun <reified E : Enum<E>> enum(
        values: Array<E>,
        crossinline prop: (E) -> String,
        crossinline default: (String) -> E = {
            throw AssertionError("No enum constant with custom name $it in type ${E::class.java.name}")
        }
): Converter<E> =
        object : EnumConverter<E>(E::class.java, "TEXT") {

            private val lookup =
                    values.associateByTo(HashMap(values.size), prop).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'prop' for each enum constant passed in 'values'"
                    } }

            override fun lookup(name: String): E =
                    lookup[name] ?: default(name)

            override fun asString(value: E): String =
                    prop(value)

        }

/**
 * Creates a custom converter for [E] enum type.
 * [lookup] sample: `{ name -> E.values().firstOrNull { it.someCustomName == name } ?: E.UNKNOWN }`
 * [asString] sample: `E::someCustomName`
 */
inline fun <reified E : Enum<E>> enum(
        crossinline lookup: (String) -> E,
        crossinline asString: (E) -> String
): Converter<E> =
        object : EnumConverter<E>(E::class.java, "TEXT") {

            override fun lookup(name: String): E =
                    lookup.invoke(name)

            override fun asString(value: E): String =
                    asString.invoke(value)

        }
