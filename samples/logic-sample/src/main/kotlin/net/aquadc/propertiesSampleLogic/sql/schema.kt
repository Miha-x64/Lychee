@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.extended.Partial
import net.aquadc.persistence.sql.Table
import net.aquadc.persistence.sql.tableOf
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string


val SampleTables: Array<Table<*, Long>> = arrayOf(Human.Tbl, Car.Tbl, Friendship.Tbl)


object Human : Schema<Human>() {
    val Id = "_id" let i64
    val Name = "name" mut string
    val Surname = "surname" let string

    val Tbl = tableOf(this, "people", Id)
}
fun Human(id: Long, name: String, surname: String) = Human {
    it[Id] = id
    it[Name] = name
    it[Surname] = surname
}
fun Human(name: String, surname: String) = Human.Partial {
    it[Name] = name
    it[Surname] = surname
}


object Car : Schema<Car>() {
    val Id = "_id" let i64
    val OwnerId = "owner_id" mut i64
    val ConditionerModel = "conditioner_model".mut(nullable(string), default = null)

    val Tbl = tableOf(this, "cars", Id)
}
fun Car(id: Long, ownerId: Long, conditionerModel: String? = null) = Car {
    it[Id] = id
    it[OwnerId] = ownerId
    it[ConditionerModel] = conditionerModel
}
fun Car(ownerId: Long, conditionerModel: String? = null) = Car.Partial {
    it[OwnerId] = ownerId
    it[ConditionerModel] = conditionerModel
}


object Friendship : Schema<Friendship>() {
    val Id = "_id" let i64
    val LeftId = "left" mut i64
    val RightId = "right" mut i64

    val Tbl = tableOf(this, "friends", Id)
}
fun Friendship(id: Long, leftId: Long, rightId: Long) = Friendship {
    it[Id] = id
    it[LeftId] = leftId
    it[RightId] = rightId
}
fun Friendship(leftId: Long, rightId: Long) = Friendship.Partial {
    it[LeftId] = leftId
    it[RightId] = rightId
}
