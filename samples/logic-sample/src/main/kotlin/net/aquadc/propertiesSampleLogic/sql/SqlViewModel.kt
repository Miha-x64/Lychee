package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.*
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly.cell
import net.aquadc.persistence.sql.blocking.Eagerly.execute
import net.aquadc.persistence.sql.blocking.Eagerly.struct
import net.aquadc.persistence.sql.blocking.Eagerly.structs
import net.aquadc.persistence.sql.template.Mutation
import net.aquadc.persistence.sql.template.Query
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.string
import net.aquadc.properties.*
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.persistence.ObservableStruct
import java.io.Closeable


class SqlViewModel<CUR>(
    private val session: Session<Blocking<CUR>>
): Closeable {

    private val count: Session<Blocking<CUR>>.() -> Int =
        Query("SELECT COUNT(*) FROM ${Human.Tbl.name}", cell(i32))
    private val fetch: Session<Blocking<CUR>>.() -> List<Struct<Human>> =
        Query("SELECT * FROM ${Human.Tbl.name}", structs(Human.Tbl, BindBy.Name))
    private val fetchName: Session<Blocking<CUR>>.(Long) -> String =
        Query("SELECT \"name\" FROM ${Human.Tbl.name} WHERE _id = ?", i64, cell(string))
    private val _find: Session<Blocking<CUR>>.(Long) -> Struct<Human> =
        Query("SELECT * FROM ${Human.Tbl.name} WHERE _id = ?", i64, struct(Human.Tbl, BindBy.Name))
    private val find: (Long) -> Struct<Human>? =
        { id -> try { session._find(id) } catch (e: NoSuchElementException) { null } } // FIXME

    private val updateName: Transaction<Blocking<CUR>>.(String, Long) -> Unit =
        Mutation("UPDATE ${Human.Tbl.name} SET ${Human.run { Name.name }} = ? WHERE _id = ?", string, i64, execute())

    private val carsWithConditionersByOwner: Session<Blocking<CUR>>.(Long) -> List<Struct<Car>> =
        Query("SELECT * FROM ${Car.Tbl.name} WHERE ${Car.run { OwnerId.name }} = ? AND ${Car.run { ConditionerModel.name }} IS NOT NULL",
            i64,
            structs(Car.Tbl, BindBy.Name))

    val titleProp: Property<String>
    private val people = HashMap<Long, ObservableStruct<Human>>()
    val humanListProp: Property<List<Struct<Human>>>
    val selectedProp = propertyOf<Struct<Human>?>(null)
    val disposeMePlz: Closeable

    init {
        fillIfEmpty()
        val countProp = propertyOf(session.count())
        titleProp = countProp.map { "$it users" }
        humanListProp = propertyOf(
            session.fetch().map { h -> ObservableStruct(h, false).also { people[h[Human.Id]] = it } }
        )
        disposeMePlz = session.observe(
            Human.Tbl to TriggerEvent.INSERT, Human.Tbl to TriggerEvent.UPDATE, Human.Tbl to TriggerEvent.DELETE) { report ->
            val (ins, upd, del) = report.of(Human.Tbl)
            countProp.value += ins.size - del.size

            val list = ArrayList(humanListProp.value)
            del.forEach {
                people.remove(it)?.let(list::remove)
            }
            upd.forEach { id ->
                people[id]?.let { list.firstOrNull { it[Human.Id] == id }?.set(Human.Name, session.fetchName(id)) }
            }
            ins.forEach { id ->
                val h = ObservableStruct(session._find(id), false)
                people[id] = h
                list.add(h)
            }
            humanListProp.value = list
        }
    }

    private val namePatch = propertyOf(mapOf<@ParameterName("humanId") Long, String>()).also {
        it.debounced(1000L).onEach { new ->
            if (new.isNotEmpty() && it.casValue(new, emptyMap())) {
                session.withTransaction {
                    new.forEach { (humanId, newName) ->
                        updateName(newName, humanId)
                    }
                }
            }
        }
    }
    val actionsEnabledProp = selectedProp.map(Objectz.IsNotNull)

    val airConditionersTextProp = selectedProp
        .map {
            val cars = if (it == null) emptyList() else session.carsWithConditionersByOwner(it[Human.Id])
            if (cars.isEmpty()) "none"
            else
                cars.map { it[Car.ConditionerModel]!! }
                    .joinToString(prefix = "Air conditioner(s) in car(s): [", postfix = "]")
        }

    val nameProp = selectedProp.map { it?.get(Human.Name) ?: "" }
    val editableNameProp = propertyOf("")
        .onEach { newName ->
            selectedProp.value?.let {
                val id = it[Human.Id]
                if (newName == it[Human.Name]) namePatch -= id
                else namePatch += it[Human.Id] to newName
            }
        }

    val lastInserted = propertyOf<Struct<Human>?>(null)

    val createClicked = propertyOf(false)
        .clearEachAndTransact {
            lastInserted.value = find(insert(Human.Tbl, Human("<new>", "")))!!
        }

    val deleteClicked = propertyOf(false)
        .clearEachAndTransact {
            val id = selectedProp.value!![Human.Id]
            namePatch -= id
            delete(Human.Tbl, id)
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

                insert(Car.Tbl, Car(electrician, "the coolest air cooler"))
            }
        }
    }

    fun nameSurnameProp(human: Struct<Human>): Property<String> =
        humanListProp.map {
            it.firstOrNull { it[Human.Id] == human[Human.Id] }?.let { it[Human.Name] + " " + it[Human.Surname] } ?: ""
        }

    override fun close() =
        disposeMePlz.close()

}
