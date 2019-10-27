package net.aquadc.properties.sql

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.reallyEqual
import net.aquadc.persistence.struct.Lens
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.properties.internal.emptyArrayOf
import net.aquadc.properties.sql.dialect.Dialect
import net.aquadc.properties.sql.dialect.appendPlaceholders
import java.lang.Math.max
import java.lang.Math.min


/**
 * A condition for record of type [SCH].
 * API is mostly borrowed from
 * https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java
 */
interface WhereCondition<SCH : Schema<SCH>> {

    /**
     * Number of columns and values to substitute
     */
    val size: Int

    /**
     * Appends corresponding part of SQL query to [builder] using [dialect].
     */
    fun appendSqlTo(context: Table<SCH, *, *>, dialect: Dialect, builder: StringBuilder): StringBuilder

    /**
     * Appends contained colName-value-pairs to the given [outCols] and [outColValues] lists.
     * [outColValues] has non-nullable type because you can't treat ` = ?` as `IS NULL`.
     */
    fun setValuesTo(offset: Int, outCols: Array<in StoredLens<SCH, *, *>>, outColValues: Array<in Any>)

    @Deprecated("replaced with a function", ReplaceWith("emptyCondition()"), DeprecationLevel.ERROR)
    object Empty

}

/**
 * Represents an absence of any conditions.
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE") // special, empty implementation
inline fun <SCH : Schema<SCH>> emptyCondition(): WhereCondition<SCH> =
        EmptyCondition as WhereCondition<SCH>

@PublishedApi internal object EmptyCondition : WhereCondition<Nothing> {
    override val size: Int get() = 0
    override fun appendSqlTo(context: Table<Nothing, *, *>, dialect: Dialect, builder: StringBuilder): StringBuilder = builder
    override fun setValuesTo(offset: Int, outCols: Array<in StoredLens<Nothing, *, *>>, outColValues: Array<in Any>) {}
}


internal class ColCond<SCH : Schema<SCH>, T> : WhereCondition<SCH> {

    // mutable for internal code, he-he
    @JvmField @JvmSynthetic internal var lens: StoredLens<SCH, T, *>
    private val op: CharSequence
    private val singleValue: Boolean
    @JvmField @JvmSynthetic internal var valueOrValues: Any // if (singleValue) Any else Array<Any>

    constructor(lens: StoredLens<SCH, T, *>, op: CharSequence, value: Any) {
        this.lens = lens
        this.op = op
        this.singleValue = true
        this.valueOrValues = value
    }

    constructor(lens: StoredLens<SCH, T, *>, op: CharSequence, values: Array<out Any>) {
        this.lens = lens
        this.op = op
        this.singleValue = false
        this.valueOrValues = values
    }

    override val size: Int
        get() = if (singleValue) 1 else (valueOrValues as Array<*>).size

    override fun appendSqlTo(context: Table<SCH, *, *>, dialect: Dialect, builder: StringBuilder): StringBuilder =
            with(dialect) { builder.appendName(context.columnByLens(lens)!!.name) }.append(op)

    override fun setValuesTo(offset: Int, outCols: Array<in StoredLens<SCH, *, *>>, outColValues: Array<in Any>) {
        if (singleValue) {
            outCols[offset] = lens
            outColValues[offset] = valueOrValues
        } else {
            (valueOrValues as Array<out Any>).forEachIndexed { i, value ->
                val idx = offset + i
                outCols[idx] = lens
                outColValues[idx] = value
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColCond<*, *>) return false

        if (lens != other.lens) return false
        if (op != other.op) return false
        if (singleValue != other.singleValue) return false
        if (!reallyEqual(valueOrValues, other.valueOrValues)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lens.hashCode()
        result = 31 * result + op.hashCode()
        result = 31 * result + valueOrValues.realHashCode()
        return result
    }

}

infix fun <SCH : Schema<SCH>, T> StoredLens<SCH, T, *>.eq(value: T): WhereCondition<SCH> =
        if (value == null) ColCond(this, " IS NULL", emptyArrayOf())
        else ColCond(this, " = ?", value as Any)

infix fun <SCH : Schema<SCH>, T> StoredLens<SCH, T, *>.notEq(value: T): WhereCondition<SCH> =
        if (value == null) ColCond(this, " IS NOT NULL", emptyArrayOf())
        else ColCond(this, " <> ?", value as Any)

infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.like(value: String): WhereCondition<SCH> =
        ColCond(this, " LIKE ?", value)

infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.notLike(value: String): WhereCondition<SCH> =
        ColCond(this, " NOT LIKE ?", value)

infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.startsWith(value: String): WhereCondition<SCH> =
        ColCond(this, " LIKE (? || '%')", value)

// fun doesNotStartWith? startsWithout?

infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.endsWith(value: String): WhereCondition<SCH> =
        ColCond(this, " LIKE ('%' || ?)", value)

// fun doesNotEndWith? endsWithout?

infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.contains(value: String): WhereCondition<SCH> =
        ColCond(this, " LIKE ('%' || ? || '%')", value)

// fun doesNotContain? notContains?

// `out T?`: allow lenses to look at nullable types

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.between(range: Array<T>): WhereCondition<SCH> =
        ColCond(this, " BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.notBetween(range: Array<T>): WhereCondition<SCH> =
        ColCond(this, " NOT BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <SCH : Schema<SCH>, T : Comparable<T>> StoredLens<SCH, out T?, *>.between(range: ClosedRange<T>): WhereCondition<SCH> =
        ColCond(this, " BETWEEN ? AND ?", arrayOf<Any>(range.start, range.endInclusive))

infix fun <SCH : Schema<SCH>, T : Comparable<T>> StoredLens<SCH, out T?, *>.notBetween(range: ClosedRange<T>): WhereCondition<SCH> =
        ColCond(this, " NOT BETWEEN ? AND ?", arrayOf<Any>(range.start, range.endInclusive))

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.isIn(values: Array<T>): WhereCondition<SCH> =
        ColCond(this, StringBuilder(" IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.notIn(values: Array<T>): WhereCondition<SCH> =
        ColCond(this, StringBuilder(" NOT IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.greaterThan(value: T): WhereCondition<SCH> =
        ColCond(this, " > ?", value)

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.greaterOrEq(value: T): WhereCondition<SCH> =
        ColCond(this, " >= ?", value)

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.lessThan(value: T): WhereCondition<SCH> =
        ColCond(this, " < ?", value)

infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T, *>.lessOrEq(value: T): WhereCondition<SCH> =
        ColCond(this, " <= ?", value)


internal class BiCond<SCH : Schema<SCH>>(
        private val left: WhereCondition<SCH>,
        private val and: Boolean,
        private val right: WhereCondition<SCH>
) : WhereCondition<SCH> {

    override val size: Int
        get() = left.size + right.size

    override fun appendSqlTo(context: Table<SCH, *, *>, dialect: Dialect, builder: StringBuilder): StringBuilder {
        builder.append('(')
        left.appendSqlTo(context, dialect, builder)
                .append(if (and) " AND " else " OR ")
        return right.appendSqlTo(context, dialect, builder)
                .append(')')
    }

    override fun setValuesTo(offset: Int, outCols: Array<in StoredLens<SCH, *, *>>, outColValues: Array<in Any>) {
        left.setValuesTo(offset, outCols, outColValues)
        right.setValuesTo(offset + left.size, outCols, outColValues)
    }

    override fun hashCode(): Int {
        val lh = left.hashCode()
        val rh = right.hashCode()
        val low = 31 * min(lh, rh)
        val hi = max(lh, rh)
        return if (and) low and hi else low or hi
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BiCond<*>) return false

        if (other.and != and) return false
        if (other.left == left && other.right == right) return true
        if (other.left == right && other.right == left) return true

        return false
    }

}

infix fun <SCH : Schema<SCH>> WhereCondition<SCH>.and(that: WhereCondition<SCH>): WhereCondition<SCH> =
        BiCond(this, true, that)

infix fun <SCH : Schema<SCH>> WhereCondition<SCH>.or(that: WhereCondition<SCH>): WhereCondition<SCH> =
        BiCond(this, false, that)


/**
 * Builder for [between] and [notBetween]. E. g. `(SomeSchema.Field between lower..upper)`
 */
inline operator fun <reified T> T.rangeTo(that: T): Array<T> = arrayOf(this, that)
