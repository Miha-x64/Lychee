@file:JvmName("PropertyStructs")
package net.aquadc.properties.persistence

import net.aquadc.persistence.struct.*
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
    infix fun <T> prop(field: FieldDef.Mutable<SCH, T>): Property<T>

}

/**
 * A struct which has observable transactional properties.
 */
interface TransactionalPropertyStruct<SCH : Schema<SCH>> : PropertyStruct<SCH>, TransactionalStruct<SCH> {

    /**
     * @return a property representing a given [field]
     */
    override fun <T> prop(field: FieldDef.Mutable<SCH, T>): TransactionalProperty<StructTransaction<SCH>, T>

}


/**
 * Returns a [Property] containing snapshots of [this] mutable struct.
 */
fun <SCH : Schema<SCH>> PropertyStruct<SCH>.snapshots(): Property<Struct<SCH>> =
        schema.mutableFields.map { prop(it) }.mapValueList { StructSnapshot(schema, it) }

/**
 * 'Template method' implementation for the case where ManagedProperty's Manager performs writes.
 */
abstract class PropStructTransaction<SCH : Schema<SCH>>(
        private val struct: TransactionalPropertyStruct<SCH>
) : SimpleStructTransaction<SCH>() {

    final override fun <T> set(field: FieldDef.Mutable<SCH, T>, update: T) {
        (struct prop field).setValue(this, update)
    }

}
