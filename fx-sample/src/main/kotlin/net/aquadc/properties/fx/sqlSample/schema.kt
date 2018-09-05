@file:Suppress("KDocMissingDocumentation")

package net.aquadc.properties.fx.sqlSample

import net.aquadc.properties.mapWith
import net.aquadc.properties.sql.*
import net.aquadc.properties.sql.dialect.sqlite.long
import net.aquadc.properties.sql.dialect.sqlite.nullableString
import net.aquadc.properties.sql.dialect.sqlite.string


val Tables = arrayOf(HumanTable, CarTable, FriendTable)

object HumanTable : Table<Human, Long>("people", t()) {
    val Id = long idCol "_id"
    val Name = string col "name"
    val Surname = string col "surname"

    override fun create(session: Session, id: Long): Human = Human(session, id)
}
fun Transaction.insertHuman(name: String, surname: String): Human =
        session[HumanTable].require(
                insert(HumanTable, HumanTable.Name - name, HumanTable.Surname - surname)
        )

class Human(session: Session, id: Long) : Record<Human, Long>(HumanTable, session, id) {
    val nameProp get() = this[HumanTable.Name]
    val surnameProp get() = this[HumanTable.Surname]
    val carsProp = CarTable.OwnerId toMany CarTable

    val friends = session[FriendTable]
            .select((FriendTable.LeftId eq id) or (FriendTable.RightId eq id))
}



object CarTable : Table<Car, Long>("cars", t()) {
    val Id = long idCol "_id"
    val OwnerId = long col "owner_id"
    val ConditionerModel = nullableString col "conditioner_model"

    override fun create(session: Session, id: Long): Car = Car(session, id)
}

class Car(session: Session, id: Long) : Record<Car, Long>(CarTable, session, id) {
    val ownerProp = CarTable.OwnerId toOne HumanTable
    val conditionerModelProp get() = this[CarTable.ConditionerModel]
}
fun Transaction.insertCar(owner: Human): Car =
        session[CarTable].require(insert(CarTable, CarTable.OwnerId - owner.primaryKey))


object FriendTable : Table<Friendship, Long>("friends", t()) {
    val Id = long idCol "_id"
    val LeftId = long col "left"
    val RightId = long col "right"

    override fun create(session: Session, id: Long): Friendship = Friendship(session, id)
}

class Friendship(session: Session, id: Long) : Record<Friendship, Long>(FriendTable, session, id) {
    val leftProp = FriendTable.LeftId toOne HumanTable
    val rightProp = FriendTable.RightId toOne HumanTable
}
