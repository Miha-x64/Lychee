
This module contains some utility methods to work with persistence and I/O streams.

### [.type package](/persistence/src/main/kotlin/net/aquadc/persistence/type)

Simplifies and generalises Java and Kotlin type systems.
Provides APIs to represent (reify) types as objects.

### [.stream package](/persistence/src/main/kotlin/net/aquadc/persistence/stream)

Contains `BetterDataInput/Output` — interfaces which enhance Java's `DataInput/Output`,
`DataStreams` — implementations for `DataInput/OutputStream`;
there's also `ParcelIo` in `:android-bindings` module.

### [.struct package](/persistence/src/main/kotlin/net/aquadc/persistence/struct)

Contains tools for defining `Struct`s — data bags
which can be easily introspected and (de)serialized.

### [.tokens package](/persistence/src/main/kotlin/net/aquadc/persistence/tokens)

`TokenStream` interface and tools to generate streams
and to bind them to `Struct`s or other `DataType`s.

#### Goals

* Provide common interface for persisting and transferring data.
* Make developing CRUDs faster and easier.

#### Non-goals

* Support persisting/transferring of only intersection of types supported by popular APIs.
  This would be not enough. For example, text formats have no efficient ways to represent binary data —
  we use Base64 with them instead of totally prohibiting this.
  SQLite has no support for storing arrays or maps — but we can flatten them into byte arrays and store as BLOBs.
* Support transfer/persistence APIs fully.
  This would include supporting very specific, platform-dependent, vendor-locked, or rarely used types.
  The truth is somewhere in between.

#### What about kotlinx.serialization and other serialization frameworks and infrastructures?

Well, in the real life, data may have different representations:
* in-memory objects
* serialized tree on wire — JSON, CBOR, Protobuf, FlatBuffers, Cap'n'Proto, etc
* relational data in a powerful and full-featured SQL database
* simplified relational data in local SQLite database of a client app

All these storages could use different schemas to represent data.
There's no easy drop-in solution to store existing JSON tree relationally and vice versa.
We should honour every storage we use, thus,
the idea that a class could easily acquire serialization capability is just wrong.

Lychee introduces `Struct`s which are objects with all fields serializable,
and you are free to adapt `Schema`s to storages as you wish:
* `TokenStream` provides transforms to change JSON schema while [avoiding mappers](https://blog.jooq.org/2019/11/13/stop-mapping-stuff-in-your-middleware-use-sqls-xml-or-json-operators-instead/)
* SQL `Table` provides relations declared outside of `Schema` itself.
  This means that you can store `Structs` of the same `Schema` in different tables
  providing potentially different relation set
* SQL `Session` should in future support executing raw queries allowing you to join anything you want
  and load it into memory directly, without N+1s and round tripping
  
