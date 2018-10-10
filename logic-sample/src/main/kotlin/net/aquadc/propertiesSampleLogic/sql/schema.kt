@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.properties.sql.*
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullableString
import net.aquadc.persistence.type.string


val Tables = arrayOf(HumanTable, CarTable, FriendTable)

object HumanTable : Table<HumanTable, Long, Human>("people", long, "_id") {
    val Name = string immutable "name"
    val Surname = string immutable "surname"

    override fun create(session: Session, id: Long): Human = Human(session, id)
}
fun Transaction.insertHuman(name: String, surname: String): Human =
        session[HumanTable].require(
                insert(HumanTable, HumanTable.Name - name, HumanTable.Surname - surname)
        )

class Human(session: Session, id: Long) : Record<HumanTable, Long>(HumanTable, session, id) {
    val nameProp get() = this[HumanTable.Name]
    val surnameProp get() = this[HumanTable.Surname]
    val carsProp = CarTable.OwnerId toMany CarTable

    val friends = session[FriendTable]
            .select((FriendTable.LeftId eq id) or (FriendTable.RightId eq id))
}



object CarTable : Table<CarTable, Long, Car>("cars", long, "_id") {
    val OwnerId = long immutable "owner_id"
    val ConditionerModel = nullableString immutable "conditioner_model"

    override fun create(session: Session, id: Long): Car = Car(session, id)
}

class Car(session: Session, id: Long) : Record<CarTable, Long>(CarTable, session, id) {
    val ownerProp = CarTable.OwnerId toOne HumanTable
    val conditionerModelProp get() = this[CarTable.ConditionerModel]
}
fun Transaction.insertCar(owner: Human): Car =
        session[CarTable].require(insert(CarTable, CarTable.OwnerId - owner.primaryKey))


object FriendTable : Table<FriendTable, Long, Friendship>("friends", long, "_id") {
    val LeftId = long immutable "left"
    val RightId = long immutable "right"

    override fun create(session: Session, id: Long): Friendship = Friendship(session, id)
}

class Friendship(session: Session, id: Long) : Record<FriendTable, Long>(FriendTable, session, id) {
    val leftProp = FriendTable.LeftId toOne HumanTable
    val rightProp = FriendTable.RightId toOne HumanTable
}
