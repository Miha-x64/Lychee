package net.aquadc.properties.sample.logic


data class User(
        val email: String,
        val name: String,
        val surname: String
)

val defaultUser = User(
        email = "john@riseup.net",
        name = "John",
        surname = "Smith"
)
