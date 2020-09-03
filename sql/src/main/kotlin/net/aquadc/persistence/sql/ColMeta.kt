package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.SqliteSession
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.string
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * A piece of metadata about table columns: relations, indices, type overrides.
 */
sealed class ColMeta<S : Schema<S>>(
    @JvmField val path: StoredLens<S, *, *>
) {

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.NotNull.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @JvmName("embedNullable")
        inline fun <S : Schema<S>, ET : Any, EDT : DataType.Nullable<ET, out DataType.NotNull.Partial<ET, *>>>
            embed(naming: NamingConvention, path: StoredLens<S, ET?, EDT>, fieldSetColName: CharSequence): ColMeta<S> =
            Rel.Embed<S>(naming, path, fieldSetColName)

        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.NotNull.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @JvmName("embedPartial")
        inline fun <S : Schema<S>, ET, EDT : DataType.NotNull.Partial<ET, *>>
            embed(naming: NamingConvention, path: StoredLens<S, ET, EDT>, fieldSetColName: CharSequence): ColMeta<S> =
            Rel.Embed<S>(naming, path, fieldSetColName)

        /**
         * @param naming which will concat names
         * @param path to a value stored as a struct with [Schema]
         */
        @JvmName("embedStruct")
        inline fun <S : Schema<S>, ES : Schema<ES>>
            embed(naming: NamingConvention, path: StoredLens<S, Struct<ES>, ES>): ColMeta<S> =
            Rel.Embed<S>(naming, path, null)

        /**
         * Override [DataType] in "CREATE TABLE" statement.
         * For example, [string] will be represented as "text" by default,
         * but you can write `type(SomeStringField, "varchar(256)")` in order to change it.
         */
        inline fun <S : Schema<S>, T> type(path: StoredLens<S, T, out DataType<T>>, typeName: CharSequence): ColMeta<S> =
            Type(path, typeName, null)

        /**
         * Override [DataType] behaviour. This will
         * * alter "CREATE TABLE" statement same way as [type] does,
         * * bypass [DataType], and bind parameters directly, e.g. with [PreparedStatement.setObject],
         * * and read parameters directly, e.g. using [ResultSet.getObject].
         */
        inline fun <S : Schema<S>, T> S.nativeType(
            path: StoredLens<S, T, out DataType<T>>, custom: Ilk<T, *>
        ): ColMeta<S> =
            Type(path, null, custom)

        /**
         * Override [DataType] behaviour. This will
         * * alter "CREATE TABLE" statement same way as [type] does,
         * * bypass [DataType], and bind parameters directly, e.g. with [PreparedStatement.setObject],
         * * and read parameters directly, e.g. using [ResultSet.getObject].
         * [SqliteSession] ignores type overrides and takes into account only [typeName].
         */
        @Suppress("UNCHECKED_CAST")
        inline fun <S : Schema<S>, T> S.nativeType(
            path: StoredLens<S, T, out DataType<T>>, typeName: CharSequence
        ): ColMeta<S> =
            Type(path, null, NativeType(typeName, path.type(this)))
    }

    @PublishedApi internal class Type<S : Schema<S>, T> constructor(
        path: StoredLens<S, T, *>,
        @JvmField val typeName: CharSequence?,
        @JvmField val override: Ilk<T, *>?
    ) : ColMeta<S>(path)

    @PublishedApi internal sealed class Rel<S : Schema<S>>(path: StoredLens<S, *, *>) : ColMeta<S>(path) {

        /**
         * Embed a (Partial)[Struct] into current table.
         * @param S outer [Schema]
         */
        @PublishedApi internal class Embed<S : Schema<S>> constructor(
            val naming: NamingConvention,
            path: StoredLens<S, *, *>,
            val fieldSetColName: CharSequence?
        ) : Rel<S>(path)

    }

    // for tests

    override fun hashCode(): Int =
        javaClass.hashCode() xor path.hashCode()
    override fun equals(other: Any?): Boolean =
            other is ColMeta<*> && javaClass === other.javaClass && path == other.path

}
