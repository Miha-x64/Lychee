package net.aquadc.persistence.struct

/**
 * Builds a [StructSnapshot] or throws if field value neither specified explicitly nor has a default.
 */
inline fun <SCH : Schema<SCH>> SCH.build(build: (StructBuilder<SCH>) -> Unit): StructSnapshot<SCH> {
    val builder = newBuilder<SCH>(this)
    build(builder)
    return builder.finish(this)
}

@PublishedApi internal fun <SCH : Schema<SCH>> newBuilder(schema: Schema<*>) =
        StructBuilder<SCH>(Array(schema.fields.size) { Unset })


inline class StructBuilder<SCH : Schema<SCH>> /*internal*/ constructor(
        private val values: Array<Any?>
) {

    operator fun <T> set(key: FieldDef<SCH, T>, value: T) {
        values[key.ordinal.toInt()] = value
    }

    fun finish(schema: SCH): StructSnapshot<SCH> {
        values.forEachIndexed { i, value ->
            if (value === Unset)
                values[i] = schema.fields[i].default
        }

        return StructSnapshot(schema, values)
    }

}
