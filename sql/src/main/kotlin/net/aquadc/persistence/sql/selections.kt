package net.aquadc.persistence.sql

import net.aquadc.persistence.sql.blocking.LowLevelSession
import net.aquadc.persistence.struct.Schema


internal class PrimaryKeys<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession<*, *>
) : (ConditionAndOrder<SCH>) -> Array<ID> {

    override fun invoke(cor: ConditionAndOrder<SCH>): Array<ID> =
            lowSession.fetchPrimaryKeys(table, cor.condition, cor.order)

}

internal class Count<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession<*, *>
): (WhereCondition<SCH>) -> Long {

    override fun invoke(condition: WhereCondition<SCH>): Long =
            lowSession.fetchCount(table, condition)

}

internal class ListSelection<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val dao: Dao<SCH, ID, REC>,
        private val primaryKeys: Array<ID>
) : AbstractList<REC>() {

    override val size: Int
        get() = primaryKeys.size

    override fun get(index: Int): REC =
            dao.require(primaryKeys[index])

}

internal class ConditionAndOrder<SCH : Schema<SCH>>(
        @JvmField internal val condition: WhereCondition<SCH>,
        @JvmField internal val order: Array<out Order<SCH>>
) {

    private var hash = 0

    override fun hashCode(): Int =
            if (hash == 0) (31 * condition.hashCode() + order.contentHashCode()).also { hash = it }
            else hash

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is ConditionAndOrder<*> && other.condition == condition && other.order.contentEquals(order))

}
