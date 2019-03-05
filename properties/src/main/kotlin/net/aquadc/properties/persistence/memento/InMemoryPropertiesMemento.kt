package net.aquadc.properties.persistence.memento

import net.aquadc.persistence.stream.BetterDataOutput
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.persistence.PropertyIo


class InMemoryPropertiesMemento : PropertiesMemento {

    @JvmField @JvmSynthetic internal val types = ArrayList<DataType<*>>()
    @JvmField @JvmSynthetic internal val vals = ArrayList<Any?>()

    constructor(properties: PersistableProperties) {
        properties.saveOrRestore(object : PropertyIo {

            override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
                types.add(this)
                vals.add(prop.value)
            }

        })
    }

    override fun reader(): PropertyIo =
            object : PropertyIo {

                private var idx = 0

                override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
                    @Suppress("UNCHECKED_CAST")
                    (prop as MutableProperty<Any?>).value = vals[idx++]
                }

            }

    @Suppress("UNCHECKED_CAST")
    fun <K> writeTo(kind: BetterDataOutput<K>, output: K) {
        for (i in types.indices) {
            val type = types[i]
            val value = vals[i]
            @Suppress("UPPER_BOUND_VIOLATED")
            (type as DataType<Any?>).write(kind, output, value)
        }
    }

}
