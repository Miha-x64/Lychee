@file:JvmName("OrderBy")
package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.ImmutableField
import net.aquadc.persistence.struct.MutableField
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.upcast
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.type.DataType


class Order<SCH : Schema<SCH>>(
        @JvmField internal val col: FieldDef<SCH, *, *>, // todo: should be StoredLens
        @JvmField internal val desc: Boolean
) {

    override fun hashCode(): Int = // yep, orders on different structs may have interfering hashes
            (if (desc) 0x100 else 0) or col.ordinal

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is Order<*> && other.col === col && other.desc == desc)

}

inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.asc: Order<SCH>
    get() = Order(this, false)
inline val <SCH : Schema<SCH>> MutableField<SCH, *, *>.asc: Order<SCH>
    get() = (this as MutableField<SCH, Any?, DataType<Any?>>).upcast().asc
inline val <SCH : Schema<SCH>> ImmutableField<SCH, *, *>.asc: Order<SCH>
    get() = (this as ImmutableField<SCH, Any?, DataType<Any?>>).upcast().asc

inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.desc: Order<SCH>
    get() = Order(this, true)
inline val <SCH : Schema<SCH>> MutableField<SCH, *, *>.desc: Order<SCH>
    get() = (this as MutableField<SCH, Any?, DataType<Any?>>).upcast().desc
inline val <SCH : Schema<SCH>> ImmutableField<SCH, *, *>.desc: Order<SCH>
    get() = (this as ImmutableField<SCH, Any?, DataType<Any?>>).upcast().desc
