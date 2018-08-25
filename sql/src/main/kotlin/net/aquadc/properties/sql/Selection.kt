package net.aquadc.properties.sql


class Selection<E : Record<E, ID>, ID : IdBound>(
        private val dao: Dao<E, ID>,
        @JvmField internal val condition: WhereCondition<out E>,
        private val primaryKeys: Array<ID>
) : AbstractList<E>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): E =
            dao.require(primaryKeys[index])

}
