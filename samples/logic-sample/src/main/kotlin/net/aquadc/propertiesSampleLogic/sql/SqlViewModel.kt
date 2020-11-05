package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.*
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.template.Mutation
import net.aquadc.persistence.sql.template.Query
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.string
import net.aquadc.properties.*
import net.aquadc.properties.function.Objectz
import java.io.Closeable


class SqlViewModel<CUR>(
    private val session: Session<Blocking<CUR>>
): Closeable {

    private val count: Session<Blocking<CUR>>.() -> Int =
        Query("SELECT COUNT(*) FROM ${Human.Tbl.name}", Eagerly.cell<CUR, Int>(i32))
    private val fetch: Session<Blocking<CUR>>.() -> List<Struct<Human>> =
        Query("SELECT name, surname FROM ${Human.Tbl.name}", Eagerly.structs<CUR, Human>(Human.Tbl, BindBy.Name))
    private val _find: Session<Blocking<CUR>>.(Long) -> Struct<Human> =
        Query("SELECT name, surname FROM ${Human.Tbl.name} WHERE _id = ?", i64, Eagerly.struct<CUR, Human>(Human.Tbl, BindBy.Name))
    private val find: (Long) -> Struct<Human>? =
        { id -> try { session._find(id) } catch (e: NoSuchElementException) { null } } // FIXME

    private val updateName: Transaction<Blocking<CUR>>.(String, Long) -> Unit =
        Mutation<Blocking<CUR>, String, Long, Unit>("UPDATE ${Human.Tbl.name} SET ${Human.run { Name.name }} = ? WHERE _id = ?", string, i64, Eagerly.execute())

    private val carsWithConditionersByOwner: Session<Blocking<CUR>>.(Long) -> List<Struct<Car>> =
        Query("SELECT * FROM ${Car.Tbl.name} WHERE ${Car.run { OwnerId.name }} = ? AND ${Car.run { ConditionerModel.name }} IS NOT NULL",
            i64,
            Eagerly.structs<CUR, Car>(Car.Tbl, BindBy.Name))

    val titleProp: Property<String>
    val humanListProp: Property<List<Struct<Human>>>
    val selectedProp = propertyOf<Struct<Human>?>(null)
    val disposeMePlz: Closeable

    init {
        fillIfEmpty()
        val countProp = propertyOf(session.count())
        titleProp = countProp.map { "Sample SQLite application ($it records)" }
        humanListProp = propertyOf(session.fetch())
        disposeMePlz = session.observe(
            Human.Tbl to TriggerEvent.INSERT, Human.Tbl to TriggerEvent.UPDATE, Human.Tbl to TriggerEvent.DELETE) { report ->
            val chg = report.of(Human.Tbl)
            // todo val (ins, upd, del) = report.of(…)
            countProp.value += chg.inserted.size - chg.removed.size
            humanListProp.value =
                /*if (chg.inserted.isEmpty() && chg.updated.isEmpty())
                    humanListProp.value.filter { it.primaryKey !in chg.removed }
                else*/
                    session.fetch()
        }
    }

    private val namePatch = propertyOf(mapOf<@ParameterName("humanId") Long, String>()).also {
        it.debounced(1000L).onEach { new ->
            if (new.isNotEmpty() && it.casValue(new, emptyMap())) {
                session.withTransaction {
                    new.forEach { (humanId, newName) ->
                        find(humanId) // if it was just deleted, ignore
                            ?.takeIf { it[Human.Name] != newName }
                            ?.let { updateName(newName, humanId) }
                    }
                }
            }
        }
    }
    val actionsEnabledProp = selectedProp.map(Objectz.IsNotNull)

    /*val airConditionersTextProp = selectedProp
            .map {
                val cars = if (it == null) emptyList() else carsWithConditionersByOwner(it.primaryKey)
                if (cars.isEmpty()) "none"
                else
                    cars.map { it[Car.ConditionerModel]!! }
                        .joinToString(prefix = "Air conditioner(s) in car(s): [", postfix = "]")
            }*/

    val nameProp = selectedProp.map { it?.get(Human.Name) } // TODO mapNotNull?
    /*val editableNameProp = propertyOf("")
        .onEach { newText ->
            selectedProp.value
                ?.let { namePatch += it.primaryKey to newText }
        }*/

    val lastInserted = propertyOf<Struct<Human>?>(null)

    val createClicked = propertyOf(false)
        .clearEachAndTransact {
            lastInserted.value = find(insert(Human.Tbl, Human("<new>", "")))!!
        }

    /*val deleteClicked = propertyOf(false)
        .clearEachAndTransact {
            delete(Human.Tbl, selectedProp.value!!.primaryKey)
        }*/

    val truncateClicked = propertyOf(false)
        .clearEachAndTransact {
            truncate(Human.Tbl)
        }

    private fun <P : MutableProperty<Boolean>> P.clearEachAndTransact(func: Transaction<Blocking<CUR>>.() -> Unit): P =
        clearEachAnd { session.withTransaction { func() } }

    private fun fillIfEmpty() {
        if (session.count() == 0) {
            session.withTransaction {
                insert(Human.Tbl, Human("Stephen", "Hawking"))
                val relativist = insert(Human.Tbl, Human("Albert", "Einstein"))
                insert(Human.Tbl, Human("Dmitri", "Mendeleev"))
                val electrician = insert(Human.Tbl, Human("Nikola", "Tesla"))

                // don't know anything about their friendship, just a sample
                insert(Friendship.Tbl, Friendship(relativist, electrician))

                val car = insert(Car.Tbl, Car(electrician, "the coolest air cooler"))
            }
        }
    }

    /*fun nameSurnameProp(human: Struct<Human>): Property<String> =
        humanListProp.map {
            it.first { it.primaryKey == human.primaryKey }.let { it[Human.Name] + " " + it[Human.Surname] }
        }*/

    override fun close() =
        disposeMePlz.close()

}
