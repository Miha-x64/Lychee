package net.aquadc.properties.sql


internal class Query<E : Record<E, ID>, ID : IdBound>(
        private val dao: Dao<E, ID>
): (WhereCondition<out E>) -> Selection<E, ID> {

    override fun invoke(condition: WhereCondition<out E>): Selection<E, ID> =
            Selection(dao, dao.fetchPrimaryKeys(condition))

}

internal class Count<E : Record<E, ID>, ID : IdBound>(
        private val dao: Dao<E, ID>
): (WhereCondition<out E>) -> Long {

    override fun invoke(condition: WhereCondition<out E>): Long =
            dao.fetchCount(condition)

}

internal class Selection<E : Record<E, ID>, ID : IdBound>(
        private val dao: Dao<E, ID>,
        private val primaryKeys: Array<ID>
) : AbstractList<E>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): E =
            dao.require(primaryKeys[index])

}
