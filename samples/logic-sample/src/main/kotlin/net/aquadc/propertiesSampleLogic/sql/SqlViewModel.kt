package net.aquadc.propertiesSampleLogic.sql

import net.aquadc.persistence.sql.*
import net.aquadc.properties.*
import net.aquadc.properties.function.Objectz
import net.aquadc.properties.persistence.propertyGetterOf


class SqlViewModel(
        private val session: Session<*>
) {

    private val humanDao get() = session[Human.Tbl]

    init {
        fillIfEmpty()
    }

    val titleProp = humanDao.count().map { "Sample SQLite application ($it records)" }
    val humanListProp = humanDao.selectAll(Human.Name.asc, Human.Surname.asc)
    val selectedProp = propertyOf<Record<Human, Long>?>(null)
    private val namePatch = propertyOf(mapOf<@ParameterName("humanId") Long, String>()).also {
        it.debounced(1000L).onEach { new ->
            if (new.isNotEmpty() && it.casValue(new, emptyMap())) {
                session.withTransaction {
                    new.forEach { (humanId, newName) ->
                        humanDao.find(humanId) // if it was just deleted, ignore
                            ?.takeIf { it[Human.Name] != newName }
                            ?.let { human ->
                                human[Human.Name] = newName
                            }
                    }
                }
            }
        }
    }
    val actionsEnabledProp = selectedProp.map(Objectz.IsNotNull)

    val airConditionersTextProp = selectedProp
            .flatMapNotNullOrDefault(emptyList(), { session[Car.Tbl].select(Car.OwnerId eq it.primaryKey) })
            .flatMap { cars: List<Record<Car, Long>> ->
                cars
                        .map(propertyGetterOf(Car.ConditionerModel))
                        .mapValueList(List<String?>::filterNotNull)
            }.map { conditioners ->
                if (conditioners.isEmpty()) "none"
                else conditioners.joinToString(prefix = "Air conditioner(s) in car(s): [", postfix = "]")
            }

    val nameProp = selectedProp.flatMapNotNullOrDefault("", propertyGetterOf(Human.Name))
    val editableNameProp = propertyOf("")
        .onEach { newText ->
            selectedProp.value
                ?.takeIf { it.isManaged }
                ?.let {
                    namePatch += it.primaryKey to newText
                }
        }

    val lastInserted = propertyOf<Record<Human, Long>?>(null)

    val createClicked = propertyOf(false)
        .clearEachAndTransact {
            lastInserted.value = insert(Human.Tbl, Human("<new>", ""))
        }

    val deleteClicked = propertyOf(false)
        .clearEachAndTransact {
            delete(selectedProp.value!!)
        }

    val truncateClicked = propertyOf(false)
        .clearEachAndTransact {
            truncate(Human.Tbl)
        }

    private fun <P : MutableProperty<Boolean>> P.clearEachAndTransact(func: Transaction.() -> Unit): P =
        clearEachAnd { session.withTransaction { func() } }

    private fun fillIfEmpty() {
        if (humanDao.count().value == 0L) {
            session.withTransaction {
                insert(Human.Tbl, Human("Stephen", "Hawking"))
                val relativist = insert(Human.Tbl, Human("Albert", "Einstein"))
                insert(Human.Tbl, Human("Dmitri", "Mendeleev"))
                val electrician = insert(Human.Tbl, Human("Nikola", "Tesla"))

                // don't know anything about their friendship, just a sample
                insert(Friendship.Tbl, Friendship(relativist.primaryKey, electrician.primaryKey))

                val car = insert(Car.Tbl, Car(electrician.primaryKey))
                car[Car.ConditionerModel] = "the coolest air cooler"
            }
        }
    }

    fun nameSurnameProp(human: Record<Human, Long>) =
            (human prop Human.Name).map { n -> "$n ${human[Human.Surname]}" }

}
