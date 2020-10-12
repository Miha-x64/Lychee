@file:JvmName("ReadWrite")
package net.aquadc.persistence.stream

import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.castNull
import net.aquadc.persistence.each
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.readPartial
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.struct.single
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.SimpleValue


/**
 * Writes [struct] into [output] with help of [this].
 */
fun <D, SCH : Schema<SCH>> BetterDataOutput<D>.write(output: D, struct: Struct<SCH>) {
    struct.schema.write(this, output, struct)
}

/**
 * Writes [value] into [output] with help of [this].
 */
fun <D, T> BetterDataOutput<D>.write(output: D, type: DataType<T>, value: T) {
    type.write(this, output, value)
}

/**
 * Writes a [value] of [this] type into [output] with help of [writer].
 */
fun <D, T> DataType<T>.write(writer: BetterDataOutput<D>, output: D, value: T) {
    val type = if (this is DataType.Nullable<*, *>) actualType as DataType<T> else this
    val nullable = type !== this
    when (type) {
        is DataType.Nullable<*, *> ->
            throw AssertionError()
        is DataType.NotNull.Simple<T> ->
            output.simple(value, nullable, type, writer)
        is DataType.NotNull.Collect<T, *, *> ->
            output.collection(value, nullable, type as DataType.NotNull.Collect<T, Any?, out DataType<Any?>>, writer)
        is DataType.NotNull.Partial<*, *> ->
            output.partial(value, nullable, type as DataType.NotNull.Partial<T, NullSchema>, writer)
    }
}

private fun <D, T> D.simple(arg: T, nullable: Boolean, type: DataType.NotNull.Simple<T>, output: BetterDataOutput<D>) {
    val arg: SimpleValue? = if (nullable && arg === null) null else type.store(arg)
    // these values can be put into stream along with nullability info
    when (type.kind) {
        DataType.NotNull.Simple.Kind.Bool ->
            return output.writeByte(this,
                    when (arg as Boolean?) {
                        true -> 1
                        false -> 0
                        null -> -1
                    }.toByte()
            )
        DataType.NotNull.Simple.Kind.Str -> return output.writeString(this, arg as String?)
        DataType.NotNull.Simple.Kind.Blob -> return output.writeBytes(this, arg as ByteArray?)
        else -> { /* continue */ }
    }

    // these values cannot preserve nullability info, write it ourselves
    if (nullable) {
        val isNull = arg == null
        output.writeByte(this, if (isNull) -1 else 0)
        if (isNull) return
    }

    when (type.kind) {
        DataType.NotNull.Simple.Kind.I32 -> output.writeInt(this, arg as Int)
        DataType.NotNull.Simple.Kind.I64 -> output.writeLong(this, arg as Long)
        DataType.NotNull.Simple.Kind.F32 -> output.writeInt(this, java.lang.Float.floatToIntBits(arg as Float))
        DataType.NotNull.Simple.Kind.F64 -> output.writeLong(this, java.lang.Double.doubleToLongBits(arg as Double))
        DataType.NotNull.Simple.Kind.Bool, DataType.NotNull.Simple.Kind.Str, DataType.NotNull.Simple.Kind.Blob -> throw AssertionError()
    }//.also { ]
}

private fun <D, T, E> D.collection(arg: T, nullable: Boolean, type: DataType.NotNull.Collect<T, E, out DataType<E>>, output: BetterDataOutput<D>) {
    val arg: AnyCollection? = if (nullable && arg === null) null else type.store(arg)
    if (arg === null) {
        output.writeInt(this, -1)
    } else {
        val arg = arg.fatAsList() // maybe small allocation
        // TODO: when [type] is primitive and [arg] is a primitive array, avoid boxing
        output.writeInt(this, arg.size)
        val elementType = type.elementType
        arg.each { /*recur*/ elementType.write<D, E>(output, this, it as E) }
    }
}

