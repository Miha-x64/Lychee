package net.aquadc.properties.sql

import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.reallyEqual
import net.aquadc.persistence.struct.FieldDef
import net.aquadc.persistence.struct.Schema
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
     * Appends corresponding part of SQL query to [builder] using [dialect].
     */
    fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder

    /**
     * Appends contained colName-value-pairs to the given [colNames] and [colValues] lists.
     * [colValues] has non-nullable type because you can't treat ` = ?` as `IS NULL`.
     */
    fun appendValuesTo(colNames: ArrayList<String>, colValues: ArrayList<Any>)

    /**
     * Represents an absence of any conditions.
     */
    object Empty : WhereCondition<Nothing> {
        override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder = builder
        override fun appendValuesTo(colNames: ArrayList<String>, colValues: ArrayList<Any>) = Unit
    }

}

internal class ColCond<SCH : Schema<SCH>, T> : WhereCondition<SCH> {

    // mutable for internal code, he-he
    @JvmField @JvmSynthetic internal var colName: String
    private val op: CharSequence
    private val singleValue: Boolean
    @JvmField @JvmSynthetic internal var valueOrValues: Any // if (singleValue) Any else Array<Any>

    constructor(col: FieldDef<SCH, T>, op: CharSequence, value: Any) {
        this.colName = col.name
        this.op = op
        this.singleValue = true
        this.valueOrValues = value
    }

    constructor(col: FieldDef<SCH, T>, op: CharSequence, values: Array<Any>) {
        this.colName = col.name
        this.op = op
        this.singleValue = false
        this.valueOrValues = values
    }

    override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder =
            with(dialect) { builder.appendName(colName) }.append(op)

    override fun appendValuesTo(colNames: ArrayList<String>, colValues: ArrayList<Any>) {
        if (singleValue) {
            colNames.add(colName)
            colValues.add(valueOrValues)
        } else {
            (valueOrValues as Array<out Any>).forEach { value ->
                colNames.add(colName)
                colValues.add(value)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColCond<*, *>) return false

        if (colName != other.colName) return false
        if (op != other.op) return false
        if (singleValue != other.singleValue) return false
        if (!reallyEqual(valueOrValues, other.valueOrValues)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colName.hashCode()
        result = 31 * result + op.hashCode()
        result = 31 * result + singleValue.hashCode()
        result = 31 * result + valueOrValues.realHashCode()
        return result
    }

}

internal class BiCond<SCH : Schema<SCH>>(
        private val left: WhereCondition<SCH>,
        private val and: Boolean,
        private val right: WhereCondition<SCH>
) : WhereCondition<SCH> {

    override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder {
        builder.append('(')
        left.appendSqlTo(dialect, builder)
                .append(if (and) " AND " else " OR ")
        return right.appendSqlTo(dialect, builder)
                .append(')')
    }

    override fun appendValuesTo(colNames: ArrayList<String>, colValues: ArrayList<Any>) {
        left.appendValuesTo(colNames, colValues)
        right.appendValuesTo(colNames, colValues)
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

infix fun <SCH : Schema<SCH>, T> FieldDef<SCH, T>.eq(value: T): WhereCondition<SCH> =
        if (value == null) ColCond(this, " IS NULL", emptyArrayOf())
        else ColCond(this, " = ?", value as Any)

infix fun <SCH : Schema<SCH>, T> FieldDef<SCH, T>.notEq(value: T): WhereCondition<SCH> =
        if (value == null) ColCond(this, " IS NOT NULL", emptyArrayOf())
        else ColCond(this, " <> ?", value as Any)

infix fun <SCH : Schema<SCH>, T : String?> FieldDef<SCH, T>.like(value: String): WhereCondition<SCH> =
        ColCond(this, " LIKE ?", value)

infix fun <SCH : Schema<SCH>, T : String?> FieldDef<SCH, T>.notLike(value: String): WhereCondition<SCH> =
        ColCond(this, " NOT LIKE ?", value)

// let U be nullable, but not T
infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.between(range: Array<T>): WhereCondition<SCH> =
        ColCond(this, " BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.notBetween(range: Array<T>): WhereCondition<SCH> =
        ColCond(this, " NOT BETWEEN ? AND ?", range.also { check(it.size == 2) })

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.isIn(values: Array<T>): WhereCondition<SCH> =
        ColCond(this, StringBuilder(" IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.notIn(values: Array<T>): WhereCondition<SCH> =
        ColCond(this, StringBuilder(" NOT IN (").appendPlaceholders(values.size).append(')'), values)

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.greaterThan(value: T): WhereCondition<SCH> =
        ColCond(this, " > ?", value)

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.greaterOrEq(value: T): WhereCondition<SCH> =
        ColCond(this, " >= ?", value)

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.lessThan(value: T): WhereCondition<SCH> =
        ColCond(this, " < ?", value)

infix fun <SCH : Schema<SCH>, T : Any, U : T> FieldDef<SCH, U>.lessOrEq(value: T): WhereCondition<SCH> =
        ColCond(this, " <= ?", value)


infix fun <SCH : Schema<SCH>> WhereCondition<SCH>.and(that: WhereCondition<SCH>): WhereCondition<SCH> =
        BiCond(this, true, that)

infix fun <SCH : Schema<SCH>> WhereCondition<SCH>.or(that: WhereCondition<SCH>): WhereCondition<SCH> =
        BiCond(this, false, that)


/**
 * Builder for [between] and [notBetween]. E. g. `(SomeSchema.Field between lower..upper)`
 */
inline operator fun <reified T> T.rangeTo(that: T): Array<T> = arrayOf(this, that)
