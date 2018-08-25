@file:Suppress("KDocMissingDocumentation") // fixme add later

package net.aquadc.properties.sql

import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.bind
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import java.util.*


typealias IdBound = Any // Serializable in some frameworks

typealias SqlProperty<T> = TransactionalProperty<Transaction, T>

interface Session {

    operator fun <REC : Record<REC, ID>, ID : IdBound> get(table: Table<REC, ID>): Dao<REC, ID>

    fun beginTransaction(): Transaction

    /**
     * TODO KDoc
     * Note: returned [Property] is not managed itself, [Record]s are.
     */
    fun <REC : Record<REC, ID>, ID : IdBound, T> createFieldOf(col: Col<REC, T>, id: ID): ManagedProperty<Transaction, T>
}

interface Dao<REC : Record<REC, ID>, ID : IdBound> {
    fun find(id: ID /* TODO fields to prefetch */): REC?
    fun select(condition: WhereCondition<out REC>/* TODO: order, prefetch */): Property<List<REC>> // TODO DiffProperty
    fun count(condition: WhereCondition<out REC>): Property<Long>

    // why they have 'out' variance? Because we want to use a single WhereCondition<Nothing> when there's no condition
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

fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.select(): Property<List<REC>> =
        select(WhereCondition.Empty)

fun <REC : Record<REC, ID>, ID : IdBound> Dao<REC, ID>.count(): Property<Long> =
        count(WhereCondition.Empty)


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


abstract class Table<REC : Record<REC, ID>, ID : IdBound>(
        val name: String,
        type: Class<ID>
) {

    private var tmp: Pair<ArrayList<Col<REC, *>>, Class<ID>>? = Pair(ArrayList(), type)
    // todo: check what's more mem-efficient — one or two fields

    /**
     * {@implNote
     *   on concurrent access, we might null out [tmp] while it's getting accessed,
     *   so let it be synchronized (it's default [lazy] mode).
     * }
     */
    val columns: List<Col<REC, *>> by lazy {
        var idCol: Col<REC, ID>? = null
        val set = HashSet<Col<REC, *>>()
        val tmpCols = tmp!!.first
        for (i in tmpCols.indices) {
            val col = tmpCols[i]
            if (col.isPrimaryKey) {
                if (idCol != null) {
                    throw  IllegalStateException("duplicate primary key `$name`.`${col.name}`, already have `${idCol.name}`")
                }
                idCol = col as Col<REC, ID>
            }
            if (!set.add(col)) {
                throw IllegalStateException("duplicate column: `$name`.`${col.name}`")
            }
        }

        _idCol = idCol ?: throw IllegalStateException("table `$name` must have a primary key column")
        //                ^ note: isManaged also relies on the fact that record has at least one field.
        val frozen = Collections.unmodifiableList(tmpCols)
        tmp = null
        frozen
    }

    abstract fun create(session: Session, id: ID): REC

    private var _idCol: Col<REC, ID>? = null
    val idCol: Col<REC, ID>
        get() = _idCol ?: columns.let { _ -> _idCol!! }


    protected inline fun <reified T> nullableCol(name: String): Col<REC, T?> =
            col0(false, name, T::class.java as Class<T?>, true)

    protected inline fun <reified T : Any> col(name: String /* TODO converter */): Col<REC, T>
            = col0(false, name, T::class.java, false)

    protected fun idCol(name: String): Col<REC, ID> =
            col0(pk = true, name = name, type = tmp().second, nullable = false)

    @PublishedApi internal fun <T> col0(pk: Boolean, name: String, type: Class<T>, nullable: Boolean): Col<REC, T> {
        val cols = tmp().first
        val col = Col<REC, T>(this, pk, name, type, nullable, cols.size)
        cols.add(col)
        return col
    }

    private fun tmp() = tmp ?: throw IllegalStateException("table `$name` is already initialized")

}


class Col<REC : Record<REC, *>, T>(
        val table: Table<REC, *>,
        val isPrimaryKey: Boolean,
        val name: String,
        val javaType: Class<out T>,
        val isNullable: Boolean,
        val ordinal: Int
) : Manager.Column<T> {
    override fun toString(): String = table.name + '.' + name
}


/**
 * Represents an active record — a container with some properties.
 * TODO: should I provide subclassing-less API, too?
 */
abstract class Record<REC : Record<REC, ID>, ID : IdBound>(
        internal val table: Table<REC, ID>,
        internal val session: Session,
        val primaryKey: ID
) {

    @JvmField @JvmSynthetic
    internal val fields: Array<ManagedProperty<Transaction, out Any?>> =
            table.columns.mapToArray { session.createFieldOf(it, primaryKey) }

    operator fun <T> get(col: Col<REC, T>): SqlProperty<T> =
            fields[col.ordinal] as SqlProperty<T>

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
inline operator fun <REC : Record<REC, *>, T> Col<REC, T>.minus(value: T) = ColValue(this, value)

// fixme may not be part of lib API

inline fun <reified T> t(): Class<T> = T::class.java

inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    val array = arrayOfNulls<R>(size)
    for (i in indices) {
        array[i] = transform(this[i])
    }
    return array as Array<R>
}
