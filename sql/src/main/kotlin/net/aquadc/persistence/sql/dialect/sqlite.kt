@file:JvmName("SqliteDialect")
package net.aquadc.persistence.sql.dialect.sqlite

import net.aquadc.collections.enumMapOf
import net.aquadc.persistence.sql.dialect.BaseDialect
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.type.DataType

/**
 * Implements SQLite [Dialect].
 */
// wannabe `@JvmField val` but this breaks compilation. Dafuq?
object SqliteDialect : BaseDialect(
    enumMapOf(
        DataType.NotNull.Simple.Kind.Bool, "INTEGER",
        DataType.NotNull.Simple.Kind.I32, "INTEGER",
        DataType.NotNull.Simple.Kind.I64, "INTEGER",
        DataType.NotNull.Simple.Kind.F32, "REAL",
        DataType.NotNull.Simple.Kind.F64, "REAL",
        DataType.NotNull.Simple.Kind.Str, "TEXT",
        DataType.NotNull.Simple.Kind.Blob, "BLOB"
    ),
    truncate = "DELETE FROM"
)
