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
        DataType.Simple.Kind.Bool, "bool",
        DataType.Simple.Kind.I32, "int",
        DataType.Simple.Kind.I64, "int8",
        DataType.Simple.Kind.F32, "real",
        DataType.Simple.Kind.F64, "float8",
        DataType.Simple.Kind.Str, "text",
        DataType.Simple.Kind.Blob, "bytea"
    ),
    truncate = "TRUNCATE TABLE"
) {
    override fun StringBuilder.appendPkType(type: DataType.Simple<*>, managed: Boolean): StringBuilder =
        if (managed) appendNameOf(type)
        else { // If PK column is 'managed', we just take `structToInsert[pkField]`.
            // Otherwise, its our responsibility to make PK auto-generated
            if (type.kind == DataType.Simple.Kind.I32) append("serial")
            else if (type.kind == DataType.Simple.Kind.I64) append("serial8")
            else throw UnsupportedOperationException() // wat? Boolean, float, double, string, byte[] primary key? O_o
        }
}
