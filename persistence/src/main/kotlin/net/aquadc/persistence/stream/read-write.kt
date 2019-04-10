package net.aquadc.persistence.stream

import android.support.annotation.RestrictTo
import net.aquadc.persistence.fatAsList
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.DataTypeVisitor
import net.aquadc.persistence.type.match


/**
 * Writes [struct] into [output] with help of [this].
 */
fun <D, SCH : Schema<SCH>> BetterDataOutput<D>.write(output: D, struct: Struct<SCH>) {
    struct.schema.fields.forEach { field ->
        writeValueFrom(struct, field, to = output)
    }
}

@Suppress("NOTHING_TO_INLINE") // single use-site, just capture T
private inline fun <D, SCH : Schema<SCH>, T> BetterDataOutput<D>.writeValueFrom(struct: Struct<SCH>, field: FieldDef<SCH, T>, to: D) {
    field.type.write(this, to, struct[field])
}

/**
 * Writes a [value] of [this] type into [output] with help of [writer].
 */
fun <D, T> DataType<T>.write(writer: BetterDataOutput<D>, output: D, value: T) {
    val encoded = encode(value)
    writeEncoded(writer, output, encoded)
}

@JvmSynthetic internal fun <D, T> DataType<T>.writeEncoded(out: BetterDataOutput<D>, put: D, encoded: Any?) {
    out.writerVisitor<T>().match(this, put, encoded)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StreamWriterVisitor<D, T>(
        private val output: BetterDataOutput<D>
) : DataTypeVisitor<D, Any?, T, Unit> {

    override fun D.simple(arg: Any?, raw: DataType<T>, kind: DataType.Simple.Kind) {
        // these values can be put into stream along with nullability info
        val isNullable = raw is DataType.Nullable<*>
        if (arg === null) check(isNullable)
        when (kind) {
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
        if (isNullable) {
            val isNull = arg == null
            output.writeByte(this, if (isNull) -1 else 0)
            if (isNull) return
        }

        when (kind) {
            DataType.Simple.Kind.I8 -> output.writeByte(this, arg as Byte)
            DataType.Simple.Kind.I16 -> output.writeShort(this, arg as Short)
            DataType.Simple.Kind.I32 -> output.writeInt(this, arg as Int)
            DataType.Simple.Kind.I64 -> output.writeLong(this, arg as Long)
            DataType.Simple.Kind.F32 -> output.writeInt(this, java.lang.Float.floatToIntBits(arg as Float))
            DataType.Simple.Kind.F64 -> output.writeLong(this, java.lang.Double.doubleToLongBits(arg as Double))
            DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
        }
    }

    override fun <E> D.collection(arg: Any?, raw: DataType<T>, type: DataType.Collect<T, E>) {
        if (arg === null) {
            check(raw is DataType.Nullable<*>)
            output.writeInt(this, -1)
        } else {
            val arg = arg.fatAsList<Any?>() // maybe small allocation
            // TODO: when [type] is primitive and [arg] is a primitive array, avoid boxing
            output.writeInt(this, arg.size)
            val elementType = type.elementType
            arg.forEach { /*recur*/ elementType.writeEncoded(output, this, it) }
        }
    }

}


/**
 * Reads a [Struct] of the given [schema] from [input] with help of [this].
 */
fun <D, SCH : Schema<SCH>> BetterDataInput<D>.read(input: D, schema: SCH): StructSnapshot<SCH> = schema.build {
    schema.fields.forEach { field ->
        writeValueFrom(input, field, to = it)
    }
}
@Suppress("NOTHING_TO_INLINE") // single call-site, just capture T
private inline fun <D, SCH : Schema<SCH>, T> BetterDataInput<D>.writeValueFrom(input: D, field: FieldDef<SCH, T>, to: StructBuilder<SCH>) {
    to[field] = field.type.read(this, input)
}

/**
 * Reads a value of [this] type from [input] with help of [reader].
 */
fun <D, T> DataType<T>.read(reader: BetterDataInput<D>, input: D): T =
        decode(readEncoded(reader, input))

@JvmSynthetic internal fun <D, T> DataType<T>.readEncoded(inp: BetterDataInput<D>, ut: D) =
        inp.readVisitor<T>().match(this, ut, null)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StreamReaderVisitor<D, T>(
        private val input: BetterDataInput<D>
) : DataTypeVisitor<D, Nothing?, T, Any?> {

    private val boolDictionary = arrayOf(null, false, true)

    override fun D.simple(arg: Nothing?, raw: DataType<T>, kind: DataType.Simple.Kind): Any? {
        when (kind) {
            DataType.Simple.Kind.Bool -> return boolDictionary[input.readByte(this).toInt() + 1]
            DataType.Simple.Kind.Str -> return input.readString(this)
            DataType.Simple.Kind.Blob -> return input.readBytes(this)
            else -> { /* continue */ }
        }

        // read separate nullability info

        if (raw is DataType.Nullable<*> && input.readByte(this) == (-1).toByte()) {
            return null as T
        }

        return when (kind) {
            DataType.Simple.Kind.I8 -> input.readByte(this)
            DataType.Simple.Kind.I16 -> input.readShort(this)
            DataType.Simple.Kind.I32 -> input.readInt(this)
            DataType.Simple.Kind.I64 -> input.readLong(this)
            DataType.Simple.Kind.F32 -> java.lang.Float.intBitsToFloat(input.readInt(this))
            DataType.Simple.Kind.F64 -> java.lang.Double.longBitsToDouble(input.readLong(this))
            DataType.Simple.Kind.Bool, DataType.Simple.Kind.Str, DataType.Simple.Kind.Blob -> throw AssertionError()
        }
    }

    override fun <E> D.collection(arg: Nothing?, raw: DataType<T>, type: DataType.Collect<T, E>): Any? {
        val count = input.readInt(this)
        return when (count) {
            -1 -> null
            0 -> emptyList()
            else -> type.elementType.let { elementType ->
                List(count) { /*recur*/ elementType.readEncoded(input, this) } // TODO: when [type] is primitive, use specialized collections
            }
        }
    }

}
