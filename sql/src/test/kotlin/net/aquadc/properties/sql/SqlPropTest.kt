package net.aquadc.properties.sql

import net.aquadc.persistence.struct.build
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.sql.dialect.sqlite.SqliteDialect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.sql.DriverManager


class SqlPropTest {

    private val sess = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
        val stmt = conn.createStatement()
        stmt.execute(SqliteDialect.createTable(SomeTable))
        stmt.close()
    }, SqliteDialect)

    @Test fun recordTest() {
        val rec = sess.withTransaction {
            insert(SomeTable, SomeSchema.build {
                it[SomeSchema.A] = "first"
                it[SomeSchema.B] = 2
                it[SomeSchema.C] = 3
            })
        }

        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = sess[SomeTable].selectAll().value.single()
        assertSame(rec, sameRec)

        val prop = rec prop SomeSchema.C
        val sameProp = sameRec prop SomeSchema.C
        assertSame(prop, sameProp) // DAO reuses and manages the only Record per table row

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
    }

}
