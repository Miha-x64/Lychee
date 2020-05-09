@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.tableOf
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string


val SampleTables: Array<Table<*, Long>> = arrayOf(Human.Tbl, Car.Tbl, Friendship.Tbl)


object Human : Schema<Human>() {
    val Name = "name" mut string
    val Surname = "surname" let string

    val Tbl = tableOf(this, "people", "_id", i64)
}
fun Human(name: String, surname: String) = Human {
    it[Name] = name
    it[Surname] = surname
}


object Car : Schema<Car>() {
    val OwnerId = "owner_id" mut i64
    val ConditionerModel = "conditioner_model".mut(nullable(string), default = null)

    val Tbl = tableOf(this, "cars", "_id", i64)
}
fun Car(ownerId: Long) = Car {
    it[OwnerId] = ownerId
}


object Friendship : Schema<Friendship>() {
    val LeftId = "left" mut i64
    val RightId = "right" mut i64

    val Tbl = tableOf(this, "friends", "_id", i64)
}
fun Friendship(leftId: Long, rightId: Long) = Friendship {
    it[LeftId] = leftId
    it[RightId] = rightId
}
