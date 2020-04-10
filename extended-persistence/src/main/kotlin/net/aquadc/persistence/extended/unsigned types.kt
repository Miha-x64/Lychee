@file:JvmName("UnsignedTypes")
@file:UseExperimental(ExperimentalUnsignedTypes::class)
package net.aquadc.persistence.extended

// TODO: make DataType sealed, introduce Number(size | var, signed, floating), String, Blob subtypes,
// TODO: re-implement unsigned on top of it, add BigInteger and BigDecimal

@Deprecated("it was a really a bad idea to represent u8 as i16", level = DeprecationLevel.ERROR)
val u8: Nothing get() = throw AssertionError()
@Deprecated("renamed", ReplaceWith("u8"), level = DeprecationLevel.ERROR)
val uByte: Nothing get() = throw AssertionError()

@Deprecated("it was a really a bad idea to represent u16 as i32", level = DeprecationLevel.ERROR)
val u16: Nothing get() = throw AssertionError()
@Deprecated("renamed", ReplaceWith("u16"), level = DeprecationLevel.ERROR)
val uShort: Nothing get() = throw AssertionError()

@Deprecated("it was a really a bad idea to represent u32 as i64", level = DeprecationLevel.ERROR)
val u32: Nothing get() = throw AssertionError()
@Deprecated("renamed", ReplaceWith("u32"), level = DeprecationLevel.ERROR)
val uInt: Nothing get() = throw AssertionError()

// ULong cannot be expressed using a primitive type ¯\_(ツ)_/¯
