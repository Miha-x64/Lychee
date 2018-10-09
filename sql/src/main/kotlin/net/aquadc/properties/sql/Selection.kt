package net.aquadc.properties.sql


internal class PrimaryKeys<TBL : Table<TBL, ID, *>, ID : IdBound>(
        private val table: Table<TBL, ID, *>,
        private val lowSession: LowLevelSession,
        private val order: Array<out Order<TBL>>
) : (WhereCondition<out TBL>) -> Array<ID> {

    override fun invoke(condition: WhereCondition<out TBL>): Array<ID> =
            lowSession.fetchPrimaryKeys(table, condition, order)

}

internal class Query<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>>(
        private val dao: Dao<TBL, ID, REC>,
        private val table: Table<TBL, ID, REC>,
        private val lowSession: LowLevelSession,
        private val condition: WhereCondition<out TBL>,
        private val order: Array<out Order<TBL>>
): (Array<ID>) -> Selection<TBL, ID, REC> {

    override fun invoke(primaryKeys: Array<ID>): Selection<TBL, ID, REC> =
            Selection(dao, lowSession.fetchPrimaryKeys(table, condition, order))

}

internal class Count<TBL : Table<TBL, ID, *>, ID : IdBound>(
        private val table: Table<TBL, ID, *>,
        private val lowSession: LowLevelSession
): (WhereCondition<out TBL>) -> Long {

    override fun invoke(condition: WhereCondition<out TBL>): Long =
            lowSession.fetchCount(table, condition)

}

internal class Selection<TBL : Table<TBL, ID, REC>, ID : IdBound, REC : Record<TBL, ID>>(
        private val dao: Dao<TBL, ID, REC>,
        private val primaryKeys: Array<ID>
) : AbstractList<REC>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): REC =
            dao.require(primaryKeys[index])

}
