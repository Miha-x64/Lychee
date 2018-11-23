package net.aquadc.persistence.struct

import java.util.*

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
        append(this@BaseStruct.javaClass.simpleName).append(':').append(schema.name).append('(')
        val fields = schema.fields
        for (i in fields.indices) {
            val field = fields[i]
            append(field.name).append('=').append(this@BaseStruct[field].realToString()).append(", ")
        }
        setLength(length - 2)
        append(')')
    }

    private fun reallyEqual(a: Any?, b: Any?): Boolean = when {
        a == b -> true
        a === null || b === null -> false
        // popular array types
        a is Array<*> -> b is Array<*> && Arrays.equals(a, b)
        a is ByteArray -> b is ByteArray && Arrays.equals(a, b)
        a is IntArray -> b is IntArray && Arrays.equals(a, b)
        a is CharArray -> b is CharArray && Arrays.equals(a, b)
        // other array types
        a is BooleanArray -> b is BooleanArray && Arrays.equals(a, b)
        a is ShortArray -> b is ShortArray && Arrays.equals(a, b)
        a is LongArray -> b is LongArray && Arrays.equals(a, b)
        a is FloatArray -> b is FloatArray && Arrays.equals(a, b)
        a is DoubleArray -> b is DoubleArray && Arrays.equals(a, b)
        // just not equal and not arrays
        else -> false
    }

    private fun Any?.realHashCode(): Int = when (this) {
        null -> 0

        is Array<*> -> Arrays.deepHashCode(this)
        is ByteArray -> Arrays.hashCode(this)
        is IntArray -> Arrays.hashCode(this)
        is CharArray -> Arrays.hashCode(this)

        is BooleanArray -> Arrays.hashCode(this)
        is ShortArray -> Arrays.hashCode(this)
        is LongArray -> Arrays.hashCode(this)
        is FloatArray -> Arrays.hashCode(this)
        is DoubleArray -> Arrays.hashCode(this)

        else -> hashCode()
    }

    private fun Any?.realToString(): String = when (this) {
        null -> "null"

        is Array<*> -> Arrays.deepToString(this)
        is ByteArray -> Arrays.toString(this)
        is IntArray -> Arrays.toString(this)
        is CharArray -> Arrays.toString(this)

        is BooleanArray -> Arrays.toString(this)
        is ShortArray -> Arrays.toString(this)
        is LongArray -> Arrays.toString(this)
        is FloatArray -> Arrays.toString(this)
        is DoubleArray -> Arrays.toString(this)

        else -> toString()
    }

}
