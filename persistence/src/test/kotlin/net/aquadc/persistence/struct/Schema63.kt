package net.aquadc.persistence.struct

import net.aquadc.persistence.type.string


object Schema63 : Schema<Schema63>() {

    init {
        repeat(63) { t ->
            "field$t" let string
        }
    }

}
