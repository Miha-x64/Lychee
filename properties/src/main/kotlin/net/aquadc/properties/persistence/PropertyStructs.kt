@file:JvmName("PropertyStructs")
package net.aquadc.properties.persistence

import net.aquadc.persistence.array
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.MutableField
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.persistence.struct.TransactionalStruct
import net.aquadc.persistence.struct.foldField
import net.aquadc.persistence.struct.isEmpty
import net.aquadc.persistence.struct.mapIndexed
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.mapValueList

/**
 * A struct which has observable properties.
 */
interface PropertyStruct<SCH : Schema<SCH>> : Struct<SCH> {

    /**
     * @return a property representing a given [field]
     */
    infix fun <T> prop(field: MutableField<SCH, T, *>): Property<T>

}

/**
 * A struct which has observable transactional properties.
 */
interface TransactionalPropertyStruct<SCH : Schema<SCH>> : PropertyStruct<SCH>, TransactionalStruct<SCH> {

    /**
     * @return a property representing a given [field]
     */
    override fun <T> prop(field: MutableField<SCH, T, *>): TransactionalProperty<StructTransaction<SCH>, T>

}


/**
 * Returns a [Property] containing snapshots of [this] mutable struct.
 */
fun <SCH : Schema<SCH>> PropertyStruct<SCH>.snapshots(): Property<Struct<SCH>> {
    return schema.mapIndexed(schema.mutableFieldSet) { _, it -> prop(it) }.asList().mapValueList { newMutableValues ->
        val array = if (schema.immutableFieldSet.isEmpty) newMutableValues.array(1)
        else {
            val fields = schema.fields
            arrayOfNulls<Any>(fields.size + 1).also { array ->
                for (i in fields.indices)
                    array[i] = (fields[i] as FieldDef<SCH, Any?, DataType<Any?>>).foldField(
                        ifMutable = { newMutableValues[it.mutableOrdinal.toInt()] },
                        ifImmutable = { this@snapshots[it] }
                    )
            }
        }
        array[array.lastIndex] = schema
        StructSnapshot<SCH>(array)
    }
}

/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [SCH].
 */
fun <SCH : Schema<SCH>, T> propertyGetterOf(field: MutableField<SCH, T, *>): (PropertyStruct<SCH>) -> Property<T> =
        { it prop field }
