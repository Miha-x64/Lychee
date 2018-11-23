package net.aquadc.persistence.struct

import android.support.annotation.RestrictTo

/**
 * Represents a fully-immutable snapshot of a struct.
 */
class StructSnapshot<SCH : Schema<SCH>> : BaseStruct<SCH> {

    private val values: Array<Any?>

    constructor(source: Struct<SCH>) : super(source.schema) {
        val fields = schema.fields
        this.values = Array(fields.size) { i -> source[fields[i]] }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(type: SCH, values: List<Any?>) : super(type) {
        this.values = values.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<SCH, T>): T =
            values[field.ordinal.toInt()] as T

}
