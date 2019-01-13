package net.aquadc.properties.persistence.memento

import net.aquadc.persistence.stream.BetterDataOutput
import net.aquadc.persistence.stream.write
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.persistence.*


class InMemoryPropertiesMemento : PropertiesMemento {

    @JvmField @JvmSynthetic internal val types = ArrayList<Any?>() // E = Converter | Class<Enum<E>> | null
    @JvmField @JvmSynthetic internal val vals = ArrayList<Any?>()

    constructor(properties: PersistableProperties) {
        properties.saveOrRestore(object : PropertyIo {

            override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) {
                types.add(this)
                vals.add(prop.value)
            }

            override fun chars(prop: MutableProperty<CharArray>) {
                addNoConv(prop)
            }

            override fun ints(prop: MutableProperty<IntArray>) {
                addNoConv(prop)
            }

            override fun longs(prop: MutableProperty<LongArray>) {
                addNoConv(prop)
            }

            override fun floats(prop: MutableProperty<FloatArray>) {
                addNoConv(prop)
            }

            override fun doubles(prop: MutableProperty<DoubleArray>) {
                addNoConv(prop)
            }

            override fun stringList(prop: MutableProperty<List<String>>) {
                addNoConv(prop)
            }

            private fun addNoConv(prop: MutableProperty<*>) {
                types.add(null)
                vals.add(prop.value)
            }

            override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) {
                types.add(type)
                vals.add(prop.value)
            }

            override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) {
                types.add(type)
                vals.add(prop.value)
            }

        })
    }

    override fun restoreTo(target: PersistableProperties) {
        target.saveOrRestore(object : PropertyIo {

            private var idx = 0

            override fun <T> DataType<T>.invoke(prop: MutableProperty<T>) = assignTo(prop)

            override fun chars(prop: MutableProperty<CharArray>) = assignTo(prop)

            override fun ints(prop: MutableProperty<IntArray>) = assignTo(prop)

            override fun longs(prop: MutableProperty<LongArray>) = assignTo(prop)

            override fun floats(prop: MutableProperty<FloatArray>) = assignTo(prop)

            override fun doubles(prop: MutableProperty<DoubleArray>) = assignTo(prop)

            override fun stringList(prop: MutableProperty<List<String>>) = assignTo(prop)

            override fun <E : Enum<E>> enum(prop: MutableProperty<E>, type: Class<E>) = assignTo(prop)

            override fun <E : Enum<E>> enumSet(prop: MutableProperty<Set<E>>, type: Class<E>) = assignTo(prop)

            private fun assignTo(prop: MutableProperty<*>) {
                @Suppress("UNCHECKED_CAST")
                (prop as MutableProperty<Any?>).value = vals[idx++]
            }

        })
    }

    @Suppress("UNCHECKED_CAST")
    fun <K> writeTo(kind: BetterDataOutput<K>, output: K) {
        for (i in types.indices) {
            val type = types[i]
            val value = vals[0]
            @Suppress("UPPER_BOUND_VIOLATED")
            when (type) {
                is DataType<*> -> (type as DataType<Any?>).write(kind, output, value)
                null -> writeValue<K>(kind, output, value)
                is Class<*> -> writeEnum<K, Any>(kind, output, type as Class<Any>, value!!)
                else -> throw AssertionError()
            }
        }
    }

    private fun <K> writeValue(kind: BetterDataOutput<K>, output: K, value: Any?) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
            is CharArray -> kind.writeCharArray(output, value)
            is IntArray -> kind.writeIntArray(output, value)
            is LongArray -> kind.writeLongArray(output, value)
            is FloatArray -> kind.writeFloatArray(output, value)
            is DoubleArray -> kind.writeDoubleArray(output, value)
            is List<*> -> kind.writeStringList(output, value as List<String>)
            else -> throw AssertionError()
        }
    }

    private fun <K, E : Enum<E>> writeEnum(kind: BetterDataOutput<K>, output: K, type: Class<E>, value: Any) {
        @Suppress("UNCHECKED_CAST")
        when (value) {
            type.isInstance(value) -> kind.writeString(output, (value as Enum<*>).name)
            value is Set<*> -> kind.writeEnumSet(output, value as Set<E>)
            else -> throw AssertionError()
        }
    }

}
