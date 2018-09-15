package net.aquadc.struct.converter

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

@PublishedApi internal open class EnumConverter<E : Enum<E>>(
        private val enumType: Class<E>,
        dataType: DataType
) : SimpleConverter<E>(dataType, false) {

    override fun bind(statement: PreparedStatement, index: Int, value: E) {
        statement.setString(1 + index, asString(value))
    }

    override fun get(resultSet: ResultSet, index: Int): E =
            lookup(resultSet.getString(1 + index))


    override fun bind(statement: SQLiteStatement, index: Int, value: E) {
        statement.bindString(1 + index, asString(value))
    }
    override fun asString(value: E): String = value.name
    override fun get(cursor: Cursor, index: Int): E = lookup(cursor.getString(index))

    protected open fun lookup(name: String): E = java.lang.Enum.valueOf<E>(enumType, name)

}
