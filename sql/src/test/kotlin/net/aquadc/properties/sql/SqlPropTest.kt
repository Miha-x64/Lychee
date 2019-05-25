package net.aquadc.properties.sql

import net.aquadc.persistence.struct.build
import net.aquadc.persistence.struct.transaction
import net.aquadc.properties.ChangeListener
import net.aquadc.properties.addUnconfinedChangeListener
import net.aquadc.properties.testing.assertReturnsGarbage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.sql.SQLException


open class SqlPropTest {

    open val session: Session get() = jdbcSession

    private val someDao get() = session[SomeTable]

    @Test fun record() = assertReturnsGarbage {
        val rec = session.createTestRecord()
        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = session[SomeTable].selectAll().value.single()
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

        session.withTransaction {
            rec[SomeSchema.C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        session.withTransaction { delete(rec) }

        rec
    }

    @Test fun count() = assertReturnsGarbage {
        val cnt = someDao.count()
        assertSame(cnt, someDao.count())
        assertEquals(0L, cnt.value)
        val rec = session.createTestRecord()
        assertEquals(1L, cnt.value)
        session.withTransaction { delete(rec) }
        assertEquals(0L, cnt.value)
        cnt
    }

    @Test fun select() = assertReturnsGarbage {
        val sel = someDao.selectAll()
        assertSame(sel, someDao.selectAll())
        assertEquals(emptyList<Nothing>(), sel.value)
        val rec = session.createTestRecord()
        assertEquals(listOf(rec), sel.value)
        session.withTransaction { delete(rec) }
        assertEquals(emptyList<Nothing>(), sel.value)
        sel
    }

    @Test fun selectConditionally() = assertReturnsGarbage {
        val rec = session.createTestRecord()
        val sel = someDao.select(SomeSchema.A eq "first", SomeSchema.A.asc)
        assertSame(sel, someDao.select(SomeSchema.A eq "first", SomeSchema.A.asc))
        session.withTransaction { delete(rec) }
        sel
    }

    @Test fun selectNone() = assertReturnsGarbage {
        val rec = session.createTestRecord()
        val sel = someDao.select(SomeSchema.A notEq "first")
        assertSame(sel, someDao.select(SomeSchema.A notEq "first"))
        assertEquals(emptyList<Nothing>(), sel.value)
        session.withTransaction { delete(rec) }
        sel
    }


    @Test fun transactionalWrapper() {
        val originalRec = session.createTestRecord()
        val rec = originalRec.transactional()

        assertEquals("first", rec[SomeSchema.A])
        assertEquals(2, rec[SomeSchema.B])
        assertEquals(3, rec[SomeSchema.C])

        val sameRec = session[SomeTable].selectAll().value.single()
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
            it[C] = 100
        }

        assertEquals(100, rec[SomeSchema.C])
        assertEquals(100, sameRec[SomeSchema.C])

        assertEquals(2, called)

        session.withTransaction {
            delete(originalRec)
        }
    }


    @Test fun pkAsField() {
        val rec = session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 19
                it[Value] = "zzz"
            })
        }
        assertEquals(19L, rec.primaryKey)
        assertEquals(19L, rec[SchWithId.Id])
        assertEquals("zzz", rec[SchWithId.Value])
    }

    open val duplicatePkExceptionClass: Class<*> = SQLException::class.java

    fun `can't insert twice with the same PK in one transaction`() {
        try {
            session.withTransaction {
                insert(TableWithId, SchWithId.build {
                    it[Id] = 44
                    it[Value] = "yyy"
                })
                insert(TableWithId, SchWithId.build {
                    it[Id] = 44
                    it[Value] = "zzz"
                })
            }
        } catch (e: Exception) {
            if (!duplicatePkExceptionClass.isInstance(e)) {
                fail()
            }
        }
    }

    fun `can't insert twice with the same PK in different transactions`() {
        try {
            session.withTransaction {
                insert(TableWithId, SchWithId.build {
                    it[Id] = 44
                    it[Value] = "yyy"
                })
            }
            session.withTransaction {
                insert(TableWithId, SchWithId.build {
                    it[Id] = 44
                    it[Value] = "zzz"
                })
            }
        } catch (e: Exception) {
            if (!duplicatePkExceptionClass.isInstance(e)) {
                fail()
            }
        }
    }

    @Test fun `poisoned statement evicted`() {
        `can't insert twice with the same PK in one transaction`()
        // now the statement may be poisoned

        session.withTransaction {
            insert(TableWithId, SchWithId.build {
                it[Id] = 86
                it[Value] = "aaa"
            })
        }
    }

}

// TODO: .shapshots() should change only one time per transaction
