package net.aquadc.persistence.struct

import androidx.annotation.RestrictTo
import net.aquadc.persistence.values

/**
 * Represents a fully immutable snapshot of a struct.
 */
class StructSnapshot<SCH : Schema<SCH>> : BaseStruct<SCH>, Struct<SCH> {

    // may become an inline-class when hashCode/equals will be allowed

    private val values: Array<Any?>

    constructor(source: Struct<SCH>) : super(source.schema) {
        this.values = source.values()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(schema: SCH, values: Array<Any?>) : super(schema) {
        this.values = values
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(field: FieldDef<SCH, T, *>): T =
            values[field.ordinal.toInt()] as T

}
