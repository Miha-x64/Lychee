package net.aquadc.properties.android.persistence.parcel

import android.os.Parcel
import android.os.Parcelable
import net.aquadc.persistence.stream.DataStreams
import net.aquadc.properties.persistence.memento.ByteArrayPropertiesMemento
import net.aquadc.properties.persistence.memento.InMemoryPropertiesMemento
import net.aquadc.properties.persistence.memento.PersistableProperties
import net.aquadc.properties.persistence.memento.PropertiesMemento
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Captures values of properties into a [ByteArray] eagerly.
 */
class ParcelPropertiesMemento : PropertiesMemento, Parcelable {

    private var memento: InMemoryPropertiesMemento? = null
    private var bytes: ByteArray? = null

    constructor(properties: PersistableProperties) {
        this.memento = InMemoryPropertiesMemento(properties)
    }

    constructor(bytes: ByteArray) {
        this.bytes = bytes
    }

    override fun restoreTo(target: PersistableProperties) {
        val memento = memento
        val bytes = bytes
        when {
            memento != null -> memento.restoreTo(target)
            bytes != null -> ByteArrayPropertiesMemento(bytes).restoreTo(target)
            else -> throw AssertionError()
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(bytes())
    }

    private fun bytes(): ByteArray {
        bytes?.let { return it }

        val os = ByteArrayOutputStream()
        memento!!.writeTo(DataStreams, DataOutputStream(os))
        val bytes = os.toByteArray()
        this.bytes = bytes
        return bytes
    }

    /* non-KDoc
     * Note: 'Add Parcelable implementation' intention by Android plug-in
     * creates a `companion object CREATOR : Parcelable.Creator`
     * which has 'com.package.Outer$CREATOR` declaration type.
     * For compatibility with the following popular ProGuard rule:
       -keepclassmembers class * implements android.os.Parcelable {
           public static final android.os.Parcelable$Creator CREATOR;
       }
     * `CREATOR` field must be declared as `android.os.Parcelable$Creator` instead of a particular subclass.
     * Thus, we use a separate field to specify this type explicitly.
     */
    private companion object {

        @Suppress("unused") @JvmField val CREATOR: Parcelable.Creator<ParcelPropertiesMemento> =
                object : Parcelable.Creator<ParcelPropertiesMemento> {

                    override fun createFromParcel(source: Parcel): ParcelPropertiesMemento =
                            ParcelPropertiesMemento(source.createByteArray())

                    override fun newArray(size: Int): Array<ParcelPropertiesMemento?> =
                            arrayOfNulls(size)
                }

    }

}
