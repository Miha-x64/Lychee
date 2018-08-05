
Object-oriented active record
(maturity: prototype)

Intended for use in client-side applications with SQLite database.
This implies:
* Many small queries (SQLite works well with it)
* Primary keys of long (or integer) type (intermediate HashMap will be created otherwise)
* ...
