@file:JvmName("NumberTypes")
package net.aquadc.persistence.type


private class Ints<T>(isNullable: Boolean, sizeBits: Int) : DataType.Integer<T>(isNullable, sizeBits) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asNumber(value: T): Any {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            1 -> value as Boolean
            8 -> value as Byte
            16 -> value as Short
            32 -> value as Int
            64 -> value as Long
            else -> throw AssertionError()
        }
    }

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Any): T = when (sizeBits) {
        1 -> value as Boolean
        8 -> value as Byte
        16 -> value as Short
        32 -> value as Int
        64 -> value as Long
        else -> throw AssertionError()
    } as T

}

@JvmField val bool: DataType.Integer<Boolean> = Ints(false, 1)
@JvmField val nullableBool: DataType.Integer<Boolean?> = Ints(true, 1)

@JvmField val byte: DataType.Integer<Byte> = Ints(false, 8)
@JvmField val nullableByte: DataType.Integer<Byte?> = Ints(true, 8)

@JvmField val short: DataType.Integer<Short> = Ints(false, 16)
@JvmField val nullableShort: DataType.Integer<Short?> = Ints(true, 16)

@JvmField val int: DataType.Integer<Int> = Ints(false, 32)
@JvmField val nullableInt: DataType.Integer<Int?> = Ints(true, 32)

@JvmField val long: DataType.Integer<Long> = Ints(false, 64)
@JvmField val nullableLong: DataType.Integer<Long?> = Ints(true, 64)


private class Floats<T>(isNullable: Boolean, sizeBits: Int) : DataType.Floating<T>(isNullable, sizeBits) {

    /**
     * {@implNote does nothing but sanity checks}
     */
    override fun asNumber(value: T): Number {
        if (value === null) throw NullPointerException()
        return when (sizeBits) {
            32 -> value as Float
            64 -> value as Double
            else -> throw AssertionError()
        }
    }

    /**
     * {@implNote does nothing but sanity checks}
     */
    @Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
    override fun asT(value: Number): T = when (sizeBits) {
        32 -> value as Float
        64 -> value as Double
        else -> throw AssertionError()
    } as T

}

@JvmField val float: DataType<Float> = Floats(false, 32)
@JvmField val nullableFloat: DataType<Float?> = Floats(true, 32)

@JvmField val double: DataType<Double> = Floats(false, 64)
@JvmField val nullableDouble: DataType<Double?> = Floats(true, 64)
