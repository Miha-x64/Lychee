@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.bind
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.StructDef
import net.aquadc.persistence.converter.Converter
import net.aquadc.persistence.struct.BaseStruct


typealias IdBound = Any // Serializable in some frameworks

typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

interface Session {

    operator fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> get(
            table: Table<TBL, ID, REC>
    ): Dao<TBL, ID, REC>

    fun beginTransaction(): Transaction

}

interface Dao<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<out TBL>, order: Array<out Order<TBL>>/* TODO: prefetch */): Property<List<REC>> // TODO DiffProperty
    // todo raw queries, joins
    fun count(condition: WhereCondition<out TBL>): Property<Long>
    // why do they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition

    /**
     * TODO KDoc
     * Note: returned [Property] is not managed itself, [Record]s are.
     */
    fun <T> createFieldOf(col: Col<TBL, T>, id: ID): ManagedProperty<Transaction, T> // fixme may be in LowLevel
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

fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.require(id: ID): REC =
        find(id) ?: throw IllegalStateException("No record found in `$this` for ID $id")

@Suppress("NOTHING_TO_INLINE")
inline fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.select(
        condition: WhereCondition<out TBL>, vararg order: Order<TBL>/* TODO: prefetch */
): Property<List<REC>> =
        select(condition, order)

fun <TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>> Dao<TBL, ID, REC>.selectAll(vararg order: Order<TBL>): Property<List<REC>> =
        select(WhereCondition.Empty, order)

fun Dao<*, *, *>.count(): Property<Long> =
        count(WhereCondition.Empty)

@JvmField val NoOrder = emptyArray<Order<Nothing>>()


interface Transaction : AutoCloseable {

    val session: Session

    fun <TBL : Table<TBL, ID, *>, ID : IdBound> insert(table: Table<TBL, ID, *>, vararg contentValues: ColValue<TBL, *>): ID

    fun <TBL : Table<TBL, ID, *>, ID : IdBound, T> update(table: Table<TBL, ID, *>, id: ID, column: Col<TBL, T>, value: T)

    fun <TBL : Table<TBL, ID, *>, ID : IdBound> delete(record: Record<TBL, ID>)

    fun setSuccessful()

    fun <T> SqlProperty<T>.set(new: T) {
        this.setValue(this@Transaction, new)
    }

}

class Order<TBL : Table<TBL, *, *>>( // fixme replace with typealias to Pair
        @JvmField internal val col: Col<TBL, *>,
        @JvmField internal val desc: Boolean
)

val <TBL : Table<TBL, *, *>> Col<TBL, *>.asc: Order<TBL>
    get() = Order(this, false)

val <TBL : Table<TBL, *, *>> Col<TBL, *>.desc: Order<TBL>
    get() = Order(this, true)


/**
 * Represents a table, i. e. defines structs which can be persisted in a database.
 * @param TBL self, i. e. this table
 * @param ID  primary key type
 * @param REC type of record, which can be simply `Record<TBL>` or a custom class extending [Record]
 */
abstract class Table<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>>(
        name: String,
        val idColConverter: Converter<ID>,
        val idColName: String
) : StructDef<TBL>(name) {


    abstract fun create(session: Session, id: ID): REC // TODO: rename: instantiate, instantiateRecord, createRecord, newRecord

    final override fun beforeFreeze(nameSet: Set<String>, fields: List<FieldDef<TBL, *>>) {
        check(idColName !in nameSet) { "duplicate column: `$name`.`$idColName`" }
    }

}


typealias Col<REC, T> = FieldDef<REC, T>


/**
 * Represents an active record — a container with some properties.
 * Subclass it to provide your own getters and/or computed/foreign properties.
 * TODO: should I provide subclassing-less API, too?
 */
open class Record<TBL : Table<TBL, ID, *>, ID : IdBound>(
        internal val table: Table<TBL, ID, *>,
        internal val session: Session,
        val primaryKey: ID
) : BaseStruct<TBL>(table) {

    @JvmField
    @JvmSynthetic
    internal val fields: Array<ManagedProperty<Transaction, out Any?>> =
            @Suppress("UPPER_BOUND_VIOLATED") // RLY, I don't want third generic for Record, this adds no type-safety here
            session.get<TBL, ID, Record<TBL, ID>>(table as Table<TBL, ID, Record<TBL, ID>>).let { dao ->
                table.fields.mapToArray { col ->
                    dao.createFieldOf(col as Col<TBL, Nothing>, primaryKey)
                }
            }

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    private inline fun <T> propOf(field: FieldDef<TBL, T>): SqlProperty<T> =
            fields[field.ordinal.toInt()] as SqlProperty<T>

    override fun <T> getValue(field: FieldDef<TBL, T>): T =
            propOf(field).value

    operator fun <T> get(col: Col<TBL, T>): SqlProperty<T> =
            propOf(col)

    val isManaged: Boolean
        get() = fields[0].isManaged

    @Suppress("UNCHECKED_CAST") // id is not nullable, so Record<ForeTBL> won't be, too
    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            Col<TBL, ForeID>.toOne(foreignTable: Table<ForeTBL, ForeID, ForeREC>): SqlProperty<ForeREC> =
            (this as Col<TBL, ForeID?>).toOneNullable(foreignTable) as SqlProperty<ForeREC>

    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            Col<TBL, ForeID?>.toOneNullable(foreignTable: Table<ForeTBL, ForeID, ForeREC>): SqlProperty<ForeREC?> =
            this@Record[this@toOneNullable].bind(
                    { id: ForeID? -> if (id == null) null else session[foreignTable].require(id) },
                    { it: ForeREC? -> it?.primaryKey }
            )

    infix fun <ForeTBL : Table<ForeTBL, ForeID, ForeREC>, ForeID : IdBound, ForeREC : Record<ForeTBL, ForeID>>
            Col<ForeTBL, ID>.toMany(foreignTable: Table<ForeTBL, ForeID, ForeREC>): Property<List<ForeREC>> =
            session[foreignTable].select(this eq primaryKey)

}


class ColValue<TBL : Table<TBL, *, *>, T>(val col: Col<TBL, T>, val value: T) // TODO replace with typealias

/**
 * Creates a type-safe mapping from a column to its value.
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <TBL : Table<TBL, *, *>, T> Col<TBL, T>.minus(value: T): ColValue<TBL, T> =
        ColValue(this, value)


/**
 * Creates a property getter, i. e. a function which returns a property of a pre-set [field] of a given [TBL].
 */
fun <TBL : Table<TBL, *, *>, T> propertyGetterOf(field: FieldDef<TBL, T>): (Record<TBL, *>) -> Property<T> =
        { it[field] }


internal inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    val array = arrayOfNulls<R>(size)
    for (i in indices) {
        array[i] = transform(this[i])
    }
    @Suppress("UNCHECKED_CAST") // now it's filled with items and not thus not nullable
    return array as Array<R>
}
