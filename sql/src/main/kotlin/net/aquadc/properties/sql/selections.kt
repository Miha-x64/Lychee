package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema
import java.util.Arrays


internal class PrimaryKeys<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession<*>
) : (ConditionAndOrder<SCH>) -> Array<ID> {

    override fun invoke(cor: ConditionAndOrder<SCH>): Array<ID> =
            lowSession.fetchPrimaryKeys(table, cor.condition, cor.order)

}

internal class Query<SCH : Schema<SCH>, ID : IdBound, REC : Record<SCH, ID>>(
        private val dao: Dao<SCH, ID, REC>
) : (Array<ID>) -> Selection<SCH, ID, REC> {

    override fun invoke(primaryKeys: Array<ID>): Selection<SCH, ID, REC> =
            Selection(dao, primaryKeys)

}

internal class Count<SCH : Schema<SCH>, ID : IdBound>(
        private val table: Table<SCH, ID, *>,
        private val lowSession: LowLevelSession<*>
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

internal class ConditionAndOrder<SCH : Schema<SCH>>(
        @JvmField internal val condition: WhereCondition<out SCH>,
        @JvmField internal val order: Array<out Order<SCH>>
) {

    private var hash = 0

    override fun hashCode(): Int =
            if (hash == 0) (31 * condition.hashCode() + Arrays.hashCode(order)).also { hash = it }
            else hash

    override fun equals(other: Any?): Boolean =
            other === this ||
                    (other is ConditionAndOrder<*> && other.condition == condition && Arrays.equals(other.order, order))

}
