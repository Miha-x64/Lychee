package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema


internal class PrimaryKeys<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession,
        private val order: Array<out Order<SCH>>
) : (WhereCondition<out SCH>) -> Array<ID> {

    override fun invoke(condition: WhereCondition<out SCH>): Array<ID> =
            lowSession.fetchPrimaryKeys(table, condition, order)

}

internal class Query<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val dao: Dao<SCH, ID, REC>,
        private val table: Table<SCH, ID, REC>,
        private val lowSession: LowLevelSession,
        private val condition: WhereCondition<out SCH>,
        private val order: Array<out Order<SCH>>
): (Array<ID>) -> Selection<SCH, ID, REC> {

    override fun invoke(primaryKeys: Array<ID>): Selection<SCH, ID, REC> =
            Selection(dao, lowSession.fetchPrimaryKeys(table, condition, order))

}

internal class Count<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession
): (WhereCondition<out SCH>) -> Long {

    override fun invoke(condition: WhereCondition<out SCH>): Long =
            lowSession.fetchCount(table, condition)

}

internal class Selection<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val dao: Dao<SCH, ID, REC>,
        private val primaryKeys: Array<ID>
) : AbstractList<REC>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): REC =
            dao.require(primaryKeys[index])

}
