package net.aquadc.properties.sql


class Selection<E : Record<E, ID>, ID : IdBound>(
        private val session: Session,
        @JvmField internal val table: Table<E, ID>,
        @JvmField internal val condition: WhereCondition<out E>,
        private val primaryKeys: Array<ID>
) : AbstractList<E>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): E =
            session.require(table, primaryKeys[index])

}
