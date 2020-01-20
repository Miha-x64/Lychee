package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.*
import net.aquadc.properties.*
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.persistence.propertyGetterOf


class SqlViewModel(
        private val session: Session<*>
) {

    private val humanDao = session[Human.Tbl]

    init {
        fillIfEmpty()
    }

    val titleProp = humanDao.count().map { "Sample SQLite application ($it records)" }
    val humanListProp = humanDao.selectAll(Human.Name.asc, Human.Surname.asc)
    val selectedProp = propertyOf<Human?>(null)
    private val namePatch = propertyOf(mapOf<@ParameterName("humanId") Long, String>()).also {
        it.debounced(1000L).onEach { new ->
            if (new.isNotEmpty() && it.casValue(new, emptyMap())) {
                session.withTransaction {
                    new.forEach { (humanId, newName) ->
                        val human = humanDao.find(humanId)
                        if (human !== null && human.nameProp.value != newName) { // if it was just deleted, ignore
                            human[Human.Name] = newName
                        }
                    }
                }
            }
        }
    }
    val actionsEnabledProp = selectedProp.map(Objectz.IsNotNull)

    val airConditionersTextProp = selectedProp
            .flatMapNotNullOrDefault(emptyList(), Human::carsProp)
            .flatMap { cars: List<Car> ->
                cars
                        .map(propertyGetterOf(Car.ConditionerModel))
                        .mapValueList(List<String?>::filterNotNull)
            }.map { conditioners ->
                if (conditioners.isEmpty()) "none"
                else conditioners.joinToString(prefix = "Air conditioner(s) in car(s): [\n", postfix = "]")
            }

    val nameProp = selectedProp.flatMapNotNullOrDefault("", propertyGetterOf(Human.Name))
    val editableNameProp = propertyOf("").also {
        it.onEach { newText ->
            val selected = selectedProp.value
            if (selected != null && selected.isManaged) {
                namePatch += selected.primaryKey to newText
            }
        }
    }

    val lastInserted = propertyOf<Human?>(null)

    val createClicked = propertyOf(false).also {
        it.clearEachAnd {
            lastInserted.value = session.withTransaction { insertHuman("<new>", "") }
        }
    }

    val deleteClicked = propertyOf(false).also {
        it.clearEachAnd {
            session.withTransaction {
                delete(selectedProp.value!!)
            }
        }
    }

    val truncateClicked = propertyOf(false).also {
        it.clearEachAnd {
            session.withTransaction {
                truncate(Human.Tbl)
            }
        }
    }

    private fun fillIfEmpty() {
        if (humanDao.count().value == 0L) {
            session.withTransaction {
                insertHuman("Stephen", "Hawking")
                val relativist = insertHuman("Albert", "Einstein")
                insertHuman("Dmitri", "Mendeleev")
                val electrician = insertHuman("Nikola", "Tesla")

                // don't know anything about their friendship, just a sample
                insertFriendship(relativist, electrician)

                val car = insertCar(electrician)
                car[Car.ConditionerModel] = "the coolest air cooler"
            }
        }
    }

    fun nameSurnameProp(human: Human) =
            human.nameProp.map { n -> "$n ${human.surname}" }

}
