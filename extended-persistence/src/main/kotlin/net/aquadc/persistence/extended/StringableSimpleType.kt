package net.aquadc.persistence.extended

import net.aquadc.persistence.type.DataType

internal abstract class StringableSimpleType<T>(kind: Kind) : DataType.NotNull.Simple<T>(kind) {
    final override val hasStringRepresentation: Boolean get() = true
}
