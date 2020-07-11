package net.aquadc.persistence

import androidx.annotation.RestrictTo
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.emptyFieldSet
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.indexOf
import net.aquadc.persistence.struct.plus
import net.aquadc.persistence.struct.single
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.struct.toString
import net.aquadc.persistence.type.AnyCollection
import net.aquadc.persistence.type.DataType
import java.util.Arrays
import kotlin.concurrent.getOrSet


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun reallyEqual(a: Any?, b: Any?): Boolean = when {
    a == b -> true
    a === null || b === null -> false
    // popular collection types
    a is Array<*> -> b is Array<*> && elementsEq(a, b)
    a is Collection<*> -> b is Collection<*> && elementsEq(a, b)
    a is CharSequence -> b is CharSequence && a.eq(b, false)
    a is ByteArray -> b is ByteArray && Arrays.equals(a, b)
    a is IntArray -> b is IntArray && Arrays.equals(a, b)
    a is CharArray -> b is CharArray && Arrays.equals(a, b)
    // other collection types
    a is BooleanArray -> b is BooleanArray && Arrays.equals(a, b)
    a is ShortArray -> b is ShortArray && Arrays.equals(a, b)
    a is LongArray -> b is LongArray && Arrays.equals(a, b)
    a is FloatArray -> b is FloatArray && Arrays.equals(a, b)
    a is DoubleArray -> b is DoubleArray && Arrays.equals(a, b)
    // just not equal and not arrays
    else -> false
}
private fun elementsEq(a: Array<*>, b: Array<*>): Boolean {
    if (a.size != b.size) return false
    for (i in a.indices)
        if (!reallyEqual(a[i], b[i]))
            return false
    return true
}
private fun elementsEq(a: Collection<*>, b: Collection<*>): Boolean {
    if (a.size != b.size) return false
    val ai = a.iterator()
    val bi = b.iterator()
    while (ai.hasNext())
        if (!bi.hasNext() || !reallyEqual(ai.next(), bi.next()))
            return false
    return true
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Any?.realHashCode(): Int = when (this) {
    null -> 0

    is Array<*> -> hash(this)
    is Collection<*> -> hash(this)
    is String -> hashCode()
    is CharSequence -> hash(this)
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
private fun hash(a: Array<*>): Int {
    var hashCode = 1
    for (e in a) hashCode = 31 * hashCode + (e?.realHashCode() ?: 0)
    return hashCode
}
private fun hash(a: Collection<*>): Int {
    var hashCode = 1
    for (e in a) hashCode = 31 * hashCode + (e?.realHashCode() ?: 0)
    return hashCode
}
private fun hash(a: CharSequence): Int {
    var h = 0
    for (ch in a) h = 31 * h + ch.toInt()
    return h
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun Any?.realToString(): String =
    realToString("[", "]")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Any?.realToString(
    arrPrefix: String, arrPostfix: String // note: these are ignored for primitive arrays
): String = when (this) {
    null -> "null"

    is Array<*> -> map<Any?, String>(Any?::realToString).joinToString(", ", arrPrefix, arrPostfix, -1, "…", null)
    is Collection<*> -> map<Any?, String>(Any?::realToString).joinToString(", ", arrPrefix, arrPostfix, -1, "…", null)
    is ByteArray -> toHexString()
    is IntArray -> Arrays.toString(this)
    is CharArray -> Arrays.toString(this)

    is BooleanArray -> Arrays.toString(this)
    is ShortArray -> Arrays.toString(this)
    is LongArray -> Arrays.toString(this)
    is FloatArray -> Arrays.toString(this)
    is DoubleArray -> Arrays.toString(this)

    else -> toString()
}
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) val HEX_ARRAY = "0123456789ABCDEF".toByteArray()
private fun ByteArray.toHexString(): String =
        String(ByteArray(size * 2).also { hexChars ->
            forEachIndexed { i, b ->
                val v = b.toInt() and 0xFF
                hexChars[i * 2] = HEX_ARRAY[v ushr 4]
                hexChars[i * 2 + 1] = HEX_ARRAY[v and 0x0F]
            }
        })

/*internal inline fun <T, R> AnyCollection.fatMap(transform: (T) -> R): List<R> = when (this) {
    is List<*> -> Array<Any?>(size) { transform(this[it] as T) }
    is Collection<*> -> arrayOfNulls<Any>(size).also { dest ->
        forEachIndexed<Any?> { i, el -> dest[i] = transform(el as T) }
    }
    is Array<*> -> Array<Any?>(size) { transform(this[it] as T) }
    is ByteArray -> Array<Any?>(size) { transform(this[it] as T) }
    is ShortArray -> Array<Any?>(size) { transform(this[it] as T) }
    is IntArray -> Array<Any?>(size) { transform(this[it] as T) }
    is LongArray -> Array<Any?>(size) { transform(this[it] as T) }
    is FloatArray -> Array<Any?>(size) { transform(this[it] as T) }
    is DoubleArray -> Array<Any?>(size) { transform(this[it] as T) }
    else -> throw AssertionError()
}.asList() as List<R>*/

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <C : MutableCollection<R>, T, R> AnyCollection.fatMapTo(dest: C, transform: (T) -> R): C = when (this) {
    is Collection<*> -> (this as Collection<T>).mapTo(dest, transform)
    is Array<*> -> (this as Array<T>).mapTo(dest, transform)
    is ByteArray -> this.mapTo(dest) { transform(it as T) }
    is ShortArray -> this.mapTo(dest) { transform(it as T) }
    is IntArray -> this.mapTo(dest) { transform(it as T) }
    is LongArray -> this.mapTo(dest) { transform(it as T) }
    is FloatArray -> this.mapTo(dest) { transform(it as T) }
    is DoubleArray -> this.mapTo(dest) { transform(it as T) }
    else -> throw AssertionError()
}

internal fun <C : MutableCollection<T>, T> AnyCollection.fatTo(dest: C): C {
    when (this) {
        is Collection<*> -> (this as Collection<T>).toCollection(dest)
        is Array<*> -> (this as Array<T>).toCollection(dest)
        is ByteArray -> this.toCollection(dest as MutableCollection<in Byte>)
        is ShortArray -> this.toCollection(dest as MutableCollection<in Short>)
        is IntArray -> this.toCollection(dest as MutableCollection<in Int>)
        is LongArray -> this.toCollection(dest as MutableCollection<in Long>)
        is FloatArray -> this.toCollection(dest as MutableCollection<in Float>)
        is DoubleArray -> this.toCollection(dest as MutableCollection<in Double>)
        else -> throw AssertionError()
    }
    return dest
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AnyCollection.fatAsList(): List<Any?> = when (this) {
    is List<*> -> this
    is Collection<*> -> this.toList()
    is Array<*> -> this.asList()
    is ByteArray -> this.asList()
    is ShortArray -> this.asList()
    is IntArray -> this.asList()
    is LongArray -> this.asList()
    is FloatArray -> this.asList()
    is DoubleArray -> this.asList()
    else -> throw AssertionError()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <E> List<E>.each(consume: (E) -> Unit) {
    for (i in indices) {
        consume(this[i])
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <SCH : Schema<SCH>> Struct<SCH>.valuesAndSchema(): Array<Any?> {
    val fields = schema.fields
    val fieldCount = fields.size
    val array = arrayOfNulls<Any>(fieldCount + 1)
    fields.forEachIndexed { i, f -> array[i] = this[f] }
    array[fieldCount] = schema
    return array
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <reified T> List<T>.array(plusSize: Int = 0): Array<T> =
        (this as java.util.List<T>).toArray(arrayOfNulls<T>(size + plusSize))

internal fun <SCH : Schema<SCH>> fill(builder: StructBuilder<SCH>, schema: SCH, fields: FieldSet<SCH, FieldDef<SCH, *, *>>, values: Any?) {
    when (fields.size) {
        0 -> {
        } // empty. Nothing to do here!

        1 ->
            builder[schema.single(fields) as FieldDef<SCH, Any?, *>] = values

        (values as Array<*>).size -> { // 'packed'
            schema.forEachIndexed(fields) { idx, field ->
                builder[field as FieldDef<SCH, Any?, *>] = values[idx]
            }
        }

        else -> error("cannot set ${schema.toString(fields)} to ${values.realToString()}: inconsistent sizes")
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <SCH : Schema<SCH>> PartialStruct<SCH>.fieldValues(): Any? {
    val fields = fields
    return when (val fieldCount = fields.size) {
        0 -> null
        1 -> getOrThrow(schema.single(fields))
        else -> {
            val values = arrayOfNulls<Any>(fieldCount)
            schema.forEachIndexed(fields) { idx, field ->
                values[idx] = getOrThrow(field)
            }
            values
        }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
inline fun <T, SCH : Schema<SCH>> readPartial(
    type: DataType.NotNull.Partial<T, SCH>, fieldValues: ThreadLocal<ArrayList<Any?>>,
    maybeReadNextField: () -> FieldDef<SCH, *, *>?,
    readNextValue: (DataType<*>) -> Any?
): T {
    var fields = emptyFieldSet<SCH, FieldDef<SCH, *, *>>()
    var values: Any? = null

    val firstField = maybeReadNextField()
    if (firstField != null) {
        fields += firstField

        val schema = type.schema
        values = readNextValue(schema.run { (firstField as FieldDef<SCH, Any?, DataType<Any?>>).type })
        // if the first field is the only one,
        // we must pass it to Partial factory without allocating an array

        // else proceed reading the following fields
        var nextField = maybeReadNextField() as FieldDef<SCH, Any?, DataType<Any?>>?
        if (nextField != null) {
            val fieldValues = fieldValues.getOrSet(::ArrayList)
            try {
                fieldValues.add(firstField)
                fieldValues.add(values)

                while (nextField != null) {
                    val newFields = fields.forceAddField(nextField, schema)
                    val value = readNextValue(schema.run { nextField!!.type })

                    fieldValues.ensureCapacity(fieldValues.size + 2)

                    // nothing crashed, commit
                    fields = newFields
                    fieldValues.add(nextField)
                    fieldValues.add(value)

                    // this still could OOM but we've already updated `fields` along with `fieldValues`
                    nextField = maybeReadNextField() as FieldDef<SCH, Any?, DataType<Any?>>?
                }
            } catch (t: Throwable) {
                // if something goes wrong (especially within read()),
                // we're gonna pop everything we've pushed, saving the rest of application from memory leaks
                fieldValues.pop(2 * fields.size)
                throw t
            }

            values = gatherValues(fields, fieldValues)
        }
    }
    return type.load(fields, values)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <SCH : Schema<SCH>> FieldSet<SCH, FieldDef<SCH, *, *>>.forceAddField(
    that: FieldDef<SCH, *, *>,
    schema: SCH
): FieldSet<SCH, FieldDef<SCH, *, *>> {
    val newFields = this + that
    if (bitSet == newFields.bitSet) {
        throw UnsupportedOperationException("duplicate name: ${schema.run { that.name }}")
    }
    return newFields
}

@PublishedApi
internal fun <T> ArrayList<T>.pop(): T =
        removeAt(size - 1)

@PublishedApi
internal fun ArrayList<*>.pop(count: Int) {
    // yep, I don't know operators precedence :)
    val size = size
    for (i in (size - 1) downTo (size - count)) {
        removeAt(i)
    }
}

@PublishedApi
internal fun <SCH : Schema<SCH>> gatherValues(fields: FieldSet<SCH, FieldDef<SCH, *, *>>, fieldValues: ArrayList<Any?>): Array<Any?> {
    val fieldCount = fields.size
    val values = arrayOfNulls<Any>(fieldCount)
    repeat(fieldCount) { _ ->
        val value = fieldValues.pop()
        val field = fieldValues.pop() as FieldDef<SCH, *, *>
        values[fields.indexOf(field).toInt()] = value
    }
    return values
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun hasFraction(nextNumber: String): Boolean {
    val delimiter = nextNumber.indexOf('.')
    if (delimiter < 0) return false // no fractional part
    var fractionDigits = 0
    var lastMeaningfulFractionAt = -1
    val firstFractionDigit = delimiter + 1
    var i = firstFractionDigit
    while (i < nextNumber.length) {
        fractionDigits++
        @Suppress("ControlFlowWithEmptyBody") if (nextNumber[i] == '0') {} // this empty `if` is just great
        else if (nextNumber[i] in '1'..'9') lastMeaningfulFractionAt = i
        else if (nextNumber[i] == 'e' || nextNumber[i] == 'E') {
            fractionDigits-- // exponent is not a digit, rollback increment
            fractionDigits -= nextNumber.substring(i+1).toInt() // e. g. 1.34e+3: fractionDigits=2, exp=3, number 1340 is not fractional
            break
        } else throw NumberFormatException("malformed number $nextNumber")
        i++
    }

    fractionDigits -= (lastMeaningfulFractionAt - i) // 1.00001000 -> 1.00001
    return fractionDigits > 0
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) fun CharSequence.eq(that: CharSequence, ignoreCase: Boolean): Boolean {
    val len = length
    if (that.length != len) return false
    return regionMatches(0, that, 0, len, ignoreCase)
}
