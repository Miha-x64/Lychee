
Object-oriented observable active records and queries
(maturity: prototype)

Intended for use in client-side applications with SQLite database.
This implies:
* Many small queries (SQLite works well with it)
* Primary keys of long (or integer) type (intermediate HashMap will be created otherwise)
* ...


#### Thanks
* GreenDAO for typed queries and performance tricks
* Kwery for the `Table` idea
* Anko for the other way of thinking
* @y2k for help in API design
