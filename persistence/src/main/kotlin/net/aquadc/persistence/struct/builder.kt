package net.aquadc.persistence.struct

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

@PublishedApi internal fun <SCH : Schema<SCH>> newBuilder(schema: SCH): StructBuilder<SCH> =
        StructBuilder<SCH>(Array(schema.fields.size) { Unset })

@PublishedApi internal fun <SCH : Schema<SCH>> buildUpon(source: Struct<SCH>): StructBuilder<SCH> {
    val fs = source.schema.fields
    return StructBuilder(Array(fs.size) { i -> source[fs[i]] })
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

}
