package net.aquadc.persistence.struct

import androidx.annotation.RestrictTo
import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.realToString
import net.aquadc.persistence.reallyEqual
import net.aquadc.persistence.valuesAndSchema

/**
 * Represents a fully immutable snapshot of a struct.
 */
inline class StructSnapshot<SCH : Schema<SCH>>
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
        private val values: Array<Any?>
) : Struct<SCH> {

    constructor(source: Struct<SCH>) : this(source.valuesAndSchema())

    override val schema: SCH
        get() = values[values.lastIndex] as SCH

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<SCH, T, *>): T =
            values[field.ordinal.toInt()] as T

    // copy-paste from BaseStruct specialized for non-partial

    @Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")
    override fun equals(other: Any?): Boolean {
        if (other !is PartialStruct<*> || other.schema !== schema || other.fields.bitSet != fields.bitSet) {
            return false
        }

        @Suppress("UNCHECKED_CAST") // other.type is our type, so it's safe
        other as PartialStruct<SCH>

        schema.forEach(schema.allFieldSet) { field -> // ignore `fields`: we're full, other.fields = our.fields
            val our = get(field)
            val their = other.getOrThrow(field)
            if (!reallyEqual(our, their)) return false
        }
        return true
    }

    @Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")
    override fun hashCode(): Int {
        var result = 0
        schema.forEach(schema.allFieldSet) { field ->
            result = 31 * result + getOrThrow(field).realHashCode()
        }
        return result
    }

    override fun toString(): String = buildString {
        append("StructSnapshot").append(':').append(schema.javaClass.simpleName).append('(')
        schema.forEach(schema.allFieldSet) { field ->
            append(schema.run { field.name }).append('=').append(this@StructSnapshot[field].realToString()).append(", ")
        }
        /*if (schema.fields.isNotEmpty() is always true)*/ setLength(length - 2)
        append(')')
    }

}
