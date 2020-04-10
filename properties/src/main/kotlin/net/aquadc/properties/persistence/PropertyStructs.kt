@file:JvmName("PropertyStructs")
package net.aquadc.properties.persistence

import net.aquadc.persistence.array
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.struct.StructTransaction
import net.aquadc.persistence.struct.TransactionalStruct
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
    infix fun <T> prop(field: FieldDef.Mutable<SCH, T, *>): Property<T>

}

/**
 * A struct which has observable transactional properties.
 */
interface TransactionalPropertyStruct<SCH : Schema<SCH>> : PropertyStruct<SCH>, TransactionalStruct<SCH> {

    /**
     * @return a property representing a given [field]
     */
    override fun <T> prop(field: FieldDef.Mutable<SCH, T, *>): TransactionalProperty<StructTransaction<SCH>, T>

}


/**
 * Returns a [Property] containing snapshots of [this] mutable struct.
 */
fun <SCH : Schema<SCH>> PropertyStruct<SCH>.snapshots(): Property<Struct<SCH>> {
    return schema.mutableFields.map { prop(it) }.mapValueList { newMutableValues ->
        val array = if (schema.immutableFields.isEmpty()) newMutableValues.array(1)
        else {
            val fields = schema.fields
            arrayOfNulls<Any>(fields.size + 1).also { array ->
                for (i in fields.indices)
                    array[i] = when (val field = fields[i]) {
                        is FieldDef.Mutable -> newMutableValues[field.mutableOrdinal.toInt()]
                        is FieldDef.Immutable -> this@snapshots[field]
                    }
            }
        }
        array[array.lastIndex] = schema
        StructSnapshot<SCH>(array)
    }
}

/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [SCH].
 */
fun <SCH : Schema<SCH>, T> propertyGetterOf(field: FieldDef.Mutable<SCH, T, *>): (PropertyStruct<SCH>) -> Property<T> =
        { it prop field }
