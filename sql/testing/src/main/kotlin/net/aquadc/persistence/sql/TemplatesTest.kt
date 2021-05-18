package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.tuple.Tuple
import net.aquadc.persistence.extended.tuple.component1
import net.aquadc.persistence.extended.tuple.component2
import net.aquadc.persistence.extended.tuple.invoke
import net.aquadc.persistence.extended.tuple.times
import net.aquadc.persistence.sql.ColMeta.Companion.embed
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.blocking.Eagerly.execute
import net.aquadc.persistence.sql.blocking.Eagerly.executeForInsertedKey
import net.aquadc.persistence.sql.blocking.Eagerly.executeForRowCount
import net.aquadc.persistence.sql.blocking.Lazily
import net.aquadc.persistence.sql.template.Mutation
import net.aquadc.persistence.sql.template.Query
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
            val kek = Query("SELECT ? || 'kek'", string, cell<CUR, String>(string))
            assertEquals("lolkek", session.kek("lol"))
        }
        Lazily.run {
            val kek = Query("SELECT ? || 'kek'", string, cell<CUR, String>(string))
            assertEquals("lolkek", session.kek("lol").value)
        }
    }
    @Test fun <CUR> noCell() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            try { session.(Query("SELECT 0 LIMIT 0", cell<CUR, String>(string)))(); fail() }
            catch (expected: NoSuchElementException) {}

            assertEquals("fallback", session.(Query("SELECT 0 LIMIT 0", cell<CUR, String>(string) { "fallback" }))())
        }
        Lazily.run {
            try { session.(Query("SELECT 0 LIMIT 0", cell<CUR, String>(string)))().value; fail() }
            catch (expected: NoSuchElementException) {}

            assertEquals("fallback", session.(Query("SELECT 0 LIMIT 0", cell<CUR, String>(string) { "fallback" }))().value)
        }
    }

    @Test fun <CUR> col() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            val one = Query("SELECT 1", col<CUR, Int>(i32))
            assertEquals(listOf(1), session.one())
        }
        Lazily.run {
            val one = Query("SELECT 1", col<CUR, Int>(i32))
            assertEquals(listOf(1), Sequence { session.one() }.toList())
        }
    }

    @Test fun <CUR> row() {
        val session = session as Session<Blocking<CUR>>
        Eagerly.run {
            val sumAndMul = Query(
                "SELECT ? + ?, ? * ?", i32, i32, i32, i32,
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
            )
            val (f, s) = session.sumAndMul(80, 4, 6, 8)
            assertEquals(Pair(84, 48), Pair(f, s))
        }
        Lazily.run {
            val sumAndMul = Query(
                "SELECT ? + ?, ? * ?", i32, i32, i32, i32,
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(i32 * i32), BindBy.Position)
            )
            session.sumAndMul(80, 4, 6, 8).use {
                val (f, s) = it
                assertEquals(Pair(84, 48), Pair(f, s))
            }
        }
    }

    @Test fun <CUR> noRow() {
        val session = session as Session<Blocking<CUR>>
        val intPair = i32 * i32
        Eagerly.run {
            try { session.(Query(
                "SELECT 0, 0 LIMIT 0",
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position)
            ))(); fail() } catch (expected: NoSuchElementException) {}

            val (f, s) = session.(Query("SELECT 0, 0 LIMIT 0",
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
            ))()
            assertEquals(1, f)
            assertEquals(2, s)
        }
        Lazily.run {
            try { session.(Query("SELECT 0, 0 LIMIT 0",
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position)
            ))(); fail() } catch (expected: NoSuchElementException) {}

            session.(Query(
                "SELECT 0, 0 LIMIT 0",
                struct<CUR, Tuple<Int, DataType.NotNull.Simple<Int>, Int, DataType.NotNull.Simple<Int>>>(projection(intPair), BindBy.Position) { intPair(1, 2) }
            ))().use {
                val (f, s) = it
                assertEquals(1, f)
                assertEquals(2, s)
            }
        }
    }

    @Test fun <CUR> join() {
        val session = session as Session<Blocking<CUR>>
        val johnPk = session.withTransaction {
            val johnPk = insert(UserTable, User("John", "john@doe.com"))
            insert(ContactTable, Contact("@johnDoe", johnPk))
            johnPk
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
            val userContact = Query(
                USER_BY_NAME, string,
                struct<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contact = session.userContact("John")
            assertEquals(expectedJohn, contact)

            val userContacts = Query(USERS_BY_NAME_AND_EMAIL_START, string, string,
                structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            val contacts = session.userContacts("John", "john")
            assertEquals(listOf(expectedJohn), contacts)
        }
        Lazily.run {
            val userContact = Query(
                USER_BY_NAME, string,
                struct<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            session.userContact("John").use { he ->
                assertEquals(expectedJohn, he)
            }

            val userContacts = Query(
                USERS_BY_NAME_AND_EMAIL_START, string, string,
                structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            session.userContacts("John", "john").use { iter ->
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            session.userContacts("John", "john").use { iter ->
                assertTrue(iter.hasNext()) // any number of `hasNext`s must be OK
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertTrue(iter.hasNext())
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            val transientUserContacts = Query(
                USERS_BY_NAME_AND_EMAIL_START, string, string,
                structs<CUR, Tuple<Struct<Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>>, Tuple<String, DataType.NotNull.Simple<String>, String, DataType.NotNull.Simple<String>>, Struct<Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>, Tuple<String, DataType.NotNull.Simple<String>, Long, DataType.NotNull.Simple<Long>>>>(joined, BindBy.Name)
            )
            session.transientUserContacts("John", "john").use { iter -> // don't collect TemporaryStructs!
                assertEquals(expectedJohn, iter.next())
                assertFalse(iter.hasNext())
            }

            session.transientUserContacts("John", "john").use { iter ->
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

    @Test fun <CUR> `same endianness`() {
        val sqlOr = Query("SELECT ? | ? | ? | ?", i64, i64, i64, i64, Eagerly.cell<CUR, Long>(i64))
        assertEquals((1L shl 48) or (2L shl 32) or (3L shl 16) or 4L, (session as Session<Blocking<CUR>>).sqlOr(1L shl 48, 2L shl 32, 3L shl 16, 4L))
        assertEquals(-1, (session as Session<Blocking<CUR>>).sqlOr(65535L shl 48, 65535L shl 32, 65535L shl 16, 65535L))
    }

    @Test fun <CUR> triggers() {
        val session = session as Session<Blocking<CUR>>
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
                        Triple(1, 1, 0),
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
            /*assertEquals(1L, */insert(UserTable, User("A", "b"))/*)*/
        }

        val insertUser = Mutation<Blocking<CUR>, String, String, Long>(
            "INSERT INTO ${UserTable.name} (${User.run { First.name }}, ${User.run { Second.name }}) VALUES (?, ?)",
            string, string,
            executeForInsertedKey(UserTable.idColType)
        )
        val renameUser4Count = Mutation<Blocking<CUR>, String, String, Int>(
            "UPDATE ${UserTable.name} SET ${User.run { First.name }} = ? WHERE ${User.run { Second.name }} = ?",
            string, string,
            executeForRowCount()
        )
        val renameUser4Unit = Mutation<Blocking<CUR>, String, Unit>("UPDATE ${UserTable.name} SET ${User.run { First.name }} = ?", string, execute())

        session.withTransaction {
            assertEquals(2L, insertUser("qwe", "asd"))
            assertEquals(1, renameUser4Count("X", "b"))
        }

        assertEquals(2, called)
        insUpdListener.close()

        session.withTransaction { // assert no calls after disposal
            renameUser4Unit("Y")
            insert(UserTable, User("A", "b"))
        }

        called = 0
        val delListener = session.observe(UserTable to TriggerEvent.DELETE) { report ->
            when (called++) {
                0 -> {
                    val userChanges = report.of(UserTable)
                    assertEquals(
                        Triple(0, 0, 3),
                        Triple(userChanges.inserted.size, userChanges.updated.size, userChanges.removed.size)
                    )
                }
                else ->
                    throw AssertionError()
            }
        }
        session.withTransaction {
            session.(Query("SELECT ${UserTable.idColName} FROM ${UserTable.name}", Eagerly.col<CUR, Long>(i64)))()
                .forEach { id -> delete(UserTable, id) }
        }
        assertEquals(1, called)
        delListener.close()
    }

}

