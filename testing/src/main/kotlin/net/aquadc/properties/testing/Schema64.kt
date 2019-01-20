package net.aquadc.properties.testing

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.string


object Schema64 : Schema<Schema64>() {

    init {
        repeat(64) { t ->
            "field$t" let string
        }
    }

}
