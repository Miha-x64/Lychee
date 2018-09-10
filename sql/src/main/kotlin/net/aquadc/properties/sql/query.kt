package net.aquadc.properties.sql

import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.lang.StringBuilder
import java.util.*


/**
 * A condition for record of type [REC].
 * API is mostly borrowed from
 * https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java
 */
interface WhereCondition<REC : Record<REC, *>> {

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

internal class ColCond<REC : Record<REC, *>, T> : WhereCondition<REC> {

    // mutable for internal code, he-he
    @JvmField @JvmSynthetic internal var colName: String
    private val op: CharSequence
    private val singleValue: Boolean
    @JvmField @JvmSynthetic internal var valueOrValues: Any // if (singleValue) Any else Array<Any>

    constructor(col: Col<REC, T>, op: CharSequence, value: Any) {
        this.colName = col.name
        this.op = op
        this.singleValue = true
        this.valueOrValues = value
    }

    constructor(col: Col<REC, T>, op: CharSequence, values: Array<Any>) {
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

internal class BiCond<REC : Record<REC, *>>(
        private val left: WhereCondition<REC>,
        private val and: Boolean,
        private val right: WhereCondition<REC>
) : WhereCondition<REC> {

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

private val EmptyArray = emptyArray<Any>()

infix fun <REC : Record<REC, *>, T> Col<REC, T>.eq(value: T): WhereCondition<REC> =
        if (value == null) ColCond(this, " IS NULL", EmptyArray)
        else ColCond(this, " = ?", value as Any)

infix fun <REC : Record<REC, *>, T> Col<REC, T>.notEq(value: T): WhereCondition<REC> =
        if (value == null) ColCond(this, " IS NOT NULL", EmptyArray)
        else ColCond(this, " <> ?", value as Any)

infix fun <REC : Record<REC, *>, T : String?> Col<REC, T>.like(value: String): WhereCondition<REC> =
        ColCond(this, " LIKE ?", value)

infix fun <REC : Record<REC, *>, T : String?> Col<REC, T>.notLike(value: String): WhereCondition<REC> =
        ColCond(this, " NOT LIKE ?", value)

// let U be nullable, but not T
infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.between(range: Array<T>): WhereCondition<REC> =
        ColCond(this, " BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.notBetween(range: Array<T>): WhereCondition<REC> =
        ColCond(this, " NOT BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.isIn(values: Array<T>): WhereCondition<REC> =
        ColCond(this, StringBuilder(" IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.notIn(values: Array<T>): WhereCondition<REC> =
        ColCond(this, StringBuilder(" NOT IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.greaterThan(value: T): WhereCondition<REC> =
        ColCond(this, " > ?", value)

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.greaterOrEq(value: T): WhereCondition<REC> =
        ColCond(this, " >= ?", value)

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.lessThan(value: T): WhereCondition<REC> =
        ColCond(this, " < ?", value)

infix fun <REC : Record<REC, *>, T : Any, U : T> Col<REC, U>.lessOrEq(value: T): WhereCondition<REC> =
        ColCond(this, " <= ?", value)


infix fun <REC : Record<REC, *>> WhereCondition<REC>.and(that: WhereCondition<REC>): WhereCondition<REC> =
        BiCond(this, true, that)

infix fun <REC : Record<REC, *>> WhereCondition<REC>.or(that: WhereCondition<REC>): WhereCondition<REC> =
        BiCond(this, false, that)


inline operator fun <reified T> T.rangeTo(that: T): Array<T> = arrayOf(this, that)