private fun <SCH : Schema<SCH>, D, T> D.partial(arg: T, nullable: Boolean, type: DataType.NotNull.Partial<T, SCH>, output: BetterDataOutput<D>) {
    if (nullable && arg === null) {
        output.writeByte(this, (-1).toByte())
    } else {
        val values = type.store(arg)
        val fields = type.fields(arg)
        val size = fields.size
        output.writeByte(this, size.toByte(/* we know is is in 1..64 */))
        val schema = type.schema
        when (size) {
            0 -> { /* nothing to do here */ }
            1 -> {
                writeBoth(output, schema.single(fields), schema, values)
            }
            else -> { // packed or all
                values as Array<*>
                schema.forEachIndexed(fields) { idx, field ->
                    writeBoth(output, field, schema, values[idx])
                }
            }
        }
    }
}

private fun <D, SCH : Schema<SCH>> D.writeBoth(output: BetterDataOutput<D>, field: FieldDef<SCH, *, *>, schema: SCH, values: Any?) {
    output.writeByte(this, field.ordinal)
    (schema.run { (field as FieldDef<SCH, Any?, DataType<Any?>>).type }).write(output, this, values)
}


/**
 * Reads a value of the given [type] from [input] with help of [this].
 */
fun <D, T> BetterDataInput<D>.read(input: D, type: DataType<T>): T {
    val actual = if (type is DataType.Nullable<*, *>) type.actualType as DataType<T> else type
    val nullable = actual !== type
    return when (actual) {
        is DataType.Nullable<*, *> ->
            throw AssertionError()
        is DataType.NotNull.Simple<T> ->
            input.simple(nullable, actual, this)
        is DataType.NotNull.Collect<T, *, *> ->
            input.collection(nullable, actual as DataType.NotNull.Collect<T, Any?, out DataType<Any?>>, this)
        is DataType.NotNull.Partial<*, *> ->
            input.partial(nullable, actual as DataType.NotNull.Partial<T, NullSchema>, this)
    }
}

private val boolDictionary = arrayOf(null, false, true)

private fun <T, D> D.simple(nullable: Boolean, type: DataType.NotNull.Simple<T>, input: BetterDataInput<D>): T {
    @Suppress("IMPLICIT_CAST_TO_ANY") val value = when (type.kind) {
        DataType.NotNull.Simple.Kind.Bool -> boolDictionary[input.readByte(this).toInt() + 1]
        DataType.NotNull.Simple.Kind.Str -> input.readString(this)
        DataType.NotNull.Simple.Kind.Blob -> input.readBytes(this)
        else -> Unit // continue
    }
    if (value != Unit) {
        return if (value === null) castNull(nullable, type::toString) else type.load(value)
    }

    // read separate nullability info

    if (nullable && input.readByte(this) == (-1).toByte()) {
        return null as T
    }

    return type.load(when (type.kind) {
        DataType.NotNull.Simple.Kind.I32 -> input.readInt(this)
        DataType.NotNull.Simple.Kind.I64 -> input.readLong(this)
        DataType.NotNull.Simple.Kind.F32 -> java.lang.Float.intBitsToFloat(input.readInt(this))
        DataType.NotNull.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(input.readLong(this))
        DataType.NotNull.Simple.Kind.Bool, DataType.NotNull.Simple.Kind.Str, DataType.NotNull.Simple.Kind.Blob -> throw AssertionError()
        else -> throw AssertionError()
    })
}

private fun <T, D, E> D.collection(nullable: Boolean, type: DataType.NotNull.Collect<T, E, out DataType<E>>, input: BetterDataInput<D>): T {
    return when (val count = input.readInt(this)) {
        -1 -> castNull(nullable, type::toString)
        0 -> type.load(emptyList<Nothing>())
        else -> type.load(type.elementType.let { elementType ->
            List(count) { /*recur*/ input.read(this, elementType) } // TODO: when [type] is primitive, use specialized collections
        })
    }
}

private val fieldValues = ThreadLocal<ArrayList<Any?>>()
private fun <T, D, SCH : Schema<SCH>> D.partial(nullable: Boolean, type: DataType.NotNull.Partial<T, SCH>, input: BetterDataInput<D>): T =
        input.readByte(this).toInt().let { size ->
            if (size == -1) {
                castNull(nullable, type::toString)
            } else {
                var fieldsLeft = size
                readPartial(
                        type, fieldValues,
                        { if (fieldsLeft == 0) (-1).toByte() else { fieldsLeft--; nextField(input) } },
                        { input.read(this, it as DataType<Any?>) }
                )
            }
        }

private fun <D> D.nextField(input: BetterDataInput<D>): Byte =
    input.readByte(this)
