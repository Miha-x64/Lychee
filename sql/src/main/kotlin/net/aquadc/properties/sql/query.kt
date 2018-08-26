package net.aquadc.properties.sql
// @see package org.greenrobot.greendao.query;

import net.aquadc.properties.sql.dialect.Dialect
import java.lang.StringBuilder
import java.sql.PreparedStatement


interface WhereCondition<REC : Record<REC, *>> {

    fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder

    fun bindValuesTo(statement: PreparedStatement, offset: Int)


    class ColCond<REC : Record<REC, *>, T> : WhereCondition<REC> {

        @JvmField @JvmSynthetic internal var col: Col<REC, T>
        private val op: String
        private val singleValue: Boolean
        @JvmField @JvmSynthetic internal var valueOrValues: Any

        constructor(col: Col<REC, T>, op: String, value: Any) {
            this.col = col
            this.op = op
            this.singleValue = true
            this.valueOrValues = value
        }

        constructor(col: Col<REC, T>, op: String, vararg values: Any) {
            this.col = col
            this.op = op
            this.singleValue = false
            this.valueOrValues = values
        }

        override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder =
                with(dialect) { builder.appendName(col.name) }.append(op)

        override fun bindValuesTo(statement: PreparedStatement, offset: Int) {
            val conv = col.converter
            if (singleValue) conv.bind(statement, offset, valueOrValues as T)
            else (valueOrValues as Array<T>).forEachIndexed { index, value ->
                conv.bind(statement, offset + index, value)
            }
        }

    }

    object Empty : WhereCondition<Nothing> {
        override fun appendSqlTo(dialect: Dialect, builder: StringBuilder): StringBuilder = builder
        override fun bindValuesTo(statement: PreparedStatement, offset: Int) = Unit
    }

    // TODO: composite conditions

}

infix fun <REC : Record<REC, *>, T> Col<REC, T>.eq(value: T): WhereCondition<REC> =
        if (value == null) WhereCondition.ColCond(this, " IS NULL")
        else WhereCondition.ColCond<REC, T>(this, " = ?", value)

// TODO: more
