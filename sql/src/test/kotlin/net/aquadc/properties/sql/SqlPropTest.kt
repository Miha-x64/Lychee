package net.aquadc.properties.sql

import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import org.junit.Assert.*
import org.junit.Test
import java.sql.DriverManager


class SqlPropTest {

    private val sess = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
        val stmt = conn.createStatement()
        stmt.execute(SqliteDialect.createTable(SomeTable))
        stmt.close()
    }, SqliteDialect)

    @Test fun recordTest() {
        val rec = createTestRecord()

        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = sess[SomeTable].selectAll().value.single()
        assertSame(rec, sameRec) // DAO reuses and manages the only Record per table row

        val prop = rec prop SomeSchema.C
        val sameProp = sameRec prop SomeSchema.C
        assertSame(prop, sameProp)

        var called = 0
        val listener: ChangeListener<Long> = { old, new ->
            assertEquals(3, old)
            assertEquals(100, new)
            called++
        }
        prop.addUnconfinedChangeListener(listener)
        sameProp.addUnconfinedChangeListener(listener)

        sess.withTransaction {
            rec[SomeSchema.C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        sess.withTransaction {
            delete(rec)
        }
    }

    @Test fun transactionalWrapperTest() {
        val originalRec = createTestRecord()
        val rec = originalRec.transactional()

        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = sess[SomeTable].selectAll().value.single()
        assertNotSame(rec, sameRec)
        assertEquals(rec, sameRec) // same data represented through different interfaces

        val prop = rec prop SomeSchema.C
        val sameProp = sameRec prop SomeSchema.C
        assertNotSame(prop, sameProp)
        assertEquals(prop.value, sameProp.value) // same here: different objects, same data

        var called = 0
        val listener: ChangeListener<Long> = { old, new ->
            assertEquals(3, old)
            assertEquals(100, new)
            called++
        }
        prop.addUnconfinedChangeListener(listener)
        sameProp.addUnconfinedChangeListener(listener)

        rec.transaction {
            it[SomeSchema.C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        sess.withTransaction {
            delete(originalRec)
        }
    }

    private fun createTestRecord() =
            sess.withTransaction {
                insert(SomeTable, SomeSchema.build {
                    it[A] = "first"
                    it[B] = 2
                    it[C] = 3
                })
            }

}
