@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.bind
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.StructDef
import net.aquadc.persistence.converter.Converter


typealias IdBound = Any // Serializable in some frameworks

typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

interface Session {

    operator fun <REC : Record<REC, ID>, ID : IdBound> get(table: Table<REC, ID>): Dao<REC, ID>

    fun beginTransaction(): Transaction

}

interface Dao<REC : Record<REC, ID>, ID : IdBound> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<out REC>, order: Array<out Order<REC>>/* TODO: prefetch */): Property<List<REC>> // TODO DiffProperty
    // todo raw queries, joins
    fun count(condition: WhereCondition<out REC>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition

    /**
     * TODO KDoc
     * Note: returned [Property] is not managed itself, [Record]s are.
     */
    fun <T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<Transaction, T>
}

inline fun <R> Session.withTransaction(block: Transaction.() -> R): R {
    val transaction = beginTransaction()
    try {
        val r = block(transaction)
        transaction.setSuccessful()
        return r
    } finally {
        transaction.close()
    }
}

fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.require(id: ID): REC =
        find(id) ?: throw IllegalStateException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.select(condition: WhereCondition<out REC>, vararg order: Order<REC>/* TODO: prefetch */): Property<List<REC>> =
        select(condition, order)

fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.selectAll(vararg order: Order<REC>): Property<List<REC>> =
        select(WhereCondition.Empty, order)

fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.count(): Property<Long> =
        count(WhereCondition.Empty)

@JvmField val NoOrder = emptyArray<Order<Nothing>>()


interface Transaction : AutoCloseable {

    val session: Session

    fun <REC : Record<REC, ID>, ID : IdBound> insert(table: Table<REC, ID>, vararg contentValues: ColValue<REC, *>): ID

    fun <REC : Record<REC, ID>, ID : IdBound, T> update(table: Table<REC, ID>, id: ID, column: Col<REC, T>, value: T)

    fun <REC : Record<REC, ID>, ID : IdBound> delete(record: REC)

    fun setSuccessful()

    fun <T> SqlProperty<T>.set(new: T) {
        this.setValue(this@Transaction, new)
    }

}

class Order<REC : Record<REC, *>>(
        @JvmField internal val col: Col<REC, *>,
        @JvmField internal val desc: Boolean
)

val <REC : Record<REC, *>> Col<REC, *>.asc: Order<REC>
    get() = Order(this, false)

val <REC : Record<REC, *>> Col<REC, *>.desc: Order<REC>
    get() = Order(this, true)


abstract class Table<REC : Record<REC, ID>, ID : IdBound>(
        name: String,
        val idColConverter: Converter<ID>,
        val idColName: String
) : StructDef<REC>(name) {


    abstract fun create(session: Session, id: ID): REC // TODO: rename: instantiate, instantiateRecord, createRecord, newRecord

    final override fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<REC, *>>) {
        check(idColName !in nameSet) { "duplicate column: `$name`.`$idColName`" }
    }

}


typealias Col<REC, T> = FieldDef<REC, T>


/**
 * Represents an active record — a container with some properties.
 * Subclass it to provide your own getters and/or computed/foreign properties.
 * TODO: should I provide subclassing-less API, too?
 */
open class Record<REC : Record<REC, ID>, ID : IdBound>(
        internal val table: Table<REC, ID>,
        internal val session: Session,
        val primaryKey: ID
) : Struct<REC> {

    @JvmField
    @JvmSynthetic
    internal val fields: Array<ManagedProperty<Transaction, out Any?>> =
            session[table].let { dao ->
                table.fields.mapToArray { col ->
                    dao.createFieldOf(col as Col<REC, Nothing>, primaryKey)
                }
            }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> propOf(field: FieldDef<REC, T>): SqlProperty<T> =
            fields[field.ordinal.toInt()] as SqlProperty<T>

    override fun <T> getValue(field: FieldDef<REC, T>): T =
            propOf(field).value

    operator fun <T> get(col: Col<REC, T>): SqlProperty<T> =
            propOf(col)

    val isManaged: Boolean
        get() = fields[0].isManaged

    @Suppress("UNCHECKED_CAST") // id is not nullable, so ForeREC won't be, too
    infix fun <ForeREC : Record<ForeREC, ForeID>, ForeID : IdBound>
            Col<REC, ForeID>.toOne(foreignTable: Table<ForeREC, ForeID>): SqlProperty<ForeREC> =
            (this as Col<REC, ForeID?>).toOneNullable(foreignTable) as SqlProperty<ForeREC>

    infix fun <ForeREC : Record<ForeREC, ForeID>, ForeID : IdBound>
            Col<REC, ForeID?>.toOneNullable(foreignTable: Table<ForeREC, ForeID>): SqlProperty<ForeREC?> =
            this@Record[this@toOneNullable].bind(
                    { id: ForeID? -> if (id == null) null else session[foreignTable].require(id) },
                    { it: ForeREC? -> it?.primaryKey }
            )

    infix fun <ForeREC : Record<ForeREC, ForeID>, ForeID : IdBound>
            Col<ForeREC, ID>.toMany(foreignTable: Table<ForeREC, ForeID>): Property<List<ForeREC>> =
            session[foreignTable].select(this eq primaryKey)

}


class ColValue<REC : Record<REC, *>, T>(val col: Col<REC, T>, val value: T)

/**
 * Creates a type-safe mapping from a column to its value.
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <REC : Record<REC, *>, T> Col<REC, T>.minus(value: T): ColValue<REC, T> =
        ColValue(this, value)


/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [REC].
 */
fun <REC : Record<REC, *>, T> propertyGetterOf(field: FieldDef<REC, T>): (REC) -> Property<T> =
        { it[field] }


internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    val array = arrayOfNulls<R>(size)
    for (i in indices) {
        array[i] = transform(this[i])
    }
    @Suppress("UNCHECKED_CAST") // now it's filled with items and not thus not nullable
    return array as Array<R>
}
