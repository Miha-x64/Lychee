
Given a struct schema:

```kt
object Player : Schema<Player>() {
    val Name = "name" let string
    val Surname = "surname" let string
    val Score = "score".mut(i32, default = 0)
}
```

Declaring a table:

```kt
// the struct does not know anything about primary key
val PlayerTable = tableOf(
    schema = Player, name = "players",
    idColName = "_id", idColType = i64,
)

// or primary key is a part of schema
val PlayerTable = tableOf(
    schema = Player, name = "players", idCol = Player.Id,
)
```

Connecting to the database:
```kt
// in-memory SQLite database with JDBC, e.g. for unit-testing:
val session = JdbcSession(DriverManager.getConnection("jdbc:sqlite::memory:").also { conn ->
    val stmt = conn.createStatement()
    Tables.forEach {
        stmt.execute("CREATE TABLE ...")
    }
    stmt.close()
}, SqliteDialect)

// Android SQLite
val session = SqliteSession(object : SQLiteOpenHelper(context, "app.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE ...")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        throw UnsupportedOperationException()
    }
}.writableDatabase)

// server setup
val session = JdbcSession(hikariDataSource, PostgresDialect)
```

Inserting:
```kt
val playerRecord = session.mutate {
    insert(PlayerTable, Player { ... })
  
    // from JSON, for example
    insert(PlayerTable, "{...}".reader().json().tokens().iteratorOfTransient(Player))
}
```

## SQL templates

Define queries somewhere close to DB schema, invoke them closer to business:

```kt
val namesToEmails = Query(
    "SELECT u.name, c.email FROM users u " +
    "INNER JOIN contacts c ON u._id = c.user_id " +
    "LIMIT ? OFFSET ?", /*limit*/ i32, /*offset*/ i32,
    /* fetch as */ Lazily.structs(projection(string * string), BindBy.Position),
)
val renameByEmail = Mutation(
    "UPDATE users SET name = ? WHERE email = ?",
    string, string,
    Eagerly.executeForRowCount(),
)

...


session.namesToEmails(/*offset*/ 0, /*rowCount*/ 10).use {
    it.forEach { (name, email) ->
        
    }
}

session.mutate { // update in a transaction
    assertEquals(1, renameByEmail("java@sun.com", "Java™"))
}
```

Bind arguments with Lychee, get a Cursor/ResultSet:
```kt
val something = Query(
    "SELECT something FROM somewhere WHERE a = ? LIMIT ?",
    /*a*/ string, /*limit*/ i32,
    JdbcDb.resultSet() or SqliteDb.cursor(),
)

jdbcOrSqliteSession.something(a, limit) -> ResultSet or Cursor
```

Execute query on your own, bind result with Lychee:
```kt
gimmeResultSetOrCursor()
    .asStructIterator(PlayerTable, BindBy.Name)
    .forEach { playa ->
        println(playa)  
    }
```

NB: asking Lychee to separately execute query and bind the result is less efficient.
```kt
// <DON'T DO THIS>
val something = Query("SELECT * FROM players", JdbcDb.resultSet()) // DON'T DO THIS
jdbcSession.something().asStructIterator(PlayerTable) // DON'T DO THIS
// </DON'T DO THIS>

// Do this instead:
val something = Query("SELECT * FROM players", Lazily.structs(PlayerTable))
jdbcSession.something()
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