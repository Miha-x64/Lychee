@file:JvmName("Partials")
@file:OptIn(ExperimentalContracts::class)
package net.aquadc.persistence.extended

import androidx.annotation.RestrictTo
import net.aquadc.persistence.fieldValues
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.buildUpon
import net.aquadc.persistence.struct.contains
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.indexOf
import net.aquadc.persistence.struct.newBuilder
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.DataType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


@JvmField @JvmSynthetic internal val EmptyArray = emptyArray<Any?>()
fun <SCH : Schema<SCH>> partial(schema: SCH): DataType.NotNull.Partial<PartialStruct<SCH>, SCH> =
        object : DataType.NotNull.Partial<PartialStruct<SCH>, SCH>(schema) {

            @Suppress("INAPPLICABLE_JVM_NAME") @JvmName("load") // avoid having both `load-<hash>()` and `bridge load()`
            override fun load(fields: FieldSet<SCH, FieldDef<SCH, *, *>>, values: Any?): PartialStruct<SCH> =
                    PartialStructSnapshot(schema, fields, when (fields.size) {
                        0 -> EmptyArray
                        1 -> arrayOf(values)
                        else -> values as Array<Any?>
                    }, null)

            override fun fields(value: PartialStruct<SCH>): FieldSet<SCH, FieldDef<SCH, *, *>> =
                    value.fields

            override fun store(value: PartialStruct<SCH>): Any? =
                    value.fieldValues()

        }

/**
 * Represents a fully immutable snapshot of a partial struct.
 */
class PartialStructSnapshot<SCH : Schema<SCH>> : BaseStruct<SCH> {

    override val fields: FieldSet<SCH, FieldDef<SCH, *, *>>
    private val values: Array<Any?>

    constructor(source: Struct<SCH>, fields: FieldSet<SCH, *>) : super(source.schema) {
        this.fields = fields
        this.values = arrayOfNulls(fields.size)
        schema.forEachIndexed<SCH, FieldDef<SCH, *, *>>(fields) { idx, field ->
            values[idx] = source[field]
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(sparseValuesAndSchema: Array<Any?>, fields: FieldSet<SCH, *>)
            : super(sparseValuesAndSchema[sparseValuesAndSchema.lastIndex] as SCH) {
        this.fields = fields
        this.values = arrayOfNulls<Any>(fields.size).also { packed ->
            schema.forEachIndexed<SCH, FieldDef<SCH, *, *>>(fields) { idx, field ->
                packed[idx] = sparseValuesAndSchema[field.ordinal.toInt()]
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(schema: SCH, fields: FieldSet<SCH, *>, packedValues: Array<Any?>, dummy: Nothing?) : super(schema) {
        check(fields.size == packedValues.size)
        this.fields = fields
        this.values = packedValues
    }

    override fun <T> getOrThrow(field: FieldDef<SCH, T, *>): T =
            try {
                values[fields.indexOf(field).toInt()] as T
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw NoSuchElementException("There's no value for $field in $this")
            }

}

/**
 * Returns value of the [field], or `null`, if absent.
 */
fun <SCH : Schema<SCH>, T> PartialStruct<SCH>.getOrNull(field: FieldDef<SCH, T, *>): T? =
        if (field in fields) getOrThrow(field)
        else null

/**
 * Returns value of the [field], or [defaultValue], if absent.
 */
fun <SCH : Schema<SCH>, T> PartialStruct<SCH>.getOrDefault(field: FieldDef<SCH, T, *>, defaultValue: T): T =
        if (field in fields) getOrThrow(field)
        else defaultValue

/**
 * Returns value of the [field], or evaluates and returns [defaultValue], if absent.
 */
inline fun <SCH : Schema<SCH>, T> PartialStruct<SCH>.getOrElse(field: FieldDef<SCH, T, *>, defaultValue: () -> T): T {
    contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }

    return if (field in fields) getOrThrow(field)
    else defaultValue()
}

/**
 * Builds a [PartialStruct].
 */
inline fun <SCH : Schema<SCH>> SCH.buildPartial(build: SCH.(StructBuilder<SCH>) -> Unit): PartialStruct<SCH> {
    contract { callsInPlace(build, InvocationKind.EXACTLY_ONCE) }

    val builder = newBuilder(this)
    build(this, builder)
    return finish(builder)
}

/**
 * Creates a [PartialStruct] consisting of [fields] from [this].
 */
fun <SCH : Schema<SCH>> Struct<SCH>.take(fields: FieldSet<SCH, FieldDef<SCH, *, *>>): PartialStruct<SCH> =
        if (fields == schema.allFieldSet) {
            // 'is' smartcasts and uselessly reboxes value https://youtrack.jetbrains.com/issue/KT-38190
            if (this.javaClass === StructSnapshot::class.java) this
            else StructSnapshot(this)
        } else {
            PartialStructSnapshot(this, fields)
        }

/**
 * Builds a [StructSnapshot] filled with data from [this] and applies changes via [mutate].
 */
inline fun <SCH : Schema<SCH>> PartialStruct<SCH>.copy(
    fields: FieldSet<SCH, FieldDef<SCH, *, *>> = schema.allFieldSet,
    mutate: SCH.(StructBuilder<SCH>) -> Unit = { }
): PartialStruct<SCH> {
    contract { callsInPlace(mutate, InvocationKind.EXACTLY_ONCE) }

    val builder = buildUpon(this, fields)
    mutate(schema, builder)
    return schema.finish(builder)
}

@PublishedApi internal fun <SCH : Schema<SCH>> SCH.finish(builder: StructBuilder<SCH>): PartialStruct<SCH> =
        builder.expose().let { valuesAndSchema ->
            if (builder.fieldsPresent() == allFieldSet) StructSnapshot(valuesAndSchema)
            else PartialStructSnapshot(valuesAndSchema, builder.fieldsPresent())
        }
