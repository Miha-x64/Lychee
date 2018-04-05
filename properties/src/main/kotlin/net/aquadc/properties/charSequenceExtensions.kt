package net.aquadc.properties


inline val Property<CharSequence>.length get() = map { it.length }

inline val Property<CharSequence>.isEmpty get() = map { it.isEmpty() }
inline val Property<CharSequence>.isNotEmpty get() = map { it.isNotEmpty() }

inline val Property<CharSequence>.isBlank get() = map { it.isBlank() }
inline val Property<CharSequence>.isNotBlank get() = map { it.isNotBlank() }

inline val Property<CharSequence>.trimmed get() = map { it.trim() }
