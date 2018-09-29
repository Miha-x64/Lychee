package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.properties.*
import net.aquadc.properties.sql.*


class SqlViewModel(
        private val session: Session
) {

    init {
        fillIfEmpty()
    }

    val titleProp = session[HumanTable].count().map { "Sample SQLite application ($it records)" }
    val humanListProp = session[HumanTable].selectAll(HumanTable.Name.asc, HumanTable.Surname.asc)
    val selectedProp = propertyOf<Human?>(null)
    private val namePatch = propertyOf(mapOf<Human, String>()).also {
        it.debounced(1000L).onEach { new ->
            if (new.isNotEmpty() && it.casValue(new, mapOf())) {
                session.withTransaction {
                    new.forEach { (human, newName) ->
                        if (human.isManaged && human.nameProp.value != newName) { // if it was just deleted, ignore
                            human.nameProp.set(newName)
                        }
                    }
                }
            }
        }
    }
    val actionsEnabledProp = selectedProp.map(isNotNull())

    val airConditionersTextProp = selectedProp
            .flatMapNotNullOrDefault(emptyList(), Human::carsProp)
            .flatMap { cars: List<Car> ->
                cars
                        .map(propertyGetterOf(CarTable.ConditionerModel))
                        .mapValueList(List<String?>::filterNotNull)
            }.map { conditioners ->
                if (conditioners.isEmpty()) "none"
                else conditioners.joinToString(prefix = "Air conditioner(s) in car(s): [\n", postfix = "]")
            }

    val nameProp = selectedProp.flatMapNotNullOrDefault("", propertyGetterOf(HumanTable.Name))
    val editableNameProp = propertyOf("").also {
        it.onEach { newText ->
            selectedProp.value?.let {
                namePatch += it to newText
            }
        }
    }

    val lastInserted = propertyOf<Human?>(null)

    val createClicked = propertyOf(false).also {
        it.clearEachAnd {
            lastInserted.value = session.withTransaction { insertHuman("", "") }
        }
    }

    val deleteClicked = propertyOf(false).also {
        it.clearEachAnd {
            session.withTransaction {
                delete(selectedProp.value!!)
            }
        }
    }

    private fun fillIfEmpty() {
        if (session[HumanTable].count().value == 0L) {
            session.withTransaction {
                insertHuman("Stephen", "Hawking")
                val relativist = insertHuman("Albert", "Einstein")
                insertHuman("Dmitri", "Mendeleev")
                val electrician = insertHuman("Nikola", "Tesla")

                // don't know anything about their friendship, just a sample
                insert(FriendTable,
                        FriendTable.LeftId - relativist.primaryKey, FriendTable.RightId - electrician.primaryKey
                )

                val car = insertCar(electrician)
                car.conditionerModelProp.set("the coolest air cooler")
            }
        }
    }

}
