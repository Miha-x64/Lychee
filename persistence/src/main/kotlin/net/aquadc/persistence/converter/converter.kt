package net.aquadc.persistence.converter


/**
 * Represents an abstract way of converting a single value of [T].
 */
interface Converter<T> {
    val isNullable: Boolean
    val dataType: DataType
}

sealed class DataType {
    class Integer internal constructor(val sizeBits: Int, val signed: Boolean) : DataType()
    class Float internal constructor(val sizeBits: Int) : DataType()
    class String internal constructor(val maxLength: Int) : DataType()
    class Blob internal constructor(val maxLength: Int) : DataType()
}

/**
 * Describes supported basic data types.
 * Any converter must be able to describe its contents using [DataType],
 * so data transfer protocol implementations could be able to handle it.
 */
object DataTypes {
    val Bool = DataType.Integer(1, false)
    val Int8 = DataType.Integer(8, true)
    val Int16 = DataType.Integer(16, true)
    val Int32 = DataType.Integer(32, true)
    val Int64 = DataType.Integer(64, true)

    val Float32 = DataType.Float(32)
    val Float64 = DataType.Float(64)

    val SmallString = DataType.String(Byte.MAX_VALUE.toInt())
    val MediumString = DataType.String(Short.MAX_VALUE.toInt())
    val LargeString = DataType.String(Int.MAX_VALUE)

    val SmallBlob = DataType.Blob(Byte.MAX_VALUE.toInt())
    val MediumBlob = DataType.Blob(Short.MAX_VALUE.toInt())
    val LargeBlob = DataType.Blob(Int.MAX_VALUE)
}

/**
 * Cool converter supporting both JDBC and Android.
 */
interface UniversalConverter<T> :
        JdbcConverter<T>, AndroidSqliteConverter<T>, // databases
        DataIoConverter<T> // IO streams
