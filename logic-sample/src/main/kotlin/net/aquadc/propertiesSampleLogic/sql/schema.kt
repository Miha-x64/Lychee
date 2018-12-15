@file:Suppress("KDocMissingDocumentation")

package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.invoke
import net.aquadc.properties.sql.*
import net.aquadc.persistence.type.long
import net.aquadc.persistence.type.nullableString
import net.aquadc.persistence.type.string


val Tables: Array<Table<*, Long, *>> = arrayOf(Human.Tbl, Car.Tbl, Friendship.Tbl)


fun Transaction.insertHuman(name: String, surname: String): Human =
        Human(this, Human {
                it[Human.Name] = name
                it[Human.Surname] = surname
        })

class Human : Record<Human.Sch, Long> {

    constructor(session: Session, id: Long) : super(Human.Tbl, session, id)
    constructor(transaction: Transaction, source: Struct<Human.Sch>) : super(Human.Tbl, transaction, source)

    val nameProp get() = this prop Name
    val surname get() = this[Surname]
    val carsProp = Car.OwnerId toMany Car.Tbl

    val friends = session[Friendship.Tbl]
            .select((Friendship.LeftId eq primaryKey) or (Friendship.RightId eq primaryKey))

    companion object Sch : Schema<Sch>() {
        val Name = "name" mut string
        val Surname = "surname" let string
    }
    object Tbl : Table<Sch, Long, Human>(Sch, "people", long, "_id") {
        override fun newRecord(session: Session, primaryKey: Long): Human = Human(session, primaryKey)
    }
}


fun Transaction.insertCar(owner: Human): Car =
        Car(this, Car {
            it[Car.OwnerId] = owner.primaryKey
        })

class Car : Record<Car.Sch, Long> {

    constructor(session: Session, id: Long) : super(Tbl, session, id)
    constructor(transaction: Transaction, source: Struct<Sch>) : super(Tbl, transaction, source)

    val ownerProp = OwnerId toOne Human.Tbl
    val conditionerModelProp get() = this prop ConditionerModel

    companion object Sch : Schema<Sch>() {
        val OwnerId = "owner_id" mut long
        val ConditionerModel = "conditioner_model".mut(nullableString, default = null)
    }
    object Tbl : Table<Sch, Long, Car>(Sch, "cars", long, "_id") {
        override fun newRecord(session: Session, primaryKey: Long): Car = Car(session, primaryKey)
    }
}



fun Transaction.insertFriendship(left: Human, right: Human): Friendship =
        Friendship(this, Friendship {
            it[Friendship.LeftId] = left.primaryKey
            it[Friendship.RightId] = right.primaryKey
        })

class Friendship : Record<Friendship.Sch, Long> {

    constructor(session: Session, id: Long) : super(Tbl, session, id)
    constructor(transaction: Transaction, source: Struct<Sch>) : super(Tbl, transaction, source)

    val leftProp = LeftId toOne Human.Tbl
    val rightProp = RightId toOne Human.Tbl

    companion object Sch : Schema<Sch>() {
        val LeftId = "left" mut long
        val RightId = "right" mut long
    }
    object Tbl : Table<Sch, Long, Friendship>(Sch, "friends", long, "_id") {
        override fun newRecord(session: Session, primaryKey: Long): Friendship = Friendship(session, primaryKey)
    }
}
