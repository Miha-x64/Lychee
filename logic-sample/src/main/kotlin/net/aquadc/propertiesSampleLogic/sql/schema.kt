@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.properties.sql.*
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullableString
import net.aquadc.persistence.type.string


val Tables = arrayOf(Human, Car, Friendship)


fun Transaction.insertHuman(name: String, surname: String): Human =
        session[Human].require(
                insert(Human, Human.Name - name, Human.Surname - surname)
        )

class Human(session: Session, id: Long) : Record<Human.Schema, Long>(Human.Schema, session, id) {
    val nameProp get() = this[Name]
    val surnameProp get() = this[Surname]
    val carsProp = Car.OwnerId toMany Car

    val friends = session[Friendship]
            .select((Friendship.LeftId eq id) or (Friendship.RightId eq id))

    companion object Schema : Table<Schema, Long, Human>("people", long, "_id") {
        val Name = "name" let string
        val Surname = "surname" let string

        override fun create(session: Session, id: Long): Human = Human(session, id)
    }
}



class Car(session: Session, id: Long) : Record<Car.Schema, Long>(Schema, session, id) {
    val ownerProp = Schema.OwnerId toOne Human
    val conditionerModelProp get() = this[Schema.ConditionerModel]

    companion object Schema : Table<Schema, Long, Car>("cars", long, "_id") {
        val OwnerId = "owner_id" let long
        val ConditionerModel = "conditioner_model" let nullableString

        override fun create(session: Session, id: Long): Car = Car(session, id)
    }
}
fun Transaction.insertCar(owner: Human): Car =
        session[Car].require(insert(Car, Car.OwnerId - owner.primaryKey))



class Friendship(session: Session, id: Long) : Record<Friendship.Schema, Long>(Friendship, session, id) {
    val leftProp = LeftId toOne Human
    val rightProp = RightId toOne Human

    companion object Schema : Table<Schema, Long, Friendship>("friends", long, "_id") {
        val LeftId = "left" let long
        val RightId = "right" let long

        override fun create(session: Session, id: Long): Friendship = Friendship(session, id)
    }
}
