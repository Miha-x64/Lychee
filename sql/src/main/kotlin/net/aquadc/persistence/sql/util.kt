@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.persistence.sql

import net.aquadc.persistence.NullSchema
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredNamedLens
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.indexOf
import net.aquadc.persistence.struct.ordinal
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.PlatformType
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.serialized
import java.sql.PreparedStatement
import java.sql.ResultSet
import net.aquadc.persistence.struct.contains as originalContains


internal inline fun <T, R> DataType<T>.flattened(func: (isNullable: Boolean, simple: DataType.NotNull.Simple<T>) -> R): R =
        when (this) {
            is DataType.Nullable<*, *> -> {
                when (val actualType = actualType as DataType<T>) {
                    is DataType.Nullable<*, *> -> throw AssertionError()
                    is DataType.NotNull.Simple -> func(true, actualType)
                    is DataType.NotNull.Collect<*, *, *>,
                    is DataType.NotNull.Partial<*, *> -> func(true, serialized(actualType))
                }
            }
            is DataType.NotNull.Simple -> func(false, this)
            is DataType.NotNull.Collect<*, *, *>,
            is DataType.NotNull.Partial<*, *> -> func(false, serialized(this))
        }

internal inline fun <SCH : Schema<SCH>, ID : IdBound> bindQueryParams( // todo inline me
    table: Table<SCH, ID>, pk: ID, bind: (Ilk<Any?, *>, idx: Int, value: Any?) -> Unit
) {
    bind(table.idColType as Ilk<Any?, *>, 0, pk)
}

internal inline fun <SCH : Schema<SCH>> bindInsertionParams(
    table: Table<SCH, *>,
    data: PartialStruct<SCH>,
    fields: FieldSet<SCH, *> = data.fields,
    bind: (Ilk<Any?, *>, idx: Int, value: Any?) -> Unit
): Int {
    var tmp: Array<Any?>? = null
    var idx = 0
    table.schema.forEach(fields) { f ->
        val d = table.delegateFor(f)
        val cc = d.colCount
        if (tmp == null || tmp.size < cc) tmp = arrayOfNulls(cc)
        d.flattenTo(tmp, table, f as FieldDef<SCH, Any?, *>, data.getOrThrow(f))
        repeat(cc) {
            bind(d.typeAt(table, f, it) as Ilk<Any?, *>, idx++, tmp[it])
        }
    }
    return idx
}

internal inline fun <T, reified R> Array<T>.mapIndexedToArray(transform: (Int, T) -> R): Array<R> {
    return Array(size) { transform(it, this[it]) }
}

/**
 * Transforms flat column values to in-memory instance.
 * Puts the resulting [StructSnapshot] into [mutColumnValues] at [_dstPos].
 */
internal fun inflate(
        recipe: Array<out Table.StructStart?>,
        mutColumnValues: Array<Any?>,
        _srcPos: Int,
        _dstPos: Int,
        _recipeOffset: Int
) {
    val start = recipe[_recipeOffset]!!

    var srcPos = _srcPos
    val schema = start.unwrappedType.schema
    val fieldSet = if (start.hasFieldSet) {
        (mutColumnValues[srcPos++] as Long?)?.let { FieldSet<NullSchema, FieldDef<NullSchema, *, *>>(it) }
    } else { // no fieldSet implies it's a non-partial Struct
        schema.allFieldSet
    } as FieldSet<NullSchema, FieldDef<NullSchema, *, *>>?

    var dstPos = _dstPos
    var lastMovedFieldIdx = -1
    var depth = 0
    var recipeOffset = _recipeOffset
    loop@ while (++recipeOffset < recipe.size) { // evaluate nesting commands, start-end pairs with some nesting
        val nesting = recipe[recipeOffset]
        if (nesting != null) {
            // gonna recurse and inflate nested stuff, but first let's move preceding field values up
            val myField = nesting.myField!!
            while (++lastMovedFieldIdx < myField.ordinal.toInt()) {
                val value = mutColumnValues[srcPos++]
                if (schema.fieldAt(lastMovedFieldIdx) in fieldSet)
                    mutColumnValues[dstPos++] = value
            }

            // now lastMovedFieldIdx == nesting.myField.ordinal, let's recurse
            if (myField in fieldSet)
                inflate(recipe, mutColumnValues, srcPos, dstPos++, recipeOffset)
            srcPos += nesting.colCount

            // and skip all nesting commands consumed by the recursive call or ignored due to empty values
            // argh, I really miss references to local variables now
            depth++
            while (depth > 0) {
                if (recipe[++recipeOffset] != null) depth++
                else depth--
            }
            // depth = 0 at this point, meaning that we're skipped nested structs
        } else {
            if (depth-- == 0) break@loop // if depth was 0, we've met enclosing (not ours) struct end
        }
    }

    // move all trailing values up — some copy-paste here
    val fields = schema.allFieldSet
    while (++lastMovedFieldIdx < fields.size) {
        val value = mutColumnValues[srcPos++]
        if (schema.fieldAt(lastMovedFieldIdx) in fieldSet)
            mutColumnValues[dstPos++] = value
    }

    // yay! commit & push
    val t = start.unwrappedType as DataType.NotNull.Partial<Any?, NullSchema>
    mutColumnValues[_dstPos] =
            if (fieldSet == null) null
            else t.load(fieldSet, when (fieldSet.size) {
                0 -> null
                1 -> mutColumnValues[_dstPos]
                else -> mutColumnValues.copyOfRange(_dstPos, _dstPos + fieldSet.size)
            })
}

