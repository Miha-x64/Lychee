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
) : PartialStruct<SCH> {

    override fun equals(other: Any?): Boolean {
        if (other !is PartialStruct<*> || other.schema !== schema || other.fields.bitSet != fields.bitSet) {
            return false
        }

        @Suppress("UNCHECKED_CAST") // other.type is our type, so it's safe
        other as PartialStruct<SCH>

        schema.forEach(fields) { field ->
            val our = getOrThrow(field)
            val their = other.getOrThrow(field)
            if (!reallyEqual(our, their)) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 0

        schema.forEach(fields) { field ->
            result = 31 * result + this.getOrThrow(field).realHashCode()
        }
        return result
    }

    override fun toString(): String = buildString {
        append(this@BaseStruct.javaClass.simpleName).append(':').append(schema.javaClass.simpleName).append('(')
        schema.forEach(fields) { field ->
            append(field.name).append('=').append(getOrThrow(field).realToString()).append(", ")
        }
        if (!fields.isEmpty) {
            setLength(length - 2)
        }
        append(')')
    }

}
