package net.aquadc.properties.persistence

import net.aquadc.persistence.source.DataReader
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructDef
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.propertyOf

/**
 * Implements a mutable, observable, in-memory [Struct].
 * This is a reference implementation:
 * * every struct should have
 *   * a constructor copying from [Struct]
 *   * a constructor reading from [DataReader]
 * * every mutable observable [Struct] implementation should have
 *   * a mutator [set]
 *   * a property getter [prop]
 */
class ObservableStruct<DEF : StructDef<DEF>> : BaseStruct<DEF> {

    private val values: Array<Any?>

    constructor(source: Struct<DEF>, concurrent: Boolean) : super(source.type) {
        val fields = type.fields
        values = Array(fields.size) { i ->
            val field = fields[i]
            val value = source[field]
            when (field) {
                is FieldDef.Mutable<DEF, *> -> propertyOf(value, concurrent)
                is FieldDef.Immutable<DEF, *> -> value
            }
        }
    }

    constructor(reader: DataReader, type: DEF, concurrent: Boolean) : super(type) {
        val vals = reader.readKeyValuePairs(type)
        val fields = type.fields
        for (i in fields.indices) {
            when (fields[i]) {
                is FieldDef.Mutable<DEF, *> -> vals[i] = propertyOf(vals[i], concurrent)
                is FieldDef.Immutable<DEF, *> -> { /* we already have a value in vals[i] */ }
            }
        }
        values = vals
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<DEF, T>): T {
        val value = values[field.ordinal.toInt()]
        return when (field) {
            is FieldDef.Mutable<DEF, T> -> (value as MutableProperty<T>).value
            is FieldDef.Immutable<DEF, T> -> value as T
        }
    }

    operator fun <T> set(field: FieldDef.Mutable<DEF, T>, value: T) {
        prop(field).value = value
    }

    @Suppress("UNCHECKED_CAST")
    infix fun <T> prop(field: FieldDef.Mutable<DEF, T>) =
            (values[field.ordinal.toInt()] as MutableProperty<T>)

}