/**
 * Scatters in-memory value to column values.
 */
internal fun flatten(
        recipe: Array<out Table.StructStart?>,
        out: Array<Any?>,
        value: Any?,
        _dstPos: Int,
        _recipeOffset: Int
) {
    val start = recipe[_recipeOffset]!!
    var dstPos = _dstPos
    val type = start.type ?: start.unwrappedType // OMG, such a hack:
    // unwrappedType is a correct type for non-embedded, top-level struct

    if (start.hasFieldSet && type is DataType.Nullable<*, *> && value === null)
        return // fieldSet is null, all fields are nulls, nothing to do here -------------------------------------------

    val erased = start.unwrappedType as DataType.NotNull.Partial<Any?, *>

    val fieldSet =
            if (start.hasFieldSet) (erased.fields(value) as FieldSet<NullSchema, FieldDef<NullSchema, *, *>>).also {
                out[dstPos++] = it.bitSet
            } else erased.schema.allFieldSet as FieldSet<NullSchema, FieldDef<NullSchema, *, *>>

    val schema = start.unwrappedType.schema
    when (fieldSet.size) {
        0 -> { /* nothing to do here */ }
        1 -> {
            val fieldValue = erased.store(value)
            flattenFieldValues(_recipeOffset, { fieldValue }, recipe, schema, fieldSet, out, dstPos)
        }
        else -> {
            val fieldValues = erased.store(value) as Array<Any?> // fixme allocation
            flattenFieldValues(_recipeOffset, { f ->
                fieldValues[fieldSet.indexOf(f as FieldDef<NullSchema, *, *>)]
            }, recipe, schema, fieldSet, out, dstPos)
        }
    }
}

private inline fun flattenFieldValues(
        _recipeOffset: Int, fieldValue: (FieldDef<*, *, *>) -> Any?, recipe: Array<out Table.StructStart?>,
        schema: Schema<*>, fieldSet: FieldSet<*, FieldDef<*, *, *>>,
        out: Array<Any?>, _dstPos: Int
) {
    var dstPos = _dstPos
    var lastSetFieldIdx = -1
    var depth = 0
    var recipeOffset = _recipeOffset
    loop@ while (++recipeOffset < recipe.size) { // evaluate nesting commands, start-end pairs with some nesting
        val nesting = recipe[recipeOffset]
        if (nesting != null) {
            // gonna recurse and flatten nested stuff, but first let's set all preceding field values up
            val myField = nesting.myField!!
            while (++lastSetFieldIdx < myField.ordinal.toInt()) {
                val field = schema.fieldAt(lastSetFieldIdx)
                if (field in fieldSet) out[dstPos] = fieldValue(field)
                dstPos++
            }

            // now lastSetFieldIdx == nesting.myField.ordinal, let's recurse
            if (myField in fieldSet)
                flatten(recipe, out, fieldValue(myField), dstPos, recipeOffset)
            dstPos += nesting.colCount

            // and skip all nesting commands consumed by the recursive call or ignored due to empty values
            // argh, I really miss references to local variables now
            depth++
            while (depth > 0) {
                if (recipe[++recipeOffset] != null) {
                    depth++
                } else {
                    depth--
                }
            }
            // depth = 0 at this point, meaning that we're skipped nested structs
        } else {
            if (depth-- == 0) break@loop // if depth was 0, we've met enclosing (not ours) struct end
        }
    }

    // assign trailing values
    while (++lastSetFieldIdx < schema.allFieldSet.size) {
        val field = schema.fieldAt(lastSetFieldIdx)
        if (field in fieldSet) out[dstPos] = fieldValue(field)
        dstPos++
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun FieldSet<*, *>?.contains(field: FieldDef<*, *, *>): Boolean =
        this != null && (this as FieldSet<NullSchema, *>).originalContains(field as FieldDef<NullSchema, *, *>)

internal fun <R, SCH : Schema<SCH>> forceIndexOfManaged(table: Table<SCH, *>, column: StoredNamedLens<SCH, R, out DataType<R>>): Int =
    table.indexOfManaged(column).let { idx ->
        if (idx >= 0) idx
        else throw NoSuchElementException(
            "${table.schema.run { column.name }} !in ${table.managedColNames.contentToString()}"
        )
    }

@PublishedApi @JvmField internal val throwNse = { throw NoSuchElementException() }

@PublishedApi internal open class JdbcType<T, DT : DataType<T>>(
    name: CharSequence,
    final override val type: DT,
) : PlatformType<ResultSet, PreparedStatement, T>(name), Ilk<T, DT> {
    override fun load(payload: ResultSet, index: Int): T =
        payload.getObject(index) as T
    override fun store(payload: PreparedStatement, index: Int, value: T): Unit =
        payload.setObject(index, value)

    final override val platformType: PlatformType<*, *, T>? get() = this
}

internal inline fun <K, V> MutableMap<K, V>?.compute(key: K, defaultValue: (K) -> V): V {
    val value = this?.get(key)
    return if (value == null) {
        val answer = defaultValue(key)
        this?.put(key, answer)
        answer
    } else {
        value
    }
}
