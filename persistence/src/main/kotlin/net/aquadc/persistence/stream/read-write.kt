package net.aquadc.persistence.stream

import android.support.annotation.RestrictTo
import net.aquadc.persistence.each
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.emptyFieldSet
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.plus
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
    struct.schema.fields.forEach { field ->
        writeValueFrom(struct, field, to = output)
    }
}

@Suppress("NOTHING_TO_INLINE") // just capture T
private inline fun <D, SCH : Schema<SCH>, T> BetterDataOutput<D>.writeValueFrom(struct: PartialStruct<SCH>, field: FieldDef<SCH, T>, to: D) {
    field.type.write(this, to, struct[field])
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

    override fun <E> D.collection(arg: T, nullable: Boolean, type: DataType.Collect<T, E>) {
        val arg: AnyCollection? = if (nullable && arg === null) null else type.store(arg)
        if (arg === null) {
            output.writeInt(this, -1)
        } else {
            val arg = arg.fatAsList<Any?>() // maybe small allocation
            // TODO: when [type] is primitive and [arg] is a primitive array, avoid boxing
            output.writeInt(this, arg.size)
            val elementType = type.elementType
            arg.each { /*recur*/ elementType.write(output, this, it as E) }
        }
    }

    override fun <SCH : Schema<SCH>> D.partial(arg: T, nullable: Boolean, type: DataType.Partial<T, SCH>) {
        if (nullable && arg === null) {
            output.writeByte(this, (-1).toByte())
        } else {
            val partial = type.store(arg)
            val fields = partial.fields
            output.writeByte(this, fields.size)
            type.schema.forEach<SCH, FieldDef<SCH, *>>(fields) { field ->
                output.writeByte(this, field.ordinal)
                output.writeValueFrom(partial, field, this)
            }
        }
    }

}


/**
 * Reads a [Struct] of the given [schema] from [input] with help of [this].
 */
fun <D, SCH : Schema<SCH>> BetterDataInput<D>.read(input: D, schema: SCH): StructSnapshot<SCH> = schema.build {
    schema.fields.forEach { field ->
        readValueFrom(input, field, to = it)
    }
}
@Suppress("NOTHING_TO_INLINE") // single call-site, just capture T
private inline fun <D, SCH : Schema<SCH>, T> BetterDataInput<D>.readValueFrom(input: D, field: FieldDef<SCH, T>, to: StructBuilder<SCH>) {
    to[field] = field.type.read(this, input)
}

/**
 * Reads a value of [this] type from [input] with help of [reader].
 */
fun <D, T> DataType<T>.read(reader: BetterDataInput<D>, input: D): T =
        reader.readVisitor<T>().match(this, input, null)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StreamReaderVisitor<D, T>(
        private val input: BetterDataInput<D>
) : DataTypeVisitor<D, Nothing?, T, T> {

    private val boolDictionary = arrayOf(null, false, true)

    override fun D.simple(arg: Nothing?, nullable: Boolean, type: DataType.Simple<T>): T {
        val value = when (type.kind) {
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

    override fun <E> D.collection(arg: Nothing?, nullable: Boolean, type: DataType.Collect<T, E>): T {
        val count = input.readInt(this)
        return when (count) {
            -1 -> check(nullable).let { null as T }
            0 -> type.load(emptyList<Nothing>())
            else -> type.load(type.elementType.let { elementType ->
                List(count) { /*recur*/ elementType.read(input, this) } // TODO: when [type] is primitive, use specialized collections
            })
        }
    }

    override fun <SCH : Schema<SCH>> D.partial(arg: Nothing?, nullable: Boolean, type: DataType.Partial<T, SCH>): T =
            input.readByte(this).let { size ->
                if (size == (-1).toByte()) {
                    check(nullable)
                    null as T
                } else {
                    var fields = emptyFieldSet<SCH, FieldDef<SCH, *>>()
                    val schema = type.schema
                    val allFields = schema.fields
                    val values = arrayOfNulls<Any>(allFields.size)
                    repeat(size.toInt()) { _ ->
                        val ordinal = input.readByte(this).toInt()
                        val field = allFields[ordinal]
                        fields += field
                        values[ordinal] = field.type.read(input, this)
                    }
                    type.load(fields, values)
                }
            }

}
