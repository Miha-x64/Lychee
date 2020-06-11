package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.SqliteSession
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.string
import java.sql.PreparedStatement
import java.sql.ResultSet

@Deprecated("renamed", level = DeprecationLevel.ERROR)
@Suppress("UNUSED_TYPEALIAS_PARAMETER")
typealias Relation<S, ID, T> = ColMeta<S>

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
        @Deprecated("renamed", ReplaceWith("embed(naming, path, fieldSetColName)", "net.aquadc.persistence.sql.ColMeta.Companion.*"), DeprecationLevel.ERROR)
        @JvmName("embeddedNullable")
        inline fun <S : Schema<S>, ET : Any, EDT : DataType.Nullable<ET, out DataType.NotNull.Partial<ET, *>>>
            Embedded(naming: NamingConvention, path: StoredLens<S, ET?, EDT>, fieldSetColName: CharSequence): Nothing =
            throw AssertionError()
        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.NotNull.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @Deprecated("renamed", ReplaceWith("embed(naming, path, fieldSetColName)", "net.aquadc.persistence.sql.ColMeta.Companion.*"), DeprecationLevel.ERROR)
        @JvmName("embeddedPartial")
        inline fun <S : Schema<S>, ET, EDT : DataType.NotNull.Partial<ET, *>>
            Embedded(naming: NamingConvention, path: StoredLens<S, ET, EDT>, fieldSetColName: CharSequence): Nothing =
            throw AssertionError()
        /**
         * @param naming which will concat names
         * @param path to a value stored as a struct with [Schema]
         */
        @Deprecated("renamed", ReplaceWith("embed(naming, path)", "net.aquadc.persistence.sql.ColMeta.Companion.*"), DeprecationLevel.ERROR)
        @JvmName("embeddedStruct")
        inline fun <S : Schema<S>, ES : Schema<ES>>
            Embedded(naming: NamingConvention, path: StoredLens<S, Struct<ES>, ES>): Nothing =
            throw AssertionError()


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
         * * bypass [DataType], and bind [store]d parameters directly, e.g. with [PreparedStatement.setObject],
         * * and read [load]ed parameters directly, e.g. using [ResultSet.getObject].
         * [SqliteSession] ignores type overrides and takes into account only [typeName].
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

        /**
         * Reference a single entity by its primary key.
         * @param S outer schema
         * @param FS foreign schema
         */
        @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR) // todo
        class ToOne<S : Schema<S>, FS : Schema<FS>, FID : IdBound>(
            path: StoredLens<S, Record<FS, FID>?, *>, foreignTable: Table<FS, *>
        ) : Rel<S>(path) {
            init {
                checkToOne(TODO(), path, foreignTable)
            }
        }

        /**
         * There are some entities which reference this one by our primary key.
         * @param S outer schema
         * @param FS foreign schema
         */
        @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR)
        class ToMany<S : Schema<S>, ID : IdBound, FS : Schema<FS>, FID : IdBound, C : Collection<Record<FS, FID>>> private constructor(
            ourTable: Table<S, ID>, path: StoredLens<S, C, *>, foreignTable: Table<FS, *>, joinColumn: StoredLens<FS, *, *>
        ) : Rel<S>(path) {
            init {
                checkToMany(ourTable.schema, path, foreignTable)
                checkToOne(foreignTable.schema, joinColumn, ourTable) // ToMany is actually many ToOnes
            }

            companion object {
                operator fun <S : Schema<S>, ID : IdBound, FS : Schema<FS>, FID : IdBound, C : Collection<Record<FS, FID>>> Table<S, ID>.invoke(
                    path: Lens<S, Record<S, ID>, Record<S, ID>, C, *>, foreignTable: Table<FS, *>, joinColumn: Lens<FS, Record<FS, *>, Record<FS, *>, *, *>
                ): Nothing = TODO() // ToMany<S, ID, FS, R, FID, FR, C> = ToMany(this, path, foreignTable, joinColumn)
            }
        }

        @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR)
        class ManyToMany<S : Schema<S>, FS : Schema<FS>, C : Collection<Record<FS, *>>>(
            path: StoredLens<S, C, *>, foreignTable: Table<FS, *>, joinTable: JoinTable
        ) : Rel<S>(path) {
            init {
                checkToMany(TODO(), path, foreignTable)
            }
        }

    }

    // for tests

    override fun hashCode(): Int =
        javaClass.hashCode() xor path.hashCode()
    override fun equals(other: Any?): Boolean =
            other is ColMeta<*> && javaClass === other.javaClass && path == other.path

}

/**
 * A special case of a table which has no PK and consists of two columns with a composite unique index.
 */
typealias JoinTable = Nothing
/*class JoinTable(
        val aName: String,
        val aType: DataType.Simple<*>,
        val bName: String,
        val bType: DataType.Simple<*>
) {

    /**
     * When table A references a list of Bs using a `joinTable`,
     * table B should reference a list of As using `joinTable.flip()`
     */
    fun filp(): JoinTable =
            JoinTable(bName, bType, aName, aType)

}*/

internal fun <S : Schema<S>, FS : Schema<FS>> checkToMany(
        schema: S, path: StoredLens<S, *, *>, foreignTable: Table<FS, *>) {
    val type = path.type(schema)
    check(type is DataType.NotNull.Collect<*, *, *>) {
        "only fields of Collection<Struct> types can be used with to-many relations"
    }
    val elType = type.elementType
    check(elType is Schema<*>) {
        "only fields of Collection<Struct> types can be used with to-many relations"
    }
    check(elType.schema.javaClass == foreignTable.schema.javaClass) {
        "type of this field and Schema of referenced table must be compatible"
    }
}

internal fun <S : Schema<S>, F : Schema<F>> checkToOne(
        schema: S, path: StoredLens<S, *, *>, foreignTable: Table<F, *>
) {
    val type = path.type(schema)
    val realType = if (type is DataType.Nullable<*, *>) type.actualType else type
    check(realType is Schema<*>) {
        "only fields of Struct types can be used with such relations, got $realType at $path"
    }
    check(realType.schema.javaClass == foreignTable.schema.javaClass) {
        "type of this field and Schema of referenced table must be compatible"
    }
}
