
This module contains some utility methods to work with persistence and I/O streams.

### [.type package](/persistence/src/main/kotlin/net/aquadc/persistence/type)

Simplifies and generalises Java and Kotlin type systems.

### [.stream package](/persistence/src/main/kotlin/net/aquadc/persistence/stream)

Contains CleverDataInput/Output — interfaces which enhance Java's DataInput/Output,
CleverDataInputStream/OutputStream — implementations on top of DataInputStream/OutputStream,
ParcelInput/Output — Clever* implementations on top of Android's Parcel.

### [.struct package](/persistence/src/main/kotlin/net/aquadc/persistence/struct)

Contains tools for defining `struct`s in Kotlin.

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
