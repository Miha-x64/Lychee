package net.aquadc.persistence.struct

import android.support.annotation.RestrictTo

/**
 * Builds a [StructSnapshot] or throws if field value neither specified explicitly nor has a default.
 */
inline fun <SCH : Schema<SCH>> SCH.build(build: SCH.(StructBuilder<SCH>) -> Unit): StructSnapshot<SCH> {
    val builder = newBuilder<SCH>(this)
    build(this, builder)
    return builder.finish(this, searchForDefaults = true)
}

/**
 * Builds a [StructSnapshot] filled with data from [this] and applies changes via [mutate].
 */
inline fun <SCH : Schema<SCH>> Struct<SCH>.copy(mutate: SCH.(StructBuilder<SCH>) -> Unit): StructSnapshot<SCH> {
    val builder = buildUpon(this)
    mutate(schema, builder)
    return builder.finish(schema, searchForDefaults = false)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <SCH : Schema<SCH>> newBuilder(schema: SCH): StructBuilder<SCH> =
        StructBuilder<SCH>(Array(schema.fields.size) { Unset })

@PublishedApi internal fun <SCH : Schema<SCH>> buildUpon(source: PartialStruct<SCH>): StructBuilder<SCH> {
    val fs = source.schema.fields
    val values = Array<Any?>(fs.size) { Unset }
    source.schema.forEach<SCH, FieldDef<SCH, *>>(source.fields) { field ->
        values[field.ordinal.toInt()] = source[field]
    }
    return StructBuilder(values)
}

/**
 * A temporary wrapper around [Array] for instantiating [StructSnapshot]s.
 */
inline class StructBuilder<SCH : Schema<SCH>> /*internal*/ constructor(
        private val values: Array<Any?>
) {

    operator fun <T> set(key: FieldDef<SCH, T>, value: T) {
        values[key.ordinal.toInt()] = value
    }

    /**
     * Create a [StructSnapshot] unsafely capturing [values] array.
     * [searchForDefaults]=false unsafely assumes that all fields have according values!
     */
    @PublishedApi internal fun finish(schema: SCH, searchForDefaults: Boolean): StructSnapshot<SCH> {
        if (searchForDefaults) {
            values.forEachIndexed { i, value ->
                if (value === Unset)
                    values[i] = schema.fields[i].default
            }
        }

        return StructSnapshot(schema, values)
    }

    fun fieldsPresent(): FieldSet<SCH, FieldDef<SCH, *>> {
        var set = 0L
        var field = 1L
        values.forEach { value ->
            if (value !== Unset) {
                set = set or field
            }
            field = field shl 1
        }
        return FieldSet(set)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun expose(): Array<Any?> =
            values

}
