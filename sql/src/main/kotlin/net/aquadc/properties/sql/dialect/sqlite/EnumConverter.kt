package net.aquadc.properties.sql.dialect.sqlite

import java.sql.PreparedStatement
import java.sql.ResultSet

@PublishedApi internal open class EnumConverter<E : Enum<E>>(
        javaType: Class<E>,
        sqlType: String
) : SimpleConverter<E>(javaType, sqlType, false) {

    override fun bind(statement: PreparedStatement, index: Int, value: E) {
        statement.setString(1 + index, asString(value))
    }

    override fun get(resultSet: ResultSet, index: Int): E =
            lookup(resultSet.getString(1 + index))

    protected open fun lookup(name: String): E = java.lang.Enum.valueOf<E>(javaType, name)
    protected open fun asString(value: E): String = value.name

}
