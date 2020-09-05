@file:[
    JvmName("IncrementalTypes")
    Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
]
package net.aquadc.persistence.extended.inctemental

import net.aquadc.persistence.extended.tuple.Box
import net.aquadc.persistence.extended.tuple.Tuple
import net.aquadc.persistence.extended.tuple.Tuple3
import net.aquadc.persistence.extended.tuple.Tuple4
import net.aquadc.persistence.extended.tuple.Tuple5
import net.aquadc.persistence.extended.tuple.Tuple6
import net.aquadc.persistence.extended.tuple.Tuple7
import net.aquadc.persistence.extended.tuple.Tuple8
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.toString
import net.aquadc.persistence.type.DataType


@PublishedApi internal class IncrementalType<SCH : Schema<SCH>>(schema: SCH)
    : DataType.NotNull.Partial<Incremental8<*, *, *, *, *, *, *, *>, SCH>(schema) {

    @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("load") // avoid having both `load-<hash>()` and `bridge load()`
    override fun load(fields: FieldSet<SCH, FieldDef<SCH, *, *>>, values: Any?): Incremental8<*, *, *, *, *, *, *, *> {
        val flds = fields.bitSet // ∈ { 0, 1, 11, 111, 1111, 11111, 111111, 1111111, 11111111 }
        check(java.lang.Long.bitCount(flds + 1L) == 1) { "got non-incremental field set: " + schema.toString(fields) }
        return when (flds) {
            0L -> emptyIncremental()
            1L -> RealIncremental(arrayOf(values))
            else -> RealIncremental(values as Array<Any?>)
        }
    }

    override fun fields(value: Incremental8<*, *, *, *, *, *, *, *>): FieldSet<SCH, FieldDef<SCH, *, *>> =
        FieldSet((1L shl value.values.size) - 1L)

    override fun store(value: Incremental8<*, *, *, *, *, *, *, *>): Any? = value.values.let {
        when (it.size) {
            0 -> null
            1 -> it[0]
            else -> it
        }
    }

}

inline fun <
    A, DA : DataType<A>
    > incremental(
    type: Box<A, DA>
): DataType.NotNull.Partial<Incremental1<A>, Box<A, DA>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental1<A>, Box<A, DA>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>
    > incremental(
    type: Tuple<A, DA, B, DB>
): DataType.NotNull.Partial<Incremental2<A, B>, Tuple<A, DA, B, DB>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental2<A, B>, Tuple<A, DA, B, DB>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>
    > incremental(
    type: Tuple3<A, DA, B, DB, C, DC>
): DataType.NotNull.Partial<Incremental3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental3<A, B, C>, Tuple3<A, DA, B, DB, C, DC>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>
    > incremental(
    type: Tuple4<A, DA, B, DB, C, DC, D, DD>
): DataType.NotNull.Partial<Incremental4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental4<A, B, C, D>, Tuple4<A, DA, B, DB, C, DC, D, DD>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>,
    E, DE : DataType<E>
    > incremental(
    type: Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>
): DataType.NotNull.Partial<Incremental5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental5<A, B, C, D, E>, Tuple5<A, DA, B, DB, C, DC, D, DD, E, DE>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>,
    E, DE : DataType<E>, F, DF : DataType<F>
    > incremental(
    type: Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>
): DataType.NotNull.Partial<Incremental6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental6<A, B, C, D, E, F>, Tuple6<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>,
    E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>
    > incremental(
    type: Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>
): DataType.NotNull.Partial<Incremental7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental7<A, B, C, D, E, F, G>, Tuple7<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG>>

inline fun <
    A, DA : DataType<A>, B, DB : DataType<B>, C, DC : DataType<C>, D, DD : DataType<D>,
    E, DE : DataType<E>, F, DF : DataType<F>, G, DG : DataType<G>, H, DH : DataType<H>
    > incremental(
    type: Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>
): DataType.NotNull.Partial<Incremental8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>> =
    IncrementalType(type) as DataType.NotNull.Partial<Incremental8<A, B, C, D, E, F, G, H>, Tuple8<A, DA, B, DB, C, DC, D, DD, E, DE, F, DF, G, DG, H, DH>>
