@file:JvmName("OrderBy")
package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.ordinal

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.")
class Order<SCH : Schema<SCH>>(
        @JvmField internal val col: FieldDef<SCH, *, *>, // todo: should be StoredLens
        @JvmField internal val desc: Boolean
) {

    override fun hashCode(): Int = // yep, orders on different structs may have interfering hashes
            (if (desc) 0x100 else 0) or col.ordinal.toInt()

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is Order<*> && other.col === col && other.desc == desc)

}

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.")
inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.asc: Order<SCH>
    get() = Order(this, false)

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.")
inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.desc: Order<SCH>
    get() = Order(this, true)
