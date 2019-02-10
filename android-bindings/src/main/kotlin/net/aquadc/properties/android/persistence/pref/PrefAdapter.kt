package net.aquadc.properties.android.persistence.pref


@Deprecated("use converters from :persistence instead", level = DeprecationLevel.ERROR)
typealias PrefAdapter<T> = Nothing

@Deprecated("use converters from :persistence instead", level = DeprecationLevel.ERROR)
typealias SimplePrefAdapter<T> = Nothing

@Deprecated("use converters from :persistence instead", ReplaceWith("string", "net.aquadc.persistence.type.string"), DeprecationLevel.ERROR)
typealias StringPrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("set(string)", "net.aquadc.persistence.type.set", "net.aquadc.persistence.type.string"),
        DeprecationLevel.ERROR
)
typealias StringSetPrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("int", "net.aquadc.persistence.type.int"),
        DeprecationLevel.ERROR
)
typealias IntPrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("long", "net.aquadc.persistence.type.long"),
        DeprecationLevel.ERROR
)
typealias LongPrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("float", "net.aquadc.persistence.type.float"),
        DeprecationLevel.ERROR
)
typealias FloatPrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("double", "net.aquadc.persistence.type.double"),
        DeprecationLevel.ERROR
)
typealias DoublePrefAdapter = Unit

@Deprecated(
        "use converters from :persistence instead",
        ReplaceWith("bool", "net.aquadc.persistence.type.bool"),
        DeprecationLevel.ERROR
)
typealias BoolPrefAdapter = Unit
