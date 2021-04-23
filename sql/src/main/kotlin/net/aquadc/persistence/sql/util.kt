@file:Suppress("UNCHECKED_CAST") // this file is for unchecked casts :)
package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.Blocking
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
import net.aquadc.persistence.type.CustomType
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.serialized
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
        if (tmp == null || tmp!!.size < cc) tmp = arrayOfNulls(cc)
        d.flattenTo(tmp!!, table, f as FieldDef<SCH, Any?, *>, data.getOrThrow(f))
        repeat(cc) {
            bind(d.typeAt(table, f, it) as Ilk<Any?, *>, idx++, tmp!![it])
        }
    }
    return idx
}

internal inline fun <T, reified R> Array<T>.mapIndexedToArray(transform: (Int, T) -> R): Array<R> {
    val array = arrayOfNulls<R>(size)
    for (i in indices) {
        array[i] = transform(i, this[i])
    }
    @Suppress("UNCHECKED_CAST") // now it's filled with items and not thus not nullable
    return array as Array<R>
}

/**
 * Transforms flat column values to in-memory instance.
 * Puts the resulting [StructSnapshot] into [mutColumnValues] at [_dstPos].
 */
@Suppress("UPPER_BOUND_VIOLATED")
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
        (mutColumnValues[srcPos++] as Long?)?.let { FieldSet<Schema<*>, FieldDef<Schema<*>, *, *>>(it) }
    } else { // no fieldSet implies it's a non-partial Struct
        schema.allFieldSet as FieldSet<Schema<*>, FieldDef<Schema<*>, *, *>>
    }

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
    val t = start.unwrappedType as DataType.NotNull.Partial<Any?, Any?>
    fieldSet as FieldSet<Any?, FieldDef<Any?, *, *>>?
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
@Suppress("UPPER_BOUND_VIOLATED")
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
            if (start.hasFieldSet) (erased.fields(value) as FieldSet<Schema<*>, FieldDef<Schema<*>, *, *>>).also {
                out[dstPos++] = it.bitSet
            } else erased.schema.allFieldSet as FieldSet<Schema<*>, FieldDef<Schema<*>, *, *>>

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
                fieldValues[fieldSet.indexOf<Schema<*>>(f as FieldDef<Schema<*>, *, *>)]
            }, recipe, schema, fieldSet, out, dstPos)
        }
    }
}

@Suppress("UPPER_BOUND_VIOLATED")
private inline fun flattenFieldValues(
        _recipeOffset: Int, fieldValue: (FieldDef<out Schema<*>, *, *>) -> Any?, recipe: Array<out Table.StructStart?>,
        schema: Schema<*>, fieldSet: FieldSet<Schema<*>, FieldDef<Schema<*>, *, *>>,
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

@Suppress("UPPER_BOUND_VIOLATED", "NOTHING_TO_INLINE")
private inline operator fun FieldSet<Schema<*>, *>?.contains(field: FieldDef<*, *, *>): Boolean =
        this != null && this.originalContains<Schema<*>>(field as FieldDef<Schema<*>, *, *>)

internal fun <CUR> Blocking<CUR>.row(
    cursor: CUR, offset: Int, columnNames: Array<out CharSequence>, columnTypes: Array<out Ilk<*, *>>, bindBy: BindBy
): Array<Any?> = when (bindBy) {
    BindBy.Name -> rowByName(cursor, columnNames, columnTypes)
    BindBy.Position -> rowByPosition(cursor, offset, columnTypes)
}

internal fun <SCH : Schema<SCH>, CUR, R> Blocking<CUR>.cell(
    cursor: CUR, table: Table<SCH, *>, column: StoredNamedLens<SCH, R, out DataType<R>>, bindBy: BindBy
): R {
    val type = column.type(table.schema) as Ilk<R, *>
    //   every DataType case implements Ilk ^^^^^^^^^
    return when (bindBy) {
        BindBy.Name -> cellByName(cursor, column.name(table.schema), type)
        BindBy.Position -> cellAt(cursor, forceIndexOfManaged(table, column), type)
    }
}

private fun <R, SCH : Schema<SCH>> forceIndexOfManaged(table: Table<SCH, *>, column: StoredNamedLens<SCH, R, out DataType<R>>): Int =
    table.indexOfManaged(column).let { idx ->
        if (idx >= 0) idx
        else throw NoSuchElementException(
            "${table.schema.run { column.name }} !in ${table.managedColNames.contentToString()}"
        )
    }

internal fun <CUR, SCH : Schema<SCH>> Blocking<CUR>.mapRow(
        bindBy: BindBy,
        cur: CUR,
        colNames: Array<out CharSequence>,
        colTypes: Array<out Ilk<*, *>>,
        recipe: Array<out Table.StructStart?>
): StructSnapshot<SCH> {
    val firstValues = row(cur, 0, colNames, colTypes, bindBy)
    inflate(recipe, firstValues, 0, 0, 0)
    @Suppress("UNCHECKED_CAST")
    return firstValues[0] as StructSnapshot<SCH>
}

@PublishedApi @JvmField internal val throwNse = { throw NoSuchElementException() }

@PublishedApi internal open class NativeType<T, DT : DataType<T>>(
    name: CharSequence,
    final override val type: DT
) : CustomType<T>(name), Ilk<T, DT> {
    override fun store(payload: Any?, value: T): Any? = value
    @Suppress("UNCHECKED_CAST")
    override fun load(payload: Any?, value: Any?): T = value as T

    final override val custom: CustomType<T>? get() = this
}
