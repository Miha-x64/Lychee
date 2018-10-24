package net.aquadc.persistence.struct

import android.support.annotation.RestrictTo
import net.aquadc.persistence.source.DataReader

/**
 * Represents a fully-immutable snapshot of a struct.
 */
class StructSnapshot<DEF : StructDef<DEF>> : BaseStruct<DEF> {

    private val values: Array<Any?>

    constructor(source: Struct<DEF>) : super(source.type) {
        val fields = type.fields
        this.values = Array(fields.size) { i -> source[fields[i]] }
    }

    constructor(reader: DataReader, type: DEF) : super(type) {
        this.values = reader.readKeyValuePairs(type)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(type: DEF, values: List<Any?>) : super(type) {
        this.values = values.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<DEF, T>): T =
            values[field.ordinal.toInt()] as T

}
