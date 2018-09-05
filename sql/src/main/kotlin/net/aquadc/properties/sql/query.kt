package net.aquadc.properties.sql

import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.lang.StringBuilder
import java.sql.PreparedStatement


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
     * Binds contained values to a [statement].
     * @return number of values bound
     */
    fun bindValuesTo(statement: PreparedStatement, offset: Int): Int

    /**
     * Represents an absence of any conditions.
     */
    object Empty : WhereCondition<Nothing> {
        override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder = builder
        override fun bindValuesTo(statement: PreparedStatement, offset: Int): Int = 0
    }

}

internal class ColCond<REC : Record<REC, *>, T> : WhereCondition<REC> {

    // mutable for internal code, he-he
    @JvmField @JvmSynthetic internal var col: Col<REC, T>
    private val op: CharSequence
    private val singleValue: Boolean
    @JvmField @JvmSynthetic internal var valueOrValues: Any

    constructor(col: Col<REC, T>, op: CharSequence, value: Any) {
        this.col = col
        this.op = op
        this.singleValue = true
        this.valueOrValues = value
    }

    constructor(col: Col<REC, T>, op: CharSequence, values: Array<Any>) {
        this.col = col
        this.op = op
        this.singleValue = false
        this.valueOrValues = values
    }

    override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder =
            with(dialect) { builder.appendName(col.name) }.append(op)

    override fun bindValuesTo(statement: PreparedStatement, offset: Int): Int {
        val conv = col.converter
        return if (singleValue) {
            conv.bind(statement, offset, valueOrValues as T)
            1
        } else {
            val v = valueOrValues as Array<T>
            v.forEachIndexed { index, value ->
                conv.bind(statement, offset + index, value)
            }
            v.size
        }
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

    override fun bindValuesTo(statement: PreparedStatement, offset: Int): Int {
        val movedBy = left.bindValuesTo(statement, offset)
        return movedBy + right.bindValuesTo(statement, offset + movedBy)
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
