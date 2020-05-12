package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType

@Deprecated("renamed")
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
         * @param path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @Deprecated("renamed", ReplaceWith("embed(naming, path, fieldSetColName)", "net.aquadc.persistence.sql.ColMeta.Companion.*"))
        @JvmName("embeddedNullable")
        inline fun <S : Schema<S>, ET : Any, EDT : DataType.Nullable<ET, out DataType.Partial<ET, *>>>
                Embedded(naming: NamingConvention, path: StoredLens<S, ET?, EDT>, fieldSetColName: String): ColMeta<S> =
                Embed<S>(naming, path, fieldSetColName)
        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @Deprecated("renamed", ReplaceWith("embed(naming, path, fieldSetColName)", "net.aquadc.persistence.sql.ColMeta.Companion.*"))
        @JvmName("embeddedPartial")
        inline fun <S : Schema<S>, ET, EDT : DataType.Partial<ET, *>>
                Embedded(naming: NamingConvention, path: StoredLens<S, ET, EDT>, fieldSetColName: String): ColMeta<S> =
                Embed<S>(naming, path, fieldSetColName)
        /**
         * @param naming which will concat names
         * @param path to a value stored as a struct with [Schema]
         */
        @Deprecated("renamed", ReplaceWith("embed(naming, path)", "net.aquadc.persistence.sql.ColMeta.Companion.*"))
        @JvmName("embeddedStruct")
        inline fun <S : Schema<S>, ES : Schema<ES>>
                Embedded(naming: NamingConvention, path: StoredLens<S, Struct<ES>, ES>): ColMeta<S> =
                Embed<S>(naming, path, null)


        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @JvmName("embedNullable")
        inline fun <S : Schema<S>, ET : Any, EDT : DataType.Nullable<ET, out DataType.Partial<ET, *>>>
            embed(naming: NamingConvention, path: StoredLens<S, ET?, EDT>, fieldSetColName: String): ColMeta<S> =
            Embed<S>(naming, path, fieldSetColName)

        /**
         * @param naming which will concat names
         * @param path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        @JvmName("embedPartial")
        inline fun <S : Schema<S>, ET, EDT : DataType.Partial<ET, *>>
            embed(naming: NamingConvention, path: StoredLens<S, ET, EDT>, fieldSetColName: String): ColMeta<S> =
            Embed<S>(naming, path, fieldSetColName)

        /**
         * @param naming which will concat names
         * @param path to a value stored as a struct with [Schema]
         */
        @JvmName("embedStruct")
        inline fun <S : Schema<S>, ES : Schema<ES>>
            embed(naming: NamingConvention, path: StoredLens<S, Struct<ES>, ES>): ColMeta<S> =
            Embed<S>(naming, path, null)
    }

    /**
     * Embed a (Partial)[Struct] of type [ET] into current table.
     * @param S outer [Schema]
     */
    @PublishedApi internal class Embed<S : Schema<S>> constructor(
        val naming: NamingConvention,
        path: StoredLens<S, *, *>,
        val fieldSetColName: String?
    ) : ColMeta<S>(path)

    /**
     * Reference a single entity by its primary key.
     * @param S outer schema
     * @param FS foreign schema
     */
    @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR) // todo
    class ToOne<S : Schema<S>, ID : IdBound, FS : Schema<FS>, FID : IdBound>(
            path: StoredLens<S, Record<FS, FID>?, *>, foreignTable: Table<FS, *>
    ) : ColMeta<S>(path) {
        init {
            checkToOne(TODO(), path, foreignTable)
        }
    }

    /**
     * There are some entities which reference this one by our primary key.
     * @param S outer schema
     * @param R outer record
     * @param FS foreign schema
     * @param FR foreign record
     */
    @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR)
    class ToMany<S : Schema<S>, ID : IdBound, FS : Schema<FS>, FID : IdBound, C : Collection<Record<FS, FID>>> private constructor(
            ourTable: Table<S, ID>, path: StoredLens<S, C, *>, foreignTable: Table<FS, *>, joinColumn: StoredLens<FS, *, *>
    ) : ColMeta<S>(path) {
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
    class ManyToMany<S : Schema<S>, ID : IdBound, FS : Schema<FS>, C : Collection<Record<FS, *>>>(
            path: StoredLens<S, C, *>, foreignTable: Table<FS, *>, joinTable: JoinTable
    ) : ColMeta<S>(path) {
        init {
            checkToMany(TODO(), path, foreignTable)
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
    check(type is DataType.Collect<*, *, *>) {
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
