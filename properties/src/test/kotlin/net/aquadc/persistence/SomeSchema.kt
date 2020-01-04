package net.aquadc.persistence

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.string


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b" let i32
    val C = "c" mut i64
}
