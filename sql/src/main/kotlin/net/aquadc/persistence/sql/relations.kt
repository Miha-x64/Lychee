package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType

/**
 * Describes which way a value of [DataType.Partial] of [Struct]/[Schema] type should be persisted in a relational database.
 */
sealed class Relation<S : Schema<S>, ID : IdBound, T>(
        @JvmField val path: StoredLens<S, T, *>
) {

    /**
     * Embed a (Partial)[Struct] of type [ET] into current table.
     * @param S outer [Schema]
     */
    class Embedded<S : Schema<S>, ID : IdBound, ET> : Relation<S, ID, ET> {

        /**
         * @param naming which will concat names
         * @param path a path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        constructor(naming: NamingConvention, path: StoredLens<S, ET, *>, fieldSetColName: String) :
                this(naming, path, fieldSetColName, null)

        /**
         * @param naming which will concat names
         * @param path a path to a value stored as a struct with [Schema]
         */
        constructor(naming: NamingConvention, path: StoredLens<S, ET, *>) :
                this(naming, path, null, null)

        val naming: NamingConvention
        val fieldSetColName: String?

        private constructor(naming: NamingConvention, path: StoredLens<S, ET, *>, fieldSetColName: String?, dummy: Nothing?) : super(path) {
            val t = path.type
            val unwrapped = if (t is DataType.Nullable<*, *>) t.actualType else t
            when {
                t is DataType.Nullable<*, *> -> if (fieldSetColName == null) throw NoSuchElementException(
                        "either use full (non-partial, non-nullable) type or call another constructor(naming, path, fieldSetColName)"
                )
                unwrapped is Schema<*> -> check(fieldSetColName == null) {
                    "either use partial/nullable type of call another constructor(naming, path)"
                }
                unwrapped is DataType.Partial<*, *> -> if (fieldSetColName == null) throw NoSuchElementException(
                        "either use full (non-partial, non-nullable) type or call another constructor(naming, path, fieldSetColName)"
                )
                else ->
                    error("only fields of Struct types can be used with such relations, got $unwrapped at $path")
            }

            this.naming = naming
            this.fieldSetColName = fieldSetColName
        }

    }

    /**
     * Reference a single entity by its primary key.
     * @param S outer schema
     * @param FS foreign schema
     */
    @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR)
    class ToOne<S : Schema<S>, ID : IdBound, FS : Schema<FS>, FID : IdBound, FR : Record<FS, FID>>(
            path: StoredLens<S, FR?, *>, foreignTable: Table<FS, *, FR>
    ) : Relation<S, ID, FR?>(path) {
        init {
            checkToOne(path, foreignTable)
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
    class ToMany<S : Schema<S>, ID : IdBound, FS : Schema<FS>, R : Record<S, ID>, FID : IdBound, FR : Record<FS, FID>, C : Collection<FR>> private constructor(
            ourTable: Table<S, ID, R>, path: StoredLens<S, C, *>, foreignTable: Table<FS, *, FR>, joinColumn: StoredLens<FS, *, *>
    ) : Relation<S, ID, C>(path) {
        init {
            checkToMany(path, foreignTable)
            checkToOne(joinColumn, ourTable) // ToMany is actually many ToOnes
        }

        companion object {
            operator fun <S : Schema<S>, ID : IdBound, FS : Schema<FS>, R : Record<S, ID>, FID : IdBound, FR : Record<FS, FID>, C : Collection<FR>> Table<S, ID, R>.invoke(
                    path: Lens<S, Record<S, ID>, Record<S, ID>, C, *>, foreignTable: Table<FS, *, FR>, joinColumn: Lens<FS, Record<FS, *>, Record<FS, *>, *, *>
            ): Nothing = TODO() // ToMany<S, ID, FS, R, FID, FR, C> = ToMany(this, path, foreignTable, joinColumn)
        }
    }

    @Deprecated("Not implemented yet.", level = DeprecationLevel.ERROR)
    class ManyToMany<S : Schema<S>, ID : IdBound, FS : Schema<FS>, FR : Record<FS, *>, C : Collection<FR>>(
            path: StoredLens<S, C, *>, foreignTable: Table<FS, *, FR>, joinTable: JoinTable
    ) : Relation<S, ID, C>(path) {
        init {
            checkToMany(path, foreignTable)
        }
    }

    // for tests

    override fun hashCode(): Int = path.hashCode()
    override fun equals(other: Any?): Boolean =
            other is Relation<*, *, *> && javaClass === other.javaClass && path == other.path

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

internal fun <S : Schema<S>, FS : Schema<FS>, FR : Record<FS, *>> checkToMany(
        path: StoredLens<S, *, *>, foreignTable: Table<FS, *, FR>) {
    val type = path.type
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

internal fun <S : Schema<S>, F : Schema<F>, R : Record<F, *>> checkToOne(path: StoredLens<S, *, *>, foreignTable: Table<F, *, R>) {
    val type = path.type
    val realType = if (type is DataType.Nullable<*, *>) type.actualType else type
    check(realType is Schema<*>) {
        "only fields of Struct types can be used with such relations, got $realType at $path"
    }
    check(realType.schema.javaClass == foreignTable.schema.javaClass) {
        "type of this field and Schema of referenced table must be compatible"
    }
}
