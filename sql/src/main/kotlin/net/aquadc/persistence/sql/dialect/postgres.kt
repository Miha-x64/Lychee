@file:JvmName("PostgresDialect")
package net.aquadc.persistence.sql.dialect.postgres

import net.aquadc.collections.contains
import net.aquadc.collections.enumMapOf
import net.aquadc.collections.plus
import net.aquadc.persistence.sql.dialect.BaseDialect
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.appendIf
import net.aquadc.persistence.type.DataType

/**
 * Implements PostgreSQL [Dialect].
 */
@JvmField
val PostgresDialect: Dialect = object : BaseDialect(
    enumMapOf(
        DataType.NotNull.Simple.Kind.Bool, "bool",
        DataType.NotNull.Simple.Kind.I32, "int",
        DataType.NotNull.Simple.Kind.I64, "int8",
        DataType.NotNull.Simple.Kind.F32, "real",
        DataType.NotNull.Simple.Kind.F64, "float8",
        DataType.NotNull.Simple.Kind.Str, "text",
        DataType.NotNull.Simple.Kind.Blob, "bytea"
    ),
    truncate = "TRUNCATE TABLE",
    arrayPostfix = "[]"
) {
    private val serial = DataType.NotNull.Simple.Kind.I32 + DataType.NotNull.Simple.Kind.I64
    override fun StringBuilder.appendPkType(type: DataType.NotNull.Simple<*>, managed: Boolean): StringBuilder =
        // If PK column is 'managed', we just take `structToInsert[pkField]`. todo unique constraint
        if (managed || type.kind !in serial) appendTwN(type)
        // Otherwise its our responsibility to make PK auto-generated
        else append("serial")
            .appendIf(type.kind == DataType.NotNull.Simple.Kind.I64, '8')
            .append(' ')
            .append("NOT NULL")
}
