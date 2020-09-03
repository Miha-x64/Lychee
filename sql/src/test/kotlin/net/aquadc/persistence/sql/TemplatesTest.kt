package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Tuple
import net.aquadc.persistence.extended.component1
import net.aquadc.persistence.extended.component2
import net.aquadc.persistence.extended.invoke
import net.aquadc.persistence.extended.times
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.blocking.Lazily
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.struct.asFieldSet
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.i64
import net.aquadc.persistence.type.string
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test


abstract class TemplatesTest {

    protected lateinit var session: Session<out Blocking<*>>

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
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
            )
            val (f, s) = sumAndMul(80, 4, 6, 8)
            assertEquals(Pair(84, 48), Pair(f, s))
        }
        Lazily.run {
            val sumAndMul = session.query(
                    "SELECT ? + ?, ? * ?", i32, i32, i32, i32,
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
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
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position)
            )(); fail() } catch (expected: NoSuchElementException) {}

            val (f, s) = session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
            )()
            assertEquals(1, f)
            assertEquals(2, s)
        }
        Lazily.run {
            try { session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position)
            )(); fail() } catch (expected: NoSuchElementException) {}

            session.query(
                    "SELECT 0, 0 LIMIT 0",
                    struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
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
            embed(NestingCase, First)
              , embed(NestingCase, Second)
        ) }

        val USER_BY_NAME =
            """SELECT u.name as "u.name", u.email as "u.email", c.value as "c.value", c.user_id as "c.user_id"
            FROM users u INNER JOIN contacts c ON u._id = c.user_id WHERE u.name = ? LIMIT 1"""
        val USERS_BY_NAME_AND_EMAIL_START =
            """SELECT u.name as "u.name", u.email as "u.email", c.value as "c.value", c.user_id as "c.user_id"
            FROM users u INNER JOIN contacts c ON u._id = c.user_id WHERE u.name = ? AND u.email LIKE (? || '%') LIMIT 1"""
        val expectedJohn = joined.schema(
                User("John", "john@doe.com"),
                Contact("@johnDoe", johnPk)
        )
        Eagerly.run {
            val userContact = session.query(
                    USER_BY_NAME, string,
                    struct<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contact = userContact("John")
            assertEquals(expectedJohn, contact)

            val userContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contacts = userContacts("John", "john")
            assertEquals(listOf(expectedJohn), contacts)
        }
        Lazily.run {
            val userContact = session.query(
                    USER_BY_NAME, string,
                    struct<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            userContact("John").use { he ->
                assertEquals(expectedJohn, he)
            }

            val userContacts = session.query(
                    USERS_BY_NAME_AND_EMAIL_START, string, string,
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
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
                    structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
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

    @Test fun <CUR> `same endianness`() {
        val sqlOr = (session as Session<Blocking<CUR>>)
            .query("SELECT ? | ? | ? | ?", i64, i64, i64, i64, Eagerly.cell<CUR, Long>(i64))
        assertEquals((1L shl 48) or (2L shl 32) or (3L shl 16) or 4L, sqlOr(1L shl 48, 2L shl 32, 3L shl 16, 4L))
        assertEquals(-1, sqlOr(65535L shl 48, 65535L shl 32, 65535L shl 16, 65535L))
    }

    @Test fun triggers() {
        var called = 0
        val insUpdListener = session.observe(UserTable to TriggerEvent.INSERT, UserTable to TriggerEvent.UPDATE) { report ->
            when (called++) {
                0 -> {
                    val userChanges = report.of(UserTable)
                    assertEquals(
                        Triple(1, 0, 0),
                        Triple(userChanges.inserted.size, userChanges.updated.size, userChanges.removed.size)
                    )
                }
                1 -> {
                    val userChanges = report.of(UserTable)
                    assertEquals(
                        Triple(0, 1, 0),
                        Triple(userChanges.inserted.size, userChanges.updated.size, userChanges.removed.size)
                    )

                    val pk = userChanges.updated.single()
                    assertEquals(User.First.asFieldSet().bitSet, userChanges.updatedFields(pk).bitSet)
                    assertArrayEquals(arrayOf(User.First), userChanges.updatedColumns(pk))
                }
                else ->
                    throw AssertionError()
            }
        }
        session.withTransaction {
            insert(UserTable, User("A", "b"))
        }
        session.withTransaction {
            session[UserTable].selectAll().value.single()[User.First] = "X"
        }

        assertEquals(2, called)
        insUpdListener.close()

        session.withTransaction { // assert no calls after disposal
            session[UserTable].selectAll().value.single()[User.First] = "Y"
            insert(UserTable, User("A", "b"))
        }

        called = 0
        val delListener = session.observe(UserTable to TriggerEvent.DELETE) { report ->
            when (called++) {
                0 -> {
                    val userChanges = report.of(UserTable)
                    assertEquals(
                        Triple(0, 0, 2),
                        Triple(userChanges.inserted.size, userChanges.updated.size, userChanges.removed.size)
                    )
                }
                else ->
                    throw AssertionError()
            }
        }
        session.withTransaction {
            session[UserTable].selectAll().value.forEach(::delete)
        }
        assertEquals(1, called)
        delListener.close()
    }

}

