package net.aquadc.properties.sql

import net.aquadc.properties.internal.emptyArrayOf
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.lang.StringBuilder
import java.util.*


/**
 * A condition for record of type [TBL].
 * API is mostly borrowed from
 * https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java
 */
interface WhereCondition<TBL : Table<TBL, *, *>> {

    /**
     * Appends corresponding part of SQL query to [builder] using [dialect].
     */
    fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder

    /**
     * Appends contained colName-value-pairs to the given [list].
     */
    fun appendValuesTo(list: ArrayList<Pair<@ParameterName("colName") String, @ParameterName("value") Any>>)

    /**
     * Represents an absence of any conditions.
     */
    object Empty : WhereCondition<Nothing> {
        override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder = builder
        override fun appendValuesTo(list: ArrayList<Pair<String, Any>>) = Unit
    }

}

internal class ColCond<TBL : Table<TBL, *, *>, T> : WhereCondition<TBL> {

    // mutable for internal code, he-he
    @JvmField @JvmSynthetic internal var colName: String
    private val op: CharSequence
    private val singleValue: Boolean
    @JvmField @JvmSynthetic internal var valueOrValues: Any // if (singleValue) Any else Array<Any>

    constructor(col: Col<TBL, T>, op: CharSequence, value: Any) {
        this.colName = col.name
        this.op = op
        this.singleValue = true
        this.valueOrValues = value
    }

    constructor(col: Col<TBL, T>, op: CharSequence, values: Array<Any>) {
        this.colName = col.name
        this.op = op
        this.singleValue = false
        this.valueOrValues = values
    }

    override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder =
            with(dialect) { builder.appendName(colName) }.append(op)

    override fun appendValuesTo(list: ArrayList<Pair<String, Any>>) {
        if (singleValue) list.add(colName to valueOrValues)
        else (valueOrValues as Array<out Any>).forEach { value -> list.add(colName to value) }
    }

}

internal class BiCond<TBL : Table<TBL, *, *>>(
        private val left: WhereCondition<TBL>,
        private val and: Boolean,
        private val right: WhereCondition<TBL>
) : WhereCondition<TBL> {

    override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder {
        builder.append('(')
        left.appendSqlTo(dialect, builder)
                .append(if (and) " AND " else " OR ")
        return right.appendSqlTo(dialect, builder)
                .append(')')
    }

    override fun appendValuesTo(list: ArrayList<Pair<String, Any>>) {
        left.appendValuesTo(list)
        right.appendValuesTo(list)
    }

}

infix fun <TBL : Table<TBL, *, *>, T> Col<TBL, T>.eq(value: T): WhereCondition<TBL> =
        if (value == null) ColCond(this, " IS NULL", emptyArrayOf())
        else ColCond(this, " = ?", value as Any)

infix fun <TBL : Table<TBL, *, *>, T> Col<TBL, T>.notEq(value: T): WhereCondition<TBL> =
        if (value == null) ColCond(this, " IS NOT NULL", emptyArrayOf())
        else ColCond(this, " <> ?", value as Any)

infix fun <TBL : Table<TBL, *, *>, T : String?> Col<TBL, T>.like(value: String): WhereCondition<TBL> =
        ColCond(this, " LIKE ?", value)

infix fun <TBL : Table<TBL, *, *>, T : String?> Col<TBL, T>.notLike(value: String): WhereCondition<TBL> =
        ColCond(this, " NOT LIKE ?", value)

// let U be nullable, but not T
infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.between(range: Array<T>): WhereCondition<TBL> =
        ColCond(this, " BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.notBetween(range: Array<T>): WhereCondition<TBL> =
        ColCond(this, " NOT BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.isIn(values: Array<T>): WhereCondition<TBL> =
        ColCond(this, StringBuilder(" IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.notIn(values: Array<T>): WhereCondition<TBL> =
        ColCond(this, StringBuilder(" NOT IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.greaterThan(value: T): WhereCondition<TBL> =
        ColCond(this, " > ?", value)

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.greaterOrEq(value: T): WhereCondition<TBL> =
        ColCond(this, " >= ?", value)

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.lessThan(value: T): WhereCondition<TBL> =
        ColCond(this, " < ?", value)

infix fun <TBL : Table<TBL, *, *>, T : Any, U : T> Col<TBL, U>.lessOrEq(value: T): WhereCondition<TBL> =
        ColCond(this, " <= ?", value)


infix fun <TBL : Table<TBL, *, *>> WhereCondition<TBL>.and(that: WhereCondition<TBL>): WhereCondition<TBL> =
        BiCond(this, true, that)

infix fun <TBL : Table<TBL, *, *>> WhereCondition<TBL>.or(that: WhereCondition<TBL>): WhereCondition<TBL> =
        BiCond(this, false, that)


inline operator fun <reified T> T.rangeTo(that: T): Array<T> = arrayOf(this, that)
