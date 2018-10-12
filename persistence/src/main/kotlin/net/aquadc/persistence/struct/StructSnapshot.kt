package net.aquadc.persistence.struct

import net.aquadc.persistence.source.DataReader

/**
 * Represents a fully-immutable snapshot of a struct.
 */
class StructSnapshot<DEF : StructDef<DEF>> : BaseStruct<DEF> {

    private val values: Array<Any?>

    constructor(source: Struct<DEF>) : super(source.type) {
        val fields = type.fields
        values = Array(fields.size) { i -> source[fields[i]] }
    }

    constructor(reader: DataReader, type: DEF, concurrent: Boolean) : super(type) {
        values = reader.readKeyValuePairs(type)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<DEF, T>): T =
            values[field.ordinal.toInt()] as T

}
