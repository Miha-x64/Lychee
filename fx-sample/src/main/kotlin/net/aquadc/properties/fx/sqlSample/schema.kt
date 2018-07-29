@file:Suppress("KDocMissingDocumentation")

package net.aquadc.properties.fx.sqlSample

import net.aquadc.properties.mapWith
import net.aquadc.properties.sql.*


val Tables = arrayOf(HumanTable, CarTable, FriendTable)

object HumanTable : Table<Human, Long>("people", t()) {
    val Id = idCol("_id")
    val Name = col<String>("name")
    val Surname = col<String>("surname")
    val SpouseId = nullableCol<Long>("spouse_id")

    override fun create(session: Session, id: Long): Human = Human(session, id)
}
fun Transaction.insertHuman(name: String, surname: String, spouseId: Long?): Human =
        session.require(
                HumanTable,
                insert(HumanTable, HumanTable.Name - name, HumanTable.Surname - surname, HumanTable.SpouseId - spouseId)
        )

class Human(session: Session, id: Long) : Record<Human, Long>(HumanTable, session, id) {
    val name = HumanTable.Name()
    val surname = HumanTable.Surname()
    val spouse = HumanTable.SpouseId toOneNullable HumanTable
    val cars = CarTable.OwnerId toMany CarTable

    private val leftFriends = FriendTable.RightId toMany FriendTable
    private val rightFriends = FriendTable.LeftId toMany FriendTable
    val friends = leftFriends.mapWith(rightFriends) { l, r ->
        l.map(Friendship::left) + r.map(Friendship::right)
    } // TODO: a query like `SELECT * FROM friends WHERE left = ? OR right = ?`
}



object CarTable : Table<Car, Long>("cars", t()) {
    val Id = idCol("_id")
    val OwnerId = col<Long>("owner_id")
    val ConditionerModel = nullableCol<String?>("conditioner_model")

    override fun create(session: Session, id: Long): Car = Car(session, id)
}

class Car(session: Session, id: Long) : Record<Car, Long>(CarTable, session, id) {
    val owner = CarTable.OwnerId toOne HumanTable
}



object FriendTable : Table<Friendship, Long>("friends", t()) {
    val Id = idCol("_id")
    val LeftId = col<Long>("left")
    val RightId = col<Long>("right")

    override fun create(session: Session, id: Long): Friendship = Friendship(session, id)
}

class Friendship(session: Session, id: Long) : Record<Friendship, Long>(FriendTable, session, id) {
    val left = FriendTable.LeftId toOne HumanTable
    val right = FriendTable.RightId toOne HumanTable
}
