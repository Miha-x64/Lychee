package net.aquadc.persistence.struct

import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.string


object SomeSchema : Schema<SomeSchema>() {
    val A = "a" let string
    val B = "b".mut(i32, 10)
    val C = "c" mut i64
    val D = "d".let(i64, 92L)
}
