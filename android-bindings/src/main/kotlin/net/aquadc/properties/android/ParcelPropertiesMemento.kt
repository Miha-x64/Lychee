package net.aquadc.properties.android

import android.os.Parcel
import android.os.Parcelable
import net.aquadc.properties.persistence.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Lazily writes encapsulated [PersistableProperties] into [Parcel].
 */
class ParcelPropertiesMemento : PropertiesMemento, Parcelable {

    private val props: PersistableProperties?
    private var data: ByteArray?

    constructor(props: PersistableProperties) {
        this.props = props
        this.data = null
    }
    constructor(data: ByteArray) {
        this.props = null
        this.data = data
    }

    private fun data(): ByteArray {
        data?.let { return it }
        props!!

        val os = ByteArrayOutputStream()

        props.saveOrRestore(PropertyOutput(DataOutputStream(os)))

        val bytes = os.toByteArray()
        data = bytes
        return bytes
    }

    /**
     * Restores value into [target].
     * When holding a reference to original properties, will restore values by reference.
     * When restored from [Parcel], will restore marshaled value from [ByteArray]
     */
    override fun restoreTo(target: PersistableProperties) {
        val source = props

        when {
            target === source -> { /* no-op */ }
            source != null -> { // restore by reference
                val propDataAndBuffer = PropertyBuffer.get()
                val (pd, pb) = propDataAndBuffer
                source.saveOrRestore(pd)
                pb.produceThen()
                target.saveOrRestore(pd)
                pb.consumeThen()
                PropertyBuffer.recycle(propDataAndBuffer)
            }
            else -> { // restore from byte array
                target.saveOrRestore(
                        PropertyInput(DataInputStream(ByteArrayInputStream(data())))
                )
            }
        }
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByteArray(data())
    }

    companion object CREATOR : Parcelable.Creator<ParcelPropertiesMemento> {
        override fun newArray(size: Int): Array<ParcelPropertiesMemento?> = arrayOfNulls(size)
        override fun createFromParcel(source: Parcel): ParcelPropertiesMemento = ParcelPropertiesMemento(source.createByteArray())
    }

}
