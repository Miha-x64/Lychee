package net.aquadc.properties.android.persistence

import android.content.Context
import android.os.Parcel
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.properties.android.persistence.parcel.ParcelIo
import net.aquadc.properties.android.persistence.parcel.ParcelPropertiesMemento
import net.aquadc.properties.android.persistence.pref.SharedPreferencesStruct
import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.enum
import net.aquadc.properties.persistence.memento.PersistableProperties
import net.aquadc.properties.persistence.memento.restoreTo
import net.aquadc.properties.persistence.x
import net.aquadc.properties.propertyOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PersistenceRoboTest {

    val t = PersistenceTest()

    @Test fun parcel() {
        val parcel = Parcel.obtain()
        ParcelIo.write(parcel, t.instance)
        parcel.setDataPosition(0)
        val deserialized = ParcelIo.read(parcel, PersistenceTest.Sch)
        t.assertEqualToOriginal(deserialized, false)
    }

    @Test fun prefs() {
        val prefs = RuntimeEnvironment.application.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        t.assertEqualToOriginal(
                SharedPreferencesStruct(t.instance, prefs), // this will copy from t.instance into prefs
                false
        )
        t.assertEqualToOriginal(
                SharedPreferencesStruct(PersistenceTest.Sch, prefs), // this will read from prefs
                false
        )
    }

    @Test fun parcelPropsMemento() {
        val props = TestProps()
        val memento = ParcelPropertiesMemento(props)

        // shuffle
        props.prop1.value = false
        props.prop2.value = 42
        props.prop3.value = "zzz"
        props.prop4.value = Thread.State.NEW

        // write
        val parcel = Parcel.obtain()
        parcel.writeParcelable(memento, 0)

        parcel.setDataPosition(0)
        val restored = parcel.readParcelable<ParcelPropertiesMemento>(ParcelPropertiesMemento::class.java.classLoader)
        parcel.recycle()

        assertNotSame(memento, restored)

        restored.restoreTo(props)

        assertEquals(true, props.prop1.value)
        assertEquals(24, props.prop2.value)
        assertEquals("hello", props.prop3.value)
        assertEquals(Thread.State.BLOCKED, props.prop4.value)
    }

    private class TestProps : PersistableProperties {
        val prop1 = propertyOf(true)
        val prop2 = propertyOf(24)
        val prop3 = propertyOf("hello")
        val prop4 = propertyOf(Thread.State.BLOCKED)
        override fun saveOrRestore(io: PropertyIo) {
            io x prop1
            io x prop2
            io x prop3
            with (io) { enum<Thread.State>()(prop4) }
        }
    }

}
