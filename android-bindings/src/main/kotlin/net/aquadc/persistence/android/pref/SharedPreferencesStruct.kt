package net.aquadc.persistence.android.pref

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
 * Represents a [Struct] stored inside [SharedPreferences].
 * Has weak transactional semantics,
 * commits using asynchronous [SharedPreferences.Editor.apply] method,
 * ignores 'dirty' state.
 */
class SharedPreferencesStruct<SCH : Schema<SCH>> : BaseStruct<SCH>, TransactionalPropertyStruct<SCH> {

    @JvmField @JvmSynthetic internal val values: Array<Any?> // = ManagedProperty<SCH, StructTransaction<SCH>, T> | T
    @JvmField @JvmSynthetic internal val prefs: SharedPreferences

    /**
     * Copies data from [source] into [prefs].
     * Overwrites existing data, if any.
     */
    constructor(source: Struct<SCH>, prefs: SharedPreferences) : super(source.schema) {
        val sch = schema
        val fields = sch.fields
        val ed = prefs.edit()
        this.values = Array(fields.size) { i ->
            val field = fields[i]
            val value = source[field]
            sch.typeOf(field as FieldDef<SCH, Any?, DataType<Any?>>).put(ed, sch.nameOf(field).toString(), value)

            when (field) {
                is FieldDef.Mutable<SCH, *, *> -> ManagedProperty(manager, field as FieldDef.Mutable<SCH, Any?, *>, null, value)
                is FieldDef.Immutable<SCH, *, *> -> value
            }
        }
        ed.apply()
        this.prefs = prefs
        prefs.registerOnSharedPreferenceChangeListener(manager)
    }

    /**
     * Reads, writes, observes [prefs],
     * assuming fields are either have values previously written to [prefs] or have [Schema.defaultOrElse] ones.
     */
    constructor(type: SCH, prefs: SharedPreferences) : super(type) {
        val fields = type.fields
        this.values = Array(fields.size) {
            when (val field = fields[it]) {
                is FieldDef.Mutable -> ManagedProperty(manager, field as FieldDef.Mutable<SCH, Any?, *>, null, Unset)
                is FieldDef.Immutable -> Unset
            }
        }
        this.prefs = prefs
        prefs.registerOnSharedPreferenceChangeListener(manager)
    }


    override fun <T> get(field: FieldDef<SCH, T, *>): T {
        val ordinal = field.ordinal.toInt()
        val value = values[ordinal]
        return when (field) {
            is FieldDef.Mutable -> (value as Property<T>).value
            is FieldDef.Immutable -> {
                if (value === Unset) {
                    val actual = schema.get(field, prefs)
                    values[ordinal] = actual
                    actual
                } else {
                    value
                } as T
            }
        }
    }

    private val manager = PrefManager()

    private inner class PrefManager : Manager<SCH, StructTransaction<SCH>, Nothing?>, SharedPreferences.OnSharedPreferenceChangeListener {

        /* non-KDoc
         * implNote:
         *   'dirty' state is in memory; there can be several parallel transactions.
         *   Thus, 'dirty' state is nonsensical here.
         */

        override fun <T> getDirty(field: FieldDef.Mutable<SCH, T, *>, id: Nothing?): T =
                Unset as T

        override fun <T> getClean(field: FieldDef<SCH, T, *>, id: Nothing?): T =
                schema.get(field, prefs)

        override fun <T> set(transaction: StructTransaction<SCH>, field: FieldDef.Mutable<SCH, T, *>, id: Nothing?, previous: T, update: T) {
            transaction.set(field, update)
        }

        // `SharedPreferences` keeps a weak reference and not going to leak us
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            schema.fieldByName(key, { field ->
                val idx = field.ordinal.toInt()
                val value = schema.get(field, sharedPreferences)
                when (field) {
                    is FieldDef.Mutable -> (values[idx] as ManagedProperty<SCH, StructTransaction<SCH>, Any?, Nothing?>).commit(value)
                    is FieldDef.Immutable -> throw IllegalStateException("Immutable field $field in $prefs was mutated externally!")
                    // there will be ugly but a bit informative toString. Deal with it
                }.also { }
            }, { return })
        }

    }

    override fun <T> prop(field: FieldDef.Mutable<SCH, T, *>) =
            (values[field.ordinal.toInt()] as TransactionalProperty<StructTransaction<SCH>, T>)

    override fun beginTransaction(): StructTransaction<SCH> = object : SimpleStructTransaction<SCH>() {

        private val ed = prefs.edit()

        override fun <T> set(field: FieldDef.Mutable<SCH, T, *>, update: T) {
            schema.typeOf(field as FieldDef<SCH, T, DataType<T>>)
                    .put(ed, schema.nameOf(field).toString(), update)
        }

        override fun close() {
            when (successful) {
                true -> ed.apply() // SharedPrefs will trigger listeners automatically, nothing to do here
                false -> Unit // nothing to do here
                null -> error("transaction is already closed")
            }
            successful = null
        }

    }

}
