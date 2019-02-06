package net.aquadc.properties.android.persistence

import android.content.Context
import android.os.Parcel
import net.aquadc.persistence.stream.read
import net.aquadc.persistence.stream.write
import net.aquadc.properties.android.persistence.parcel.ParcelIo
import net.aquadc.properties.android.persistence.pref.SharedPreferencesStruct
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
        SharedPreferencesStruct(t.instance, prefs)
        t.assertEqualToOriginal(SharedPreferencesStruct(PersistenceTest.Sch, prefs), false)
    }

}
