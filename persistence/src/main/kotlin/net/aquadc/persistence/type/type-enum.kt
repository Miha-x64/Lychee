@file:JvmName("EnumTypes")
package net.aquadc.persistence.type


// TODO: support EnumSet

inline fun <reified E : Any, U : Any> enum(
        values: Array<E>,
        encodeAs: DataType.Simple<U>,
        noinline encode: (E) -> U,
        noinline fallback: (U) -> E = NoConstant(E::class.java) as (Any?) -> Nothing
): DataType.Simple<E> =
        enumInternal(values, encodeAs, encode, fallback)

/**
 * Represents values of [E] type like [U] values.
 * [values] sample: `E.values()`
 * [nameProp] sample: `E::name`
 * [fallback] sample: `{ E.UNSUPPORTED }`
 */
@Suppress("UNCHECKED_CAST")
@PublishedApi internal fun <E : Any, U : Any> enumInternal(
        values: Array<E>,
        encodeAs: DataType.Simple<U>,
        encode: (E) -> U,
        fallback: (U) -> E
): DataType.Simple<E> =
        object : DataType.Simple<Any?>(false, encodeAs.kind) {

            private val lookup =
                    values.associateByTo(HashMap(values.size), encode).also { check(it.size == values.size) {
                        "there were duplicate names, check values of 'nameProp' for each enum constant passed in 'values'"
                    } }

            override fun decode(value: Any): Any? {
                val u = encodeAs.decode(value)
                return lookup[u] ?: fallback(u)
            }

            override fun encode(value: Any?): Any =
                    encode.invoke(value as E)

        } as DataType.Simple<E>

@PublishedApi internal class NoConstant(private val t: Class<*>) : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? {
        throw NoSuchElementException("No enum constant with name $p1 in type $t")
    }
}
