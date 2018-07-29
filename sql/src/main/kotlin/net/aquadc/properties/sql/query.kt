package net.aquadc.properties.sql
// @see package org.greenrobot.greendao.query;

import java.lang.StringBuilder
import java.util.*


interface WhereCondition<REC : Record<REC, *>> {

    fun appendTo(builder: StringBuilder): StringBuilder

    fun appendValuesTo(target: MutableList<Any>)


    class ColCond<REC : Record<REC, *>, T> : WhereCondition<REC> {

        private val col: Col<REC, T>
        private val op: String
        private val singleValue: Boolean
        private val valueOrValues: Any

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

        override fun appendTo(builder: StringBuilder): StringBuilder = // TODO: dialect support
                builder.append(col.name.replace("\"", "\"\"")).append(op)

        override fun appendValuesTo(target: MutableList<Any>) {
            if (singleValue) target.add(valueOrValues)
            else Collections.addAll(target, valueOrValues as Array<*>)
        }

    }

    object Empty : WhereCondition<Nothing> {
        override fun appendTo(builder: StringBuilder): StringBuilder = builder
        override fun appendValuesTo(target: MutableList<Any>) = Unit
    }

    // TODO: composite conditions

}

infix fun <REC : Record<REC, *>, T> Col<REC, T>.eq(value: T): WhereCondition<REC> =
        if (value == null) WhereCondition.ColCond(this, " IS NULL")
        else WhereCondition.ColCond<REC, T>(this, " = ?", value)

// TODO: more
