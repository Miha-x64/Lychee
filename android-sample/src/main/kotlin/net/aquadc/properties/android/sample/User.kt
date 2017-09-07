package net.aquadc.properties.android.sample

import net.aquadc.properties.Property
import net.aquadc.properties.getValue

interface User {
    val email: String
    val name: String
    val surname: String
}

fun User.commonHashCode(): Int {
    var result = email.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + surname.hashCode()
    return result
}

fun User.commonEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is User) return false
    if (other.email != email) return false
    if (other.name != name) return false
    if (other.surname != surname) return false
    return true
}

class InMemoryUser(
        override val email: String,
        override val name: String,
        override val surname: String
) : User {
    override fun hashCode(): Int = commonHashCode()
    override fun equals(other: Any?): Boolean = commonEquals(other)
}

class OnScreenUser(
        emailProp: Property<String>,
        nameProp: Property<String>,
        surnameProp: Property<String>
) : User {
    override val email by emailProp
    override val name by nameProp
    override val surname by surnameProp

    override fun hashCode(): Int = commonHashCode()
    override fun equals(other: Any?): Boolean = commonEquals(other)

    fun snapshot() = InMemoryUser(email, name, surname)
}
