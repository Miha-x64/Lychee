package net.aquadc.persistence

import androidx.annotation.RestrictTo
import net.aquadc.persistence.struct.BaseStruct
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.type.nothing
import java.io.Closeable

interface CloseableIterator<out T> : Iterator<T>, Closeable
interface CloseableStruct<SCH : Schema<SCH>> : Struct<SCH>, Closeable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class IteratorAndTransientStruct<SCH : Schema<SCH>, R>(
        schema: SCH
): BaseStruct<SCH>(schema), CloseableIterator<R>, CloseableStruct<SCH> {
// he-he, like this weird iterator https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/util/MapCollections.java#74

    final override fun equals(other: Any?): Boolean =
            if (schema === NullSchema) this === other
            else super<BaseStruct>.equals(other)
    final override fun hashCode(): Int =
            if (schema === NullSchema) System.identityHashCode(this)
            else super<BaseStruct>.hashCode()
    final override fun toString(): String =
            if (schema === NullSchema) javaClass.name + '@' + Integer.toHexString(hashCode())
            else super<BaseStruct>.toString()

}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) object NullSchema : Schema<NullSchema>() {
    init { "" let nothing }
}
