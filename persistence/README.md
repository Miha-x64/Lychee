
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
