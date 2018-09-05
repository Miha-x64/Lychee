
Object-oriented observable active records and queries
(maturity: prototype)

Intended for use in client-side applications with SQLite database.
This implies:
* Many small queries (SQLite works well with it)
* Primary keys of long (or integer) type (intermediate HashMap will be created otherwise)
* ...


#### Thanks
* GreenDAO for [Query Builder](https://github.com/greenrobot/greenDAO/blob/72cad8c9d5bf25d6ed3bdad493cee0aee5af8a70/DaoCore/src/main/java/org/greenrobot/greendao/Property.java)
  and performance tricks
* [Kwery](https://github.com/andrewoma/kwery) for the `Table` idea
* [Anko](https://github.com/Kotlin/anko/wiki/Anko-SQLite) for the other way of thinking
* [@y2k](https://github.com/y2k) for help in API design
