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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <SCH : Schema<SCH>> buildUpon(source: PartialStruct<SCH>): StructBuilder<SCH> {
    val fs = source.schema.fields
    val values = Array<Any?>(fs.size) { Unset }
    source.schema.forEach(source.fields) { field ->
        values[field.ordinal.toInt()] = source.getOrThrow(field)
    }
    return StructBuilder(values)
}

/**
 * A temporary wrapper around [Array] for instantiating [StructSnapshot]s.
 */
inline class StructBuilder<SCH : Schema<SCH>> /*internal*/ constructor(
        private val values: Array<Any?>
) {

    /**
     * Asserts that the given field is set and gets its value.
     * Useful for patching structures deeply:
     *    struct.copy {
     *        it[Nested] = it[Nested].copy {
     *            it[SomeField] = newValue
     *        }
     *    }
     */
    operator fun <T> get(key: FieldDef<SCH, T>): T {
        val v = values[key.ordinal.toInt()]
        if (v === Unset) throw NoSuchElementException()
        else return v as T
    }

    /**
     * Assigns the given [value] to the specified [field].
     */
    operator fun <T> set(field: FieldDef<SCH, T>, value: T) {
        values[field.ordinal.toInt()] = value
    }

    /**
     * Assigns field values from [source].
     * @return a set of updated fields
     *   = intersection of requested [fields] and [PartialStruct.fields] present in [source]
     */
    fun setFrom(
            source: PartialStruct<SCH>,
            fields: FieldSet<SCH, FieldDef<SCH, *>> = source.schema.allFieldSet()
    ): FieldSet<SCH, FieldDef<SCH, *>> =
            source.fields.intersect(fields).also { intersect ->
                source.schema.forEach(intersect) { field ->
                    mutateFrom(source, field)
                }
            }
    // captures [T] and asserts [field] is present in [source]
    private inline fun <T> mutateFrom(source: PartialStruct<SCH>, field: FieldDef<SCH, T>) {
        this[field] = source.getOrThrow(field)
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
