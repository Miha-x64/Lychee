package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.component1
import net.aquadc.persistence.extended.component2
import net.aquadc.persistence.extended.invoke
import net.aquadc.persistence.extended.times
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.blocking.Lazily
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


open class TemplatesTest {

    open val session: Session<out Blocking<out AutoCloseable>> get() = jdbcSession

    @Test fun <CUR : AutoCloseable> cell() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            val kek = session.query("SELECT ? || 'kek'", string, cell<CUR, String>(string))
            assertEquals("lolkek", kek("lol"))
        }
        Lazily.run {
            val kek = session.query("SELECT ? || 'kek'", string, cell<CUR, String>(string))
            assertEquals("lolkek", kek("lol").value)
        }
    }

    @Test fun <CUR : AutoCloseable> col() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            val one = session.query("SELECT 1", col<CUR, Int>(i32))
            assertEquals(listOf(1), one())
        }
        Lazily.run {
            val one = session.query("SELECT 1", col<CUR, Int>(i32))
            assertEquals(listOf(1), Sequence { one() }.toList())
        }
    }

    @Test fun <CUR : AutoCloseable> row() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            val sumAndMul = session.query(
                    "SELECT ? + ?, ? * ?", i32, i32, i32, i32,
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
            )
            val (f, s) = sumAndMul(80, 4, 6, 8)
            assertEquals(Pair(84, 48), Pair(f, s))
        }
        Lazily.run {
            val sumAndMul = session.query(
                    "SELECT ? + ?, ? * ?", i32, i32, i32, i32,
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
            )
            val (f, s) = sumAndMul(80, 4, 6, 8).value
            assertEquals(Pair(84, 48), Pair(f, s))
        }
    }

    @Test fun <CUR : AutoCloseable> join() {
        val session = session as Session<Blocking<CUR>>
        val johnPk = session.withTransaction {
            val john = insert(UserTable, User("John", "john@doe.com"))
            insert(ContactTable, Contact("@johnDoe", john.primaryKey))
            john.primaryKey
        }

        val userAndContact = Tuple("u", User, "c", Contact)
        //  ^^^^^^^^^^^^^^ should inline this variable after inference fix
        val joined = projection(userAndContact) { arrayOf(
                Relation.Embedded(NestingCase, First)
              , Relation.Embedded(NestingCase, Second)
        ) }

        val USER_BY_NAME = "SELECT u._id as 'u.id', u.name as 'u.name', u.email as 'u.email'," +
                "c._id as 'c.id', c.value as 'c.value', c.user_id as 'c.user_id' " +
                "FROM users u INNER JOIN contacts c ON u._id = c.user_id WHERE u.name = ? LIMIT 1"
        val USERS_BY_NAME_AND_EMAIL_START = "SELECT u._id as 'u.id', u.name as 'u.name', u.email as 'u.email'," +
                "c._id as 'c.id', c.value as 'c.value', c.user_id as 'c.user_id' " +
                "FROM users u INNER JOIN contacts c ON u._id = c.user_id WHERE u.name = ? AND u.email LIKE (? || '%') LIMIT 1"
        val expectedJohn = joined.schema(
                User("John", "john@doe.com"),
                Contact("@johnDoe", johnPk)
        )
        Eagerly.run {
            val userContact = session.query(
                    USER_BY_NAME, string,
                    struct<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contact = userContact("John")
            assertEquals(expectedJohn, contact)

            val userContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contacts = userContacts("John", "john")
            assertEquals(listOf(expectedJohn), contacts)
        }
        Lazily.run {
            val userContact = session.query(
                    USER_BY_NAME, string,
                    struct<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contact = userContact("John").value
            assertEquals(expectedJohn, contact)

            val userContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            val iter = userContacts("John", "john") // don't collect TemporaryStructs!
            assertTrue(iter.hasNext())
            assertEquals(expectedJohn, iter.next())
            assertFalse(iter.hasNext())
        }
    }

}

