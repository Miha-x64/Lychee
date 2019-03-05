package net.aquadc.persistence

import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.memento.InMemoryPropertiesMemento
import net.aquadc.properties.persistence.memento.PersistableProperties
import net.aquadc.properties.persistence.memento.restoreTo
import net.aquadc.properties.persistence.x
import net.aquadc.properties.propertyOf
import org.junit.Assert.assertEquals
import org.junit.Test


class Stateful {

    class SomeStateful(state: PropertyIo?) : PersistableProperties {
        val prop = propertyOf(1)
        override fun saveOrRestore(io: PropertyIo) {
            io x prop
        }
        init {
            state?.let(::saveOrRestore)
        }
    }

    @Test fun `save-restore`() {
        val stateful = SomeStateful(null)
        stateful.prop.value = 100500
        val memento = InMemoryPropertiesMemento(stateful)
        stateful.prop.value = 200700
        memento.restoreTo(stateful)
        assertEquals(100500, stateful.prop.value)

        val newStateful = SomeStateful(memento.reader())
        assertEquals(100500, newStateful.prop.value)
    }

    class DelegatingStateful(state: PropertyIo?) : PersistableProperties {
        val ownProp1 = propertyOf(1)
        lateinit var delegate: SomeStateful
        val ownProp2 = propertyOf(2)
        init {
            if (state === null) delegate = SomeStateful(null)
            else saveOrRestore(state)
        }
        override fun saveOrRestore(io: PropertyIo) {
            io x ownProp1
            if (::delegate.isInitialized) delegate.saveOrRestore(io) else delegate = SomeStateful(io)
            io x ownProp2
        }
    }

    @Test fun `delegating save-restore`() {
        val delegating = DelegatingStateful(null).apply {
            ownProp1.value = 10
            delegate.prop.value = 20
            ownProp2.value = 30
        }
        val memento = InMemoryPropertiesMemento(delegating)
        delegating.apply {
            ownProp1.value = -1
            delegate.prop.value = -2
            ownProp2.value = -3
        }
        memento.restoreTo(delegating)
        delegating.apply {
            assertEquals(10, ownProp1.value)
            assertEquals(20, delegate.prop.value)
            assertEquals(30, ownProp2.value)
        }

        DelegatingStateful(memento.reader()).apply {
            assertEquals(10, ownProp1.value)
            assertEquals(20, delegate.prop.value)
            assertEquals(30, ownProp2.value)
        }
    }

}
