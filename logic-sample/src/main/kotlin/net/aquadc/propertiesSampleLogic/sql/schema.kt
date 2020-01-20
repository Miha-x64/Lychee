@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.*
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.build
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.nullable
import net.aquadc.persistence.type.string


val SampleTables: Array<Table<*, Long, *>> = arrayOf(Human.Tbl, Car.Tbl, Friendship.Tbl)


fun Transaction.insertHuman(name: String, surname: String): Human =
        insert(Human.Tbl, Human.build {
                it[Name] = name
                it[Surname] = surname
        })

class Human(session: Session<*>, id: Long) : Record<Human.Sch, Long>(Tbl, session, id) {

    val nameProp get() = this prop Name
    val surname get() = this[Surname]
    val carsProp = Car.OwnerId toMany Car.Tbl

    val friends = session[Friendship.Tbl]
            .select((Friendship.LeftId eq primaryKey) or (Friendship.RightId eq primaryKey))

    companion object Sch : Schema<Sch>() {
        val Name = "name" mut string
        val Surname = "surname" let string
    }
    object Tbl : Table<Sch, Long, Human>(Sch, "people", "_id", i64) {
        override fun newRecord(session: Session<*>, primaryKey: Long): Human = Human(session, primaryKey)
    }
}


fun Transaction.insertCar(owner: Human): Car =
        insert(Car.Tbl, Car.build {
            it[OwnerId] = owner.primaryKey
        })

class Car(session: Session<*>, id: Long) : Record<Car.Sch, Long>(Tbl, session, id) {

    val ownerProp = OwnerId toOne Human.Tbl
    val conditionerModelProp get() = this prop ConditionerModel

    companion object Sch : Schema<Sch>() {
        val OwnerId = "owner_id" mut i64
        val ConditionerModel = "conditioner_model".mut(nullable(string), default = null)
    }
    object Tbl : Table<Sch, Long, Car>(Sch, "cars", "_id", i64) {
        override fun newRecord(session: Session<*>, primaryKey: Long): Car = Car(session, primaryKey)
    }
}



fun Transaction.insertFriendship(left: Human, right: Human): Friendship =
        insert(Friendship.Tbl, Friendship.build {
            it[LeftId] = left.primaryKey
            it[RightId] = right.primaryKey
        })

class Friendship(session: Session<*>, id: Long) : Record<Friendship.Sch, Long>(Tbl, session, id) {

    val leftProp = LeftId toOne Human.Tbl
    val rightProp = RightId toOne Human.Tbl

    companion object Sch : Schema<Sch>() {
        val LeftId = "left" mut i64
        val RightId = "right" mut i64
    }
    object Tbl : Table<Sch, Long, Friendship>(Sch, "friends", "_id", i64) {
        override fun newRecord(session: Session<*>, primaryKey: Long): Friendship = Friendship(session, primaryKey)
    }
}
