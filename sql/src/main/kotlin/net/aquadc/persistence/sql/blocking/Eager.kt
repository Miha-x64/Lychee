package net.aquadc.persistence.sql.blocking

import net.aquadc.persistence.sql.BindBy
import net.aquadc.persistence.sql.Exec
import net.aquadc.persistence.sql.Fetch
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.mapRow
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StructSnapshot
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.Ilk
import net.aquadc.persistence.type.nothing

@PublishedApi internal class FetchCellEagerly<CUR, R>(
        private val rt: Ilk<out R, *>,
        private val orElse: () -> R
) : Fetch<Blocking<CUR>, R> {
    override fun fetch(
        from: Blocking<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): R =
        from.cell(query, argumentTypes, receiverAndArguments, rt, orElse)
}

@PublishedApi internal class FetchColEagerly<CUR, R>(
        private val rt: Ilk<R, *>
) : Fetch<Blocking<CUR>, List<R>> {
    override fun fetch(
        from: Blocking<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): List<R> {
        val cur = from.select(query, argumentTypes, receiverAndArguments, 1)
        try {
            return if (from.next(cur)) {
                val first = from.cellAt(cur, 0, rt)
                if (from.next(cur)) {
                    ArrayList<R>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.cellAt(cur, 0, rt)) while (from.next(cur))
                    }
                } else listOf(first)
            } else emptyList()
        } finally {
            from.close(cur)
        }
    }
}

@PublishedApi internal class FetchStructEagerly<SCH : Schema<SCH>, CUR>(
    private val table: Table<SCH, *>,
    private val bindBy: BindBy,
    private val orElse: () -> StructSnapshot<SCH>?,
) : Fetch<Blocking<CUR>, StructSnapshot<SCH>?> {
    override fun fetch(
        from: Blocking<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): StructSnapshot<SCH>? {
        val managedColNames = table.managedColNames
        val managedColTypes = table.managedColTypes
        val cur = from.select(query, argumentTypes, receiverAndArguments, managedColNames.size)
        try {
            if (!from.next(cur)) return orElse()
            val value = from.mapRow<CUR, SCH>(bindBy, cur, managedColNames, managedColTypes, table.recipe)
            check(!from.next(cur)) // single row expected
            return value
        } finally {
            from.close(cur)
        }
    }
}

@PublishedApi internal class FetchStructListEagerly<CUR, SCH : Schema<SCH>>(
        private val table: Table<SCH, *>,
        private val bindBy: BindBy
) : Fetch<Blocking<CUR>, List<StructSnapshot<SCH>>> {
    override fun fetch(
        from: Blocking<CUR>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): List<StructSnapshot<SCH>> {
        val colNames = table.managedColNames
        val colTypes = table.managedColTypes
        val recipe = table.recipe

        val cur = from.select(query, argumentTypes, receiverAndArguments, colNames.size)
        try {
            return if (from.next(cur)) {
                val first = from.mapRow<CUR, SCH>(bindBy, cur, colNames, colTypes, recipe)
                if (from.next(cur)) {
                    ArrayList<StructSnapshot<SCH>>(from.sizeHint(cur).let { if (it < 0) 10 else it }).also {
                        it.add(first)
                        do it.add(from.mapRow(bindBy, cur, colNames, colTypes, recipe)) while (from.next(cur))
                    }
                } else listOf<StructSnapshot<SCH>>(first)
            } else emptyList()
        } finally {
            from.close(cur)
        }
    }
}

@PublishedApi @JvmField internal val ExecuteForUnit = ExecuteEagerlyFor(nothing)
    as Fetch<Blocking<*>, Unit>
@PublishedApi @JvmField internal val ExecuteForRowCount = ExecuteEagerlyFor<Nothing>(null)
    as Fetch<Blocking<*>, Int>

@PublishedApi internal class ExecuteEagerlyFor<ID>(
    private val retKeyType: Ilk<ID, DataType.NotNull.Simple<ID>>?
) : Exec<Blocking<*>, Any?> {
    override fun fetch(
        from: Blocking<*>, query: String,
        argumentTypes: Array<out Ilk<*, DataType.NotNull<*>>>, receiverAndArguments: Array<out Any>
    ): Any? {
        val ret = from.execute(query, argumentTypes, receiverAndArguments, if (retKeyType === nothing) null else retKeyType)
        return if (retKeyType === nothing) Unit else ret // if (retKeyType == null) affected else insertedPrimaryKey
    }
}
