package net.aquadc.properties.persistence.memento

import net.aquadc.persistence.stream.DataStreams
import net.aquadc.properties.persistence.PropertyIo
import net.aquadc.properties.persistence.PropertyReader
import net.aquadc.properties.persistence.PropertyWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

/**
 * Captures values of properties into a [ByteArray] eagerly.
 */
class ByteArrayPropertiesMemento(
        private var bytes: ByteArray
) : PropertiesMemento, Externalizable {

    constructor() : this(ByteArray(0))

    constructor(properties: PersistableProperties) : this(ByteArrayOutputStream().let { os ->
        properties.saveOrRestore(PropertyWriter(DataStreams, DataOutputStream(os)))
        os.toByteArray()
    })

    constructor(memento: InMemoryPropertiesMemento) : this(ByteArrayOutputStream().let { os ->
        memento.writeTo(DataStreams, DataOutputStream(os))
        os.toByteArray()
    })

    override fun reader(): PropertyIo =
            PropertyReader(DataStreams, DataInputStream(ByteArrayInputStream(bytes)))

    override fun writeExternal(out: ObjectOutput) {
        out.writeObject(bytes)
    }

    override fun readExternal(`in`: ObjectInput) {
        bytes = `in`.readObject() as ByteArray
    }

    fun copyValue(): ByteArray
            = bytes.clone()

}
