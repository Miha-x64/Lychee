@file:JvmName("OrderBy")
package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
typealias Order<SCH> = Nothing

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.asc: Nothing
    get() = throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
inline val <SCH : Schema<SCH>> FieldDef<SCH, *, *>.desc: Nothing
    get() = throw AssertionError()
