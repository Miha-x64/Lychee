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
        DataType.Simple.Kind.Bool, "INTEGER",
        DataType.Simple.Kind.I32, "INTEGER",
        DataType.Simple.Kind.I64, "INTEGER",
        DataType.Simple.Kind.F32, "REAL",
        DataType.Simple.Kind.F64, "REAL",
        DataType.Simple.Kind.Str, "TEXT",
        DataType.Simple.Kind.Blob, "BLOB"
    ),
    truncate = "DELETE FROM"
)
