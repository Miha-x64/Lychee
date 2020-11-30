package net.aquadc.persistence.sql

import net.aquadc.persistence.struct.invoke
import org.junit.Assert.fail
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.sql.SQLException


abstract class SqlPropTest {
    protected lateinit var session: Session<*>

    open val duplicatePkExceptionClass: Class<*> = SQLException::class.java

    fun `can't insert twice with the same PK in one transaction`() {
        try {
            session.withTransaction {
                insert(TableWithId, SchWithId {
                    it[Id] = 44
                    it[Value] = "yyy"
                })
                insert(TableWithId, SchWithId {
                    it[Id] = 44
                    it[Value] = "zzz"
                })
            }
        } catch (e: Exception) {
            if (e is AssumptionViolatedException) throw e
            if (!duplicatePkExceptionClass.isInstance(e)) {
                fail("expected:<" + duplicatePkExceptionClass.name + "> but was:<" + e.javaClass + ">")
            }
        }
    }

    fun `can't insert twice with the same PK in different transactions`() {
        try {
            session.withTransaction {
                insert(TableWithId, SchWithId {
                    it[Id] = 44
                    it[Value] = "yyy"
                })
            }
            session.withTransaction {
                insert(TableWithId, SchWithId {
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
            insert(TableWithId, SchWithId {
                it[Id] = 86
                it[Value] = "aaa"
            })
        }
    }

    private fun Session<*>.createTestRecord() =
        withTransaction {
            insert(SomeTable, SomeSchema {
                it[A] = "first"
                it[B] = 2
                it[C] = 3
            })
        }

}
