package net.aquadc.properties.persistence

import java.io.*

/**
 * Captures properties' value into a [ByteArray] eagerly.
 */
class ByteArrayPropertiesMemento : PropertiesMemento, Externalizable {

    constructor(props: PersistableProperties) {
        val os = ByteArrayOutputStream()
        props.saveOrRestore(PropertyOutput(DataOutputStream(os)))
        bytes = os.toByteArray()
    }

    constructor() {
        bytes = EMPTY
    }

    private var bytes: ByteArray

    override fun writeExternal(out: ObjectOutput) {
        val bytes = bytes
        out.writeInt(bytes.size)
        out.write(bytes)
    }

    override fun readExternal(oi: ObjectInput) {
        val ba = ByteArray(oi.readInt())
        oi.read(ba)
        bytes = ba
    }

    override fun restoreTo(target: PersistableProperties) {
        target.saveOrRestore(PropertyInput(DataInputStream(ByteArrayInputStream(bytes))))
    }

    fun copyValue(): ByteArray =
            bytes.clone()

    private companion object {
        private const val serialVersionUid: Long = 1
        @JvmField val EMPTY = ByteArray(0)
    }

}
