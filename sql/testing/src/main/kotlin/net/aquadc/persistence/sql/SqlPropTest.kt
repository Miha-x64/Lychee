package net.aquadc.persistence.sql

import net.aquadc.persistence.extended.Partial
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.Eagerly
import net.aquadc.persistence.sql.template.Query
import net.aquadc.persistence.struct.invoke
import net.aquadc.persistence.type.i32
import net.aquadc.persistence.type.string
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.AssumptionViolatedException
import org.junit.Test
import java.io.Closeable
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

    @Test fun `can't insert twice with the same PK in different transactions`() {
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

    @Test fun crud() {
        val id = session.withTransaction {
            insert(TableWithId, SchWithId {
                it[Id] = 136
                it[Value] = "aaa"
            })
        }

        session.withTransaction {
            update(TableWithId, id, SchWithId.Partial {
                it[Value] = "bbb"
            })
        }

        assertEquals(
            "bbb",
            Query("""SELECT "value" FROM with_id WHERE _id = ?""", i32, Eagerly.cell<Closeable, String>(string))
                .invoke(session as Session<Blocking<Closeable>>, id)
        )
    }

}
