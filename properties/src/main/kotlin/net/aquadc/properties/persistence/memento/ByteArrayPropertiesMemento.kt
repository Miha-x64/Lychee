package net.aquadc.properties.persistence.memento

import net.aquadc.persistence.stream.CleverDataInputStream
import net.aquadc.persistence.stream.CleverDataOutputStream
import net.aquadc.properties.persistence.PropertyReader
import net.aquadc.properties.persistence.PropertyWriter
import java.io.*

/**
 * Captures values of properties into a [ByteArray] eagerly.
 */
class ByteArrayPropertiesMemento(
        private var bytes: ByteArray
) : PropertiesMemento, Externalizable {

    constructor() : this(ByteArray(0))

    constructor(properties: PersistableProperties) : this(ByteArrayOutputStream().let { os ->
        properties.saveOrRestore(PropertyWriter(CleverDataOutputStream(os)))
        os.toByteArray()
    })

    constructor(memento: InMemoryPropertiesMemento) : this(ByteArrayOutputStream().let { os ->
        memento.writeTo(CleverDataOutputStream(os))
        os.toByteArray()
    })

    override fun restoreTo(target: PersistableProperties) {
        target.saveOrRestore(PropertyReader(CleverDataInputStream(ByteArrayInputStream(bytes))))
    }

    override fun writeExternal(out: ObjectOutput) {
        out.writeObject(bytes)
    }

    override fun readExternal(`in`: ObjectInput) {
        bytes = `in`.readObject() as ByteArray
    }

    fun copyValue(): ByteArray
            = bytes.clone()

}
