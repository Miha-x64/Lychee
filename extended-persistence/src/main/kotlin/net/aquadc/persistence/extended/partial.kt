@file:JvmName("Partials")
package net.aquadc.persistence.extended

import android.support.annotation.RestrictTo
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.FieldSet
import net.aquadc.persistence.struct.PartialStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructBuilder
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.allFieldSet
import net.aquadc.persistence.struct.forEach
import net.aquadc.persistence.struct.forEachIndexed
import net.aquadc.persistence.struct.indexOf
import net.aquadc.persistence.struct.newBuilder
import net.aquadc.persistence.struct.size
import net.aquadc.persistence.type.DataType


fun <SCH : Schema<SCH>> partial(schema: SCH): DataType.Partial<PartialStruct<SCH>, SCH> =
        object : DataType.Partial<PartialStruct<SCH>, SCH>() {

            override val schema: SCH
                get() = schema

            override fun load(fields: FieldSet<SCH, FieldDef<SCH, *>>, values: Array<Any?>?): PartialStruct<SCH> =
                    schema.buildPartial { b ->
                        schema.forEach(fields) { field ->
                            b[field as FieldDef<SCH, Any?>] = values!![field.ordinal.toInt()]
                        }
                    }

            override fun store(value: PartialStruct<SCH>): PartialStruct<SCH> =
                    value

        }

/**
 * Represents a fully immutable snapshot of a partial struct.
 */
class PartialStructSnapshot<SCH : Schema<SCH>> : BaseStruct<SCH> {

    override val fields: FieldSet<SCH, FieldDef<SCH, *>>
    private val values: Array<Any?>

    constructor(source: Struct<SCH>, fields: FieldSet<SCH, *>) : super(source.schema) {
        this.fields = fields
        this.values = arrayOfNulls(fields.size.toInt())
        schema.forEachIndexed<SCH, FieldDef<SCH, *>>(fields) { idx, field ->
            values[idx] = source[field]
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(schema: SCH, fields: FieldSet<SCH, *>, values: Array<Any?>) : super(schema) {
        this.fields = fields
        this.values = arrayOfNulls(fields.size.toInt())
        schema.forEachIndexed<SCH, FieldDef<SCH, *>>(fields) { idx, field ->
            this.values[idx] = values[field.ordinal.toInt()]
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<SCH, T>): T =
            try {
                values[fields.indexOf(field).toInt()] as T
            } catch (e: ArrayIndexOutOfBoundsException) {
                throw NoSuchElementException("There's no value for $field in $this")
            }

}

/**
 * Builds a [PartialStruct].
 */
inline fun <SCH : Schema<SCH>> SCH.buildPartial(build: SCH.(StructBuilder<SCH>) -> Unit): PartialStruct<SCH> {
    val builder = newBuilder(this)
    build(this, builder)
    val values = builder.expose()
    return if (builder.fieldsPresent() == allFieldSet()) StructSnapshot(schema, values) else PartialStructSnapshot(schema, builder.fieldsPresent(), values)
}

fun <SCH : Schema<SCH>> Struct<SCH>.take(fields: FieldSet<SCH, FieldDef<SCH, *>>): PartialStruct<SCH> =
        if (fields == schema.allFieldSet()) StructSnapshot(this)
        else PartialStructSnapshot(this, fields)
