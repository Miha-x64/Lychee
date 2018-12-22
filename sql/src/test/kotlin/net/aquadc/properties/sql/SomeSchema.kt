package net.aquadc.properties.sql

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.int
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.string


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b" let int
    val C = "c".mut(long, default = 100500)
}

val SomeTable = SimpleTable<SomeSchema, Long>(SomeSchema, "some_table", long, "_id")
