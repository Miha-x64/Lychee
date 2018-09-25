package net.aquadc.persistence.converter

@Suppress("RedundantModalityModifier")
@PublishedApi internal open class EnumConverter<E : Enum<E>>(
        private val enumType: Class<E>
) : DelegatingConverter<E, String>(string) {

    open override fun asString(value: E): String = value.name

    open override fun from(value: String): E = java.lang.Enum.valueOf<E>(enumType, value)

    final override fun E.to(): String = asString(this)

} // TODO: support EnumSet
