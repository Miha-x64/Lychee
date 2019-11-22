package net.aquadc.persistence.stream

import androidx.annotation.RestrictTo
import net.aquadc.persistence.each
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.readPartial
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.single
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.SimpleValue
import net.aquadc.persistence.type.match


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
    writer.writerVisitor<T>().match(this, output, value)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StreamWriterVisitor<D, T>(
        private val output: BetterDataOutput<D>
) : DataTypeVisitor<D, T, T, Unit> {

    override fun D.simple(arg: T, nullable: Boolean, type: DataType.Simple<T>) {
        val arg: SimpleValue? = if (nullable && arg === null) null else type.store(arg)
        // these values can be put into stream along with nullability info
        when (type.kind) {
            DataType.Simple.Kind.Bool ->
                return output.writeByte(this,
                        when (arg as Boolean?) {
                            true -> 1
                            false -> 0
                            null -> -1
                        }.toByte()
                )
            DataType.Simple.Kind.Str -> return output.writeString(this, arg as String?)
            DataType.Simple.Kind.Blob -> return output.writeBytes(this, arg as ByteArray?)
            else -> { /* continue */ }
        }

        // these values cannot preserve nullability info, write it ourselves
        if (nullable) {
            val isNull = arg == null
            output.writeByte(this, if (isNull) -1 else 0)
            if (isNull) return
        }

        when (type.kind) {
            DataType.Simple.Kind.I8 -> output.writeByte(this, arg as Byte)
            DataType.Simple.Kind.I16 -> output.writeShort(this, arg as Short)
            DataType.Simple.Kind.I32 -> output.writeInt(this, arg as Int)
            DataType.Simple.Kind.I64 -> output.writeLong(this, arg as Long)
            DataType.Simple.Kind.F32 -> output.writeInt(this, java.lang.Float.floatToIntBits(arg as Float))
            DataType.Simple.Kind.F64 -> output.writeLong(this, java.lang.Double.doubleToLongBits(arg as Double))
            DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
        }
    }

    override fun <E> D.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>) {
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

    override fun <SCH : Schema<SCH>> D.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
        if (nullable && arg === null) {
            output.writeByte(this, (-1).toByte())
        } else {
            val values = type.store(arg)
            val fields = type.fields(arg)
            val size = fields.size
            output.writeByte(this, size)
            when (size.toInt()) {
                0 -> { /* nothing to do here */ }
                1 -> {
                    val field = type.schema.single(fields)
                    output.writeByte(this, field.ordinal)
                    (field.type as DataType<Any?>).write(output, this, values)
                }
                else -> { // packed or all
                    values as Array<*>
                    type.schema.forEachIndexed(fields) { idx, field ->
                        output.writeByte(this, field.ordinal)
                        (field.type as DataType<Any?>).write(output, this, values[idx])
                    }
                }
            }
        }
    }

}


/**
 * Reads a value of the given [type] from [input] with help of [this].
 */
fun <D, T> BetterDataInput<D>.read(input: D, type: DataType<T>): T =
        this.readVisitor<T>().match(type, input, null)


/**
 * Reads a value of [this] type from [input] with help of [reader].
 */
@Deprecated("public version has another signature", ReplaceWith("reader.read<D, T>(input, this)"), DeprecationLevel.ERROR)
private fun <D, T> DataType<T>.read(reader: BetterDataInput<D>, input: D): T =
        reader.readVisitor<T>().match(this, input, null)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StreamReaderVisitor<D, T>(
        private val input: BetterDataInput<D>
) : DataTypeVisitor<D, Nothing?, T, T> {

    private val boolDictionary = arrayOf(null, false, true)

    override fun D.simple(arg: Nothing?, nullable: Boolean, type: DataType.Simple<T>): T {
        @Suppress("IMPLICIT_CAST_TO_ANY") val value = when (type.kind) {
            DataType.Simple.Kind.Bool -> boolDictionary[input.readByte(this).toInt() + 1]
            DataType.Simple.Kind.Str -> input.readString(this)
            DataType.Simple.Kind.Blob -> input.readBytes(this)
            else -> Unit // continue
        }
        if (value != Unit) {
            return if (value === null) check(nullable).let { null as T } else type.load(value)
        }

        // read separate nullability info

        if (nullable && input.readByte(this) == (-1).toByte()) {
            return null as T
        }

        return type.load(when (type.kind) {
            DataType.Simple.Kind.I8 -> input.readByte(this)
            DataType.Simple.Kind.I16 -> input.readShort(this)
            DataType.Simple.Kind.I32 -> input.readInt(this)
            DataType.Simple.Kind.I64 -> input.readLong(this)
            DataType.Simple.Kind.F32 -> java.lang.Float.intBitsToFloat(input.readInt(this))
            DataType.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(input.readLong(this))
            DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
        })
    }

    override fun <E> D.collection(arg: Nothing?, nullable: Boolean, type: DataType.Collect<T, E, out DataType<E>>): T {
        return when (val count = input.readInt(this)) {
            -1 -> check(nullable).let { null as T }
            0 -> type.load(emptyList<Nothing>())
            else -> type.load(type.elementType.let { elementType ->
                List(count) { /*recur*/ input.read(this, elementType) } // TODO: when [type] is primitive, use specialized collections
            })
        }
    }

    private val fieldValues = ThreadLocal<ArrayList<Any?>>()
    override fun <SCH : Schema<SCH>> D.partial(arg: Nothing?, nullable: Boolean, type: DataType.Partial<T, SCH>): T =
            input.readByte(this).toInt().let { size ->
                if (size == -1) {
                    check(nullable)
                    null as T
                } else {
                    val fields = type.schema.fields
                    var fieldsLeft = size
                    readPartial(
                            type, fieldValues,
                            { if (fieldsLeft == 0) null else { fieldsLeft--; nextField(fields) } },
                            { input.read(this, it as DataType<Any?>) }
                    )
                }
            }

    private fun <SCH : Schema<SCH>> D.nextField(fields: Array<out FieldDef<SCH, *, *>>): FieldDef<SCH, *, *> =
            fields[input.readByte(this).toInt()]

}
