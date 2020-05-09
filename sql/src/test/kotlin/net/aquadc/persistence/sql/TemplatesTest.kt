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
import org.junit.Assert.fail
import org.junit.Test


open class TemplatesTest {

    open val session: Session<out Blocking<*>> get() = jdbcSession

    @Test fun <CUR> cell() {
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
    @Test fun <CUR> noCell() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            try { session.query("SELECT 0 LIMIT 0", cell<CUR, String>(string))(); fail() }
            catch (expected: NoSuchElementException) {}

            assertEquals("fallback", session.query("SELECT 0 LIMIT 0", cell<CUR, String>(string) { "fallback" })())
        }
        Lazily.run {
            try { session.query("SELECT 0 LIMIT 0", cell<CUR, String>(string))().value; fail() }
            catch (expected: NoSuchElementException) {}

            assertEquals("fallback", session.query("SELECT 0 LIMIT 0", cell<CUR, String>(string) { "fallback" })().value)
        }
    }

    @Test fun <CUR> col() {
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

    @Test fun <CUR> row() {
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
            sumAndMul(80, 4, 6, 8).use {
                val (f, s) = it
                assertEquals(Pair(84, 48), Pair(f, s))
            }
        }
    }

    @Test fun <CUR> noRow() {
        val session = session as Session<Blocking<CUR>>
        val intPair = i32 * i32
        Eagerly.run {
            try { session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(intPair), BindBy.Position)
            )(); fail() } catch (expected: NoSuchElementException) {}

            val (f, s) = session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
            )()
            assertEquals(1, f)
            assertEquals(2, s)
        }
        Lazily.run {
            try { session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(intPair), BindBy.Position)
            )(); fail() } catch (expected: NoSuchElementException) {}

            session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.Simple<Int>, Int, DataType.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
            )().use {
                val (f, s) = it
                assertEquals(1, f)
                assertEquals(2, s)
            }
        }
    }

    @Test fun <CUR> join() {
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

        val USER_BY_NAME = "SELECT u.name as 'u.name', u.email as 'u.email'," +
                "c.value as 'c.value', c.user_id as 'c.user_id' " +
                "FROM users u INNER JOIN contacts c ON u._id = c.user_id WHERE u.name = ? LIMIT 1"
        val USERS_BY_NAME_AND_EMAIL_START = "SELECT u.name as 'u.name', u.email as 'u.email'," +
                "c.value as 'c.value', c.user_id as 'c.user_id' " +
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
            userContact("John").use { he ->
                assertEquals(expectedJohn, he)
            }

            val userContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            userContacts("John", "john").use { iter ->
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            userContacts("John", "john").use { iter ->
                assertTrue(iter.hasNext()) // any number of `hasNext`s must be OK
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            val transientUserContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>>, Tuple<String, DataType.Simple<String>, String, DataType.Simple<String>>, Struct<Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>, Tuple<String, DataType.Simple<String>, Long, DataType.Simple<Long>>>>(joined, BindBy.Name)
            )
            transientUserContacts("John", "john").use { iter -> // don't collect TemporaryStructs!
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            transientUserContacts("John", "john").use { iter ->
                assertTrue(iter.hasNext()) // any number of `hasNext`s must be OK
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }
        }
    }

    // todo left/right/outer join
    // TODO insert, update, delete

}

