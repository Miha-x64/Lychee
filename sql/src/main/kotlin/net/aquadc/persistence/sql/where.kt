@file:JvmName("Where")
package net.aquadc.persistence.sql

import androidx.annotation.Size
import net.aquadc.persistence.realHashCode
import net.aquadc.persistence.reallyEqual
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.StoredLens
import net.aquadc.properties.internal.emptyArrayOf
import net.aquadc.persistence.sql.dialect.Dialect
import net.aquadc.persistence.sql.dialect.appendPlaceholders
import java.lang.Math.max
import java.lang.Math.min


/**
 * A condition for record of type [SCH].
 * API is mostly borrowed from
 * https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java
 */
@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
typealias WhereCondition<SCH> = Nothing

/**
 * Represents an absence of any conditions.
 */
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE") // special, empty implementation
@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
inline fun <SCH : Schema<SCH>> emptyCondition(): Nothing =
    throw AssertionError()


@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T> StoredLens<SCH, T, *>.eq(value: T): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T> StoredLens<SCH, T, *>.notEq(value: T): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.like(value: String): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.notLike(value: String): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.startsWith(value: String): Nothing =
    throw AssertionError()

// fun doesNotStartWith? startsWithout?

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.endsWith(value: String): Nothing =
    throw AssertionError()

// fun doesNotEndWith? endsWithout?

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : String?> StoredLens<SCH, T, *>.contains(value: String): Nothing =
    throw AssertionError()

// fun doesNotContain? notContains?

// `out T?`: allow lenses to look at nullable types

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.between(@Size(2) range: Array<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.notBetween(@Size(2) range: Array<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Comparable<T>> StoredLens<SCH, out T?, *>.between(range: ClosedRange<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Comparable<T>> StoredLens<SCH, out T?, *>.notBetween(range: ClosedRange<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.isIn(values: Array<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.notIn(values: Array<T>): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.greaterThan(value: T): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.greaterOrEq(value: T): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T?, *>.lessThan(value: T): Nothing =
    throw AssertionError()

@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
infix fun <SCH : Schema<SCH>, T : Any> StoredLens<SCH, out T, *>.lessOrEq(value: T): Nothing =
    throw AssertionError()


/**
 * Builder for [between] and [notBetween]. E. g. `(SomeSchema.Field between lower..upper)`
 */
@JvmSynthetic // rangeTo(from, to) is useless for Java
@Deprecated("The query builder is poor, use SQL templates (session.query()=>function) instead.", level = DeprecationLevel.ERROR)
inline operator fun <reified T> T.rangeTo(that: T): Array<T> = throw AssertionError()
