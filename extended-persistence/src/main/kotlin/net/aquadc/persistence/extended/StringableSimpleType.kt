package net.aquadc.persistence.extended

import net.aquadc.persistence.type.DataType

abstract class StringableSimpleType<T> internal constructor(kind: Kind) : DataType.Simple<T>(kind) {
    final override val hasStringRepresentation: Boolean get() = true
}
