package net.aquadc.properties.testing

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.string


object Schema63 : Schema<Schema63>() {

    init {
        repeat(63) { t ->
            "field$t" let string
        }
    }

}
