package net.aquadc.propertiesSampleLogic

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.type.string


object User : Schema<User>() {
    val Email = "email".mut(string, default = "john@riseup.net")
    val Name = "name".mut(string, default = "John")
    val Surname = "surname".mut(string, default = "Smith")
}
