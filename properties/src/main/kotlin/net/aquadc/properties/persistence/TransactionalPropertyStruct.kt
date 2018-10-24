package net.aquadc.properties.persistence

import net.aquadc.persistence.struct.*
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.mapValueList


/**
 * A struct which has observable properties.
 */
interface PropertyStruct<DEF : StructDef<DEF>> : Struct<DEF> {

    /**
     * @return a property representing a given [field]
     */
    infix fun <T> prop(field: FieldDef.Mutable<DEF, T>): Property<T>

}

/**
 * A struct which has observable transactional properties.
 */
interface TransactionalPropertyStruct<DEF : StructDef<DEF>> : PropertyStruct<DEF>, TransactionalStruct<DEF> {

    /**
     * @return a property representing a given [field]
     */
    override fun <T> prop(field: FieldDef.Mutable<DEF, T>): TransactionalProperty<StructTransaction<DEF>, T>

}


fun <DEF : StructDef<DEF>> PropertyStruct<DEF>.snapshots(): Property<Struct<DEF>> =
        type.mutableFields.map { prop(it) }.mapValueList { StructSnapshot(type, it) }

/**
 * 'Template method' implementation for the case where ManagedProperty's Manager performs writes.
 */
abstract class PropStructTransaction<DEF : StructDef<DEF>>(
        private val struct: TransactionalPropertyStruct<DEF>
) : StructTransaction<DEF> {

    protected var successful: Boolean? = false

    final override fun <T> set(field: FieldDef.Mutable<DEF, T>, update: T) {
        (struct prop field).setValue(this, update)
    }

    final override fun setSuccessful() {
        successful = true
    }

}
