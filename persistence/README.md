
This module contains some utility methods to work with persistence and I/O streams.

### [.converter package](/persistence/src/main/kotlin/net/aquadc/persistence/converter)

Contains converters from Java types (primitive, strings, arrays)
to JDBC, Android SQLite, and readers/writers to/from input/output streams.

### [.stream package](/persistence/src/main/kotlin/net/aquadc/persistence/stream)

Contains CleverDataInput/Output — interfaces which enhance Java's DataInput/Output,
CleverDataInputStream/OutputStream — implementations on top of DataInputStream/OutputStream,
ParcelInput/Output — Clever* implementations on top of Android's Parcel.

### [.struct package](/persistence/src/main/kotlin/net/aquadc/persistence/struct)

Contains tools for defining `struct`s in Kotlin.
