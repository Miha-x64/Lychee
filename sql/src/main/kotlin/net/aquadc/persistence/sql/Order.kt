package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema


class Order<SCH : Schema<SCH>>(
        @JvmField internal val col: FieldDef<SCH, *, *>,
        @JvmField internal val desc: Boolean
) {
    // may become an inline-class when hashCode/equals will be allowed

    override fun hashCode(): Int = // yep, orders on different structs may have interfering hashes
            (if (desc) 0x100 else 0) or col.ordinal.toInt()

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is Order<*> && other.col === col && other.desc == desc)

}

val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.asc: Order<SCH>
    get() = Order(this, false)

val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.desc: Order<SCH>
    get() = Order(this, true)
