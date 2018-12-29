package net.aquadc.persistence.struct

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual

/**
 * Implements basic [Struct] capabilities.
 * Used for implementation inheritance, adds no contract and should not be treated as a separate type.
 */
abstract class BaseStruct<SCH : Schema<SCH>>(
        final override val schema: SCH
) : Struct<SCH> {

    override fun equals(other: Any?): Boolean {
        if (other !is Struct<*> || other.schema !== schema) return false
        @Suppress("UNCHECKED_CAST")
        other as Struct<SCH> // other.type is our type, so it's safe
        val fields = schema.fields
        for (i in fields.indices) {
            val field = fields[i]
            val our = this[field]
            val their = other[field]
            if (!reallyEqual(our, their)) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 0
        val fields = schema.fields
        for (i in fields.indices) {
            result = 31 * result + this[fields[i]].realHashCode()
        }
        return result
    }

    override fun toString(): String = buildString {
        append(this@BaseStruct.javaClass.simpleName).append(':').append(schema.javaClass.simpleName).append('(')
        val fields = schema.fields
        for (i in fields.indices) {
            val field = fields[i]
            append(field.name).append('=').append(this@BaseStruct[field].realToString()).append(", ")
        }
        setLength(length - 2)
        append(')')
    }

}
