package net.aquadc.properties.android.persistence.pref

import android.content.SharedPreferences
import net.aquadc.persistence.struct.*
import net.aquadc.persistence.type.DataType
import net.aquadc.properties.Property
import net.aquadc.properties.TransactionalProperty
import net.aquadc.properties.internal.ManagedProperty
import net.aquadc.properties.internal.Manager
import net.aquadc.properties.internal.Unset
import net.aquadc.properties.persistence.TransactionalPropertyStruct

/**
 * Represents a [Struct] stored in [SharedPreferences].
 * Has weak transactional semantics,
 * commits using asynchronous [SharedPreferences.Editor.apply] method,
 * ignores 'dirty' state.
 *
 * Note: [SharedPreferences] has no support for `null` values.
 * when you put `null` value into Prefs, [SharedPreferences.Editor.remove] is being called,
 * and you will see [FieldDef.default] value.
 * To avoid inconsistencies, either use non-nullable types, or use `null` [FieldDef.default] value.
 */
class SharedPreferenceStruct<DEF : StructDef<DEF>> : BaseStruct<DEF>, TransactionalPropertyStruct<DEF> {

    @JvmField @JvmSynthetic internal val values: Array<Any?> // = ManagedProperty<DEF, StructTransaction<DEF>, T> | T
    @JvmField @JvmSynthetic internal val prefs: SharedPreferences

    /**
     * Copies data from [source] into [prefs].
     * Overwrites existing data, if any.
     */
    constructor(source: Struct<DEF>, prefs: SharedPreferences) : super(source.type) {
        val fields = type.fields
        val ed = prefs.edit()
        this.values = Array(fields.size) { i ->
            val field = fields[i]
            val value = source[field]
            when (field) {
                is FieldDef.Mutable<DEF, *> -> ManagedProperty(manager, field as FieldDef.Mutable<DEF, Any?>, value)
                is FieldDef.Immutable<DEF, *> -> value
            }
            (field.type as DataType<Any?>).put(ed, field.name, value)
        }
        ed.apply()
        this.prefs = prefs
        prefs.registerOnSharedPreferenceChangeListener(manager)
    }

    /**
     * Reads, writes, observes [prefs],
     * assuming fields are either have values previously written to [prefs] or have [FieldDef.default] ones.
     */
    constructor(type: DEF, prefs: SharedPreferences) : super(type) {
        val fields = type.fields
        this.values = Array(fields.size) {
            val field = fields[it]
            when (field) {
                is FieldDef.Mutable -> ManagedProperty(manager, field as FieldDef.Mutable<DEF, Any?>, Unset)
                is FieldDef.Immutable -> Unset
            }
        }
        this.prefs = prefs
        prefs.registerOnSharedPreferenceChangeListener(manager)
    }


    override fun <T> get(field: FieldDef<DEF, T>): T {
        val value = values[field.ordinal.toInt()]
        return when (field) {
            is FieldDef.Mutable -> (value as Property<T>).value
            is FieldDef.Immutable -> {
                if (value === Unset) {
                    val actual = field.get(prefs)
                    values[field.ordinal.toInt()] = actual
                    actual
                } else {
                    value
                } as T
            }
        }
    }

    private val manager = PrefManager()

    private inner class PrefManager : Manager<DEF, StructTransaction<DEF>>(), SharedPreferences.OnSharedPreferenceChangeListener {

        /* non-KDOC
         * getDirty implNote:
         *   'dirty' state is in memory; there can be several parallel transactions.
         *   Thus, 'dirty' state is nonsensical here.
         */

        override fun <T> getClean(field: FieldDef.Mutable<DEF, T>, id: Long): T =
                field.get(prefs)

        override fun <T> set(transaction: StructTransaction<DEF>, field: FieldDef.Mutable<DEF, T>, id: Long, update: T) {
            transaction.set(field, update)
        }

        // `SharedPreferences` keeps a weak reference and not going to leak us
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            val field = type.byName[key] ?: return
            val idx = field.ordinal.toInt()
            val value = field.get(sharedPreferences)
            when (field) {
                is FieldDef.Mutable -> (values[idx] as ManagedProperty<DEF, StructTransaction<DEF>, Any?>).commit(value)
                is FieldDef.Immutable -> throw IllegalStateException("Immutable field $field in $prefs was mutated externally!") // fixme: check SharedPreferences.toString
            }.also { }
        }

    }

    override fun <T> prop(field: FieldDef.Mutable<DEF, T>) =
            (values[field.ordinal.toInt()] as TransactionalProperty<StructTransaction<DEF>, T>)

    override fun beginTransaction(): StructTransaction<DEF> = object : SimpleStructTransaction<DEF>() {

        private val ed = prefs.edit()
        private var success: Boolean? = false

        override fun <T> set(field: FieldDef.Mutable<DEF, T>, update: T) {
            field.type.put(ed, field.name, update)
        }

        override fun close() {
            when (success) {
                true -> ed.apply() // SharedPrefs will trigger listeners automatically, nothing to do here
                false -> Unit // nothing to do here
                null -> error("attempting to close an already closed transaction")
            }
            success = null
        }

    }

}
