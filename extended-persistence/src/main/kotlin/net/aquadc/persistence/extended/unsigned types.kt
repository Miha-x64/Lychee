@file:JvmName("UnsignedTypes")
@file:UseExperimental(ExperimentalUnsignedTypes::class)
package net.aquadc.persistence.extended

import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleValue


@JvmField val uByte: DataType.Simple<UByte> = object : DataType.Simple<UByte>(Kind.I16) {

    override fun load(value: SimpleValue): UByte {
        val value = value as Short
        require((value.toInt() and 0xFF) == value.toInt()) {
            "value $value cannot be fit into ${UByte::class.java.simpleName}"
        }
        return value.toUByte()
    }

    override fun store(value: UByte): SimpleValue =
            value.toShort()

}

@JvmField val uShort: DataType.Simple<UShort> = object : DataType.Simple<UShort>(Kind.I32) {

    override fun load(value: SimpleValue): UShort {
        val value = value as Int
        require((value and 0xFFFF) == value) {
            "value $value cannot be fit into ${UShort::class.java.simpleName}"
        }
        return value.toUShort()
    }

    override fun store(value: UShort): SimpleValue =
            value.toInt()

}

@JvmField val uInt: DataType.Simple<UInt> = object : DataType.Simple<UInt>(Kind.I64) {

    override fun load(value: SimpleValue): UInt {
        val value = value as Long
        require((value and 0xFFFFFFFF) == value) {
            "value $value cannot be fit into ${UInt::class.java.simpleName}"
        }
        return value.toUInt()
    }

    override fun store(value: UInt): SimpleValue =
            value.toLong()

}

// ULong cannot be expressed using a primitive type ¯\_(ツ)_/¯
