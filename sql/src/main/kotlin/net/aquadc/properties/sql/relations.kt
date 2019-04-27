package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType


/**
 * Describes which way a value of [DataType.Partial] of [Struct]/[Schema] type should be persisted in a relational database.
 */
sealed class Relation {

    /**
     * Embed a (Partial)[Struct] of type [NS] into current table.
     * @param S outer [Schema]
     * @param NS nested [Schema]
     */
    class Embedded<S : Schema<S>, NS : Schema<NS>> : Relation {

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

        private constructor(factory: LensFactory, path: Lens<S, *>, fieldSetColName: String?, dummy: Nothing?) {
            when (path.type) {
                is Schema<*> -> check(fieldSetColName == null) {
                    "either use partial type of call another constructor(factory, path)"
                }
                is DataType.Partial<*, *> -> check(fieldSetColName != null) {
                    "either use full (non-partial) type or call another constructor(factory, path, fieldSetColName)"
                }
                else -> error("only fields of Struct types can be used with such relations")
            }

            /*val schema = (path.type as DataType.Partial<*, *>).schema as NS
            val nestedFields = schema.fields.map {
                factory.concatNames(path.name, it.name)
            }*/
        }

    }

    /**
     * Reference a single entity by its primary key.
     * @param S outer schema
     * @param FS foreign schema
     */
    class ToOne<S : Schema<S>, FS : Schema<FS>, FR : Record<FS, *>>(
            path: Lens<S, *>, foreignTable: Table<FS, *, FR>
    ) : Relation() {
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
    class ToMany<S : Schema<S>, SR : Record<S, *>, FS : Schema<FS>, FR : Record<FS, *>>
    private constructor(ourTable: Table<S, *, SR>, path: Lens<S, *>, foreignTable: Table<FS, *, FR>, joinColumn: Lens<FS, *>) : Relation() {
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
    ) : Relation() {
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
        "only fields of Struct types can be used with such relations"
    }
    check(realType.schema.javaClass == foreignTable.schema.javaClass) {
        "type of this field and Schema of referenced table must be compatible"
    }
}
