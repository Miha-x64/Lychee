package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType


/**
 * Describes which way a value of [DataType.Partial] of [Struct]/[Schema] type should be persisted in a relational database.
 */
sealed class Relation<S : Schema<S>, P : Lens<S, *>?>(
        @JvmField val path: P
) {

    /**
     * A special marker for primary key. Should not be used directly.
     */
    object PrimaryKey : Relation<Nothing, Nothing?>(null)

    /**
     * Embed a (Partial)[Struct] of type [NS] into current table.
     * @param S outer [Schema]
     */
    class Embedded<S : Schema<S>> : Relation<S, Lens<S, *>> {

        /**
         * @param factory which will concat names
         * @param path a path to a value stored as a [DataType.Partial]
         * @param fieldSetColName a name of a column which will internally be used to remember which fields are set
         */
        constructor(factory: LensFactory, path: Lens<S, *>, fieldSetColName: String) :
                this(factory, path, fieldSetColName, null)

        /**
         * @param factory which will concat names
         * @param path a path to a value stored as a struct with [Schema]
         */
        constructor(factory: LensFactory, path: Lens<S, *>) :
                this(factory, path, null, null)

        val factory: LensFactory
        val fieldSetColName: String?

        private constructor(factory: LensFactory, path: Lens<S, *>, fieldSetColName: String?, dummy: Nothing?) : super(path) {
            val t = path.type
            val unwrapped = if (t is DataType.Nullable) t.actualType else t
            when {
                t is DataType.Nullable<*> -> if (fieldSetColName == null) throw NoSuchElementException(
                        "either use full (non-partial, non-nullable) type or call another constructor(factory, path, fieldSetColName)"
                )
                unwrapped is Schema<*> -> check(fieldSetColName == null) {
                    "either use partial/nullable type of call another constructor(factory, path)"
                }
                unwrapped is DataType.Partial<*, *> -> if (fieldSetColName == null) throw NoSuchElementException(
                        "either use full (non-partial, non-nullable) type or call another constructor(factory, path, fieldSetColName)"
                )
                else ->
                    error("only fields of Struct types can be used with such relations, got $unwrapped at $path")
            }

            this.factory = factory
            this.fieldSetColName = fieldSetColName
        }

    }

    /**
     * Reference a single entity by its primary key.
     * @param S outer schema
     * @param FS foreign schema
     */
    class ToOne<S : Schema<S>, FS : Schema<FS>, FR : Record<FS, *>>(
            path: Lens<S, *>, foreignTable: Table<FS, *, FR>
    ) : Relation<S, Lens<S, *>>(path) {
        init {
            checkToOne(path, foreignTable)
        }
    }

    /**
     * There are some entities which reference this one by our primary key.
     * @param S outer schema
     * @param SR outer record
     * @param FS foreign schema
     * @param FR foreign record
     */
    class ToMany<S : Schema<S>, SR : Record<S, *>, FS : Schema<FS>, FR : Record<FS, *>> private constructor(
            ourTable: Table<S, *, SR>, path: Lens<S, *>, foreignTable: Table<FS, *, FR>, joinColumn: Lens<FS, *>
    ) : Relation<S, Lens<S, *>>(path) {
        init {
            checkToMany(path, foreignTable)
            checkToOne(joinColumn, ourTable) // ToMany is actually many ToOnes
        }

        companion object {
            operator fun <S : Schema<S>, SR : Record<S, *>, F : Schema<F>, FR : Record<F, *>> Table<S, *, SR>.invoke(
                    path: Lens<S, *>, foreignTable: Table<F, *, FR>, joinColumn: Lens<F, *>
            ) = ToMany(this, path, foreignTable, joinColumn)
        }
    }

    class ManyToMany<S : Schema<S>, FS : Schema<FS>, FR : Record<FS, *>>(
            path: Lens<S, *>, foreignTable: Table<FS, *, FR>, joinTable: JoinTable
    ) : Relation<S, Lens<S, *>>(path) {
        init {
            checkToMany(path, foreignTable)
        }
    }

}

/**
 * A special case of a table which has no PK and consists of two columns with a composite unique index.
 */
class JoinTable(
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

}

private fun <S : Schema<S>, FS : Schema<FS>, FR : Record<FS, *>> checkToMany(
        path: Lens<S, *>, foreignTable: Table<FS, *, FR>) {
    val type = path.type
    check(type is DataType.Collect<*, *>) {
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

private fun <S : Schema<S>, F : Schema<F>, R : Record<F, *>> checkToOne(path: Lens<S, *>, foreignTable: Table<F, *, R>) {
    val type = path.type
    val realType = if (type is DataType.Nullable<*>) type.actualType else type
    check(realType is Schema<*>) {
        "only fields of Struct types can be used with such relations, got $realType at $path"
    }
    check(realType.schema.javaClass == foreignTable.schema.javaClass) {
        "type of this field and Schema of referenced table must be compatible"
    }
}
