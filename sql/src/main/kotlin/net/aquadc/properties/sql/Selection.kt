package net.aquadc.properties.sql


class Selection<E : Record<E, ID>, ID : IdBound>(
        private val session: Session,
        private val table: Table<E, ID>,
        private val condition: WhereCondition<E>,
        private val primaryKeys: Array<ID>
) : AbstractList<E>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): E =
            session.require(table, primaryKeys[index])

}
