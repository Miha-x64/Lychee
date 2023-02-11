
Given a struct schema, e. g.

```kt
object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(int, default = 0)
}
```

Declaring a table is trivial:

```kt
// the struct does not know anything about primary key
val PlayerTable = SimpleTable(
    schema = Player, name = "players",
    idColName = "_id", idColType = long
)

// alternatively, when primary key is a part of schema
val PlayerTable = SimpleTable(
    schema = Player, name = "players", idCol = Player.Id
)

// hold all your tables to create all of them easily
val Tables = arrayOf(PlayerTable)
```

Creating a session:
```kt
// in-memory SQLite database with JDBC, e. g. for unit-testing:
val session = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
    val stmt = conn.createStatement()
    Tables.forEach {
        stmt.execute(SqliteDialect.createTable(it))
    }
    stmt.close()
}, SqliteDialect)

// Android SQLite
val session = SqliteSession(object : SQLiteOpenHelper(context, "app.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        Tables.forEach { db.execSQL(SqliteDialect.createTable(it)) }
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException()
    }
}.writableDatabase)

// server setup
JdbcSession(hikariDataSource, PostgresDialect)
```

Inserting:
```kt
val playerRecord = session.mutate {
    insert(PlayerTable, Player { ... })
}
```

## SQL templates
```kt
val namesToEmails = Query(
    "SELECT u.name, c.email FROM users u " +
    "INNER JOIN contacts c ON u._id = c.user_id " +
    "LIMIT ?, ?", /*offset*/ i32, /*rowCount*/ i32,
    /* fetch as */ structs(string * string, BindBy.Position)
)
namesToEmails(/*offset*/ 0, /*rowCount*/ 10).use {
    it.forEach { (name, email) ->
        
    }
}

val update = Mutation(
    "UPDATE users SET name = ? WHERE email = ?",
    string, string,
    executeForRowCount()
)

session.mutate { // update in a transaction
    assertEquals(1, update("java@sun.com", "Java™"))
}

// single statement transaction
session.update(...)
```



#### Thanks
* GreenDAO for [Query Builder](https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java)
  and performance tricks
* [Kwery](https://github.com/andrewoma/kwery) for the `Table` idea
* [Anko](https://github.com/Kotlin/anko/wiki/Anko-SQLite) for the other way of thinking
* [@y2k](https://github.com/y2k) for help in API design
* [Andrey Antipov](https://github.com/gorttar) for type inference hints
* [Denis Podlesnykh](https://github.com/denniselite) for help with triggers
* [TutGruzBot](https://bot.tutgruz.ru/) for server-side experience