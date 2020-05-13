package net.aquadc.persistence.android

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.aquadc.persistence.sql.EmbedRelationsTest
import net.aquadc.persistence.sql.QueryBuilderTests
import net.aquadc.persistence.sql.Session
import net.aquadc.persistence.sql.SqlPropTest
import net.aquadc.persistence.sql.TemplatesTest
import net.aquadc.persistence.sql.TestTables
import net.aquadc.persistence.sql.blocking.Blocking
import net.aquadc.persistence.sql.blocking.SqliteSession
import net.aquadc.persistence.sql.dialect.sqlite.SqliteDialect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


private fun sqliteDb() = object : SQLiteOpenHelper(
        RuntimeEnvironment.application, "test.db", null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        TestTables.forEach { db.execSQL(SqliteDialect.createTable(it)) }
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException()
    }
}.writableDatabase


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SqlPropRoboTest : SqlPropTest() {

    // Robolectric Kills SQLite DB after each test, let's recreate it
    private lateinit var db: SQLiteDatabase
    override lateinit var session: Session<*>
    @Before fun init() {
        db = sqliteDb()
        session = SqliteSession(db)
    }
    @After fun close() {
        db.close()
    }

    override val duplicatePkExceptionClass: Class<*>
        get() = SQLiteConstraintException::class.java

    @Test fun `assert robolectric works`() {
        db.execSQL("CREATE TABLE test(value STRING)")
        db.execSQL("INSERT INTO test VALUES ('test value')")
        db.query("test", arrayOf("COUNT(*)"), null, null, null, null, null).let {
            check(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
        db.query("test", arrayOf("*"), null, null, null, null, null).let {
            check(it.moveToFirst())
            assertEquals(1, it.count)
            assertEquals(1, it.columnCount)
            assertEquals("test value", it.getString(0))
        }
    }

}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmbedRelationsRoboTest : EmbedRelationsTest() {
    private lateinit var db: SQLiteDatabase
    override lateinit var session: Session<*>
    @Before fun init() {
        db = sqliteDb()
        session = SqliteSession(db)
    }
    @After fun close() {
        db.close()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QueryBuilderRoboTests : QueryBuilderTests() {
    private lateinit var db: SQLiteDatabase
    override lateinit var session: Session<*>
    @Before fun init() {
        db = sqliteDb()
        session = SqliteSession(db)
    }
    @After fun close() {
        db.close()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TemplatesRoboTests : TemplatesTest() {
    private lateinit var db: SQLiteDatabase
    override lateinit var session: Session<out Blocking<*>>
    @Before fun init() {
        db = sqliteDb()
        session = SqliteSession(db)
    }
    @After fun close() {
        db.close()
    }
}
