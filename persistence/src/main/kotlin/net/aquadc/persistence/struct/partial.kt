package net.aquadc.persistence.struct

import net.aquadc.persistence.type.DataType

/**
 * Represents an instance of a partial struct —
 * a map with [FieldDef] keys (can be treated as [String] or [Byte])
 * and heterogeneous statically typed values. Having a value for each key is not required.
 * @see Schema
 * @see DataType.Partial
 * @see FieldDef
 * @see PartialStruct
 * see extended-persistence
 */
interface PartialStruct<SCH : Schema<SCH>> {

    /**
     * Represents the type of this struct.
     */
    val schema: SCH

    /**
     * Returns a set of fields which have values.
     */
    val fields: FieldSet<SCH, FieldDef<SCH, *, *>>

    /**
     * Returns the value of the requested field, if it is present.
     * @throws NoSuchElementException if requested value is absent
     */
    fun <T> getOrThrow(field: FieldDef<SCH, T, *>): T

}
