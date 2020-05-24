@file:JvmName("PostgresDialect")
package net.aquadc.persistence.sql.dialect.postgres

import net.aquadc.collections.enumMapOf
import net.aquadc.persistence.sql.dialect.BaseDialect
import net.aquadc.persistence.sql.dialect.Dialect
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
    truncate = "TRUNCATE TABLE"
) {
    override fun StringBuilder.appendPkType(type: DataType.NotNull.Simple<*>, managed: Boolean): StringBuilder =
        if (managed) appendNameOf(type)
        else { // If PK column is 'managed', we just take `structToInsert[pkField]`.
            // Otherwise, its our responsibility to make PK auto-generated
            if (type.kind == DataType.NotNull.Simple.Kind.I32) append("serial NOT NULL")
            else if (type.kind == DataType.NotNull.Simple.Kind.I64) append("serial8 NOT NULL")
            else throw UnsupportedOperationException() // wat? Boolean, float, double, string, byte[] primary key? O_o
        }
}
