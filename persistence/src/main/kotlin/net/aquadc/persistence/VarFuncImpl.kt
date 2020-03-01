package net.aquadc.persistence

import androidx.annotation.RestrictTo

// TODO: looks like I should create a separate utility library for such things
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class VarFuncImpl<T, R> :
        () -> R
      , (T) -> R
      , (T, T) -> R
      , (T, T, T) -> R
      , (T, T, T, T) -> R
      , (T, T, T, T, T) -> R
      , (T, T, T, T, T, T) -> R
      , (T, T, T, T, T, T, T) -> R
      , (T, T, T, T, T, T, T, T) -> R
{

    abstract fun invokeUnchecked(vararg arg: T): R

    override fun invoke(): R =
            invokeUnchecked()

    override fun invoke(p1: T): R =
            invokeUnchecked(p1)

    override fun invoke(p1: T, p2: T): R =
            invokeUnchecked(p1, p2)

    override fun invoke(p1: T, p2: T, p3: T): R =
            invokeUnchecked(p1, p2, p3)

    override fun invoke(p1: T, p2: T, p3: T, p4: T): R =
            invokeUnchecked(p1, p2, p3, p4)

    override fun invoke(p1: T, p2: T, p3: T, p4: T, p5: T): R =
            invokeUnchecked(p1, p2, p3, p4, p5)

    override fun invoke(p1: T, p2: T, p3: T, p4: T, p5: T, p6: T): R =
            invokeUnchecked(p1, p2, p3, p4, p5, p6)

    override fun invoke(p1: T, p2: T, p3: T, p4: T, p5: T, p6: T, p7: T): R =
            invokeUnchecked(p1, p2, p3, p4, p5, p6, p7)

    override fun invoke(p1: T, p2: T, p3: T, p4: T, p5: T, p6: T, p7: T, p8: T): R =
            invokeUnchecked(p1, p2, p3, p4, p5, p6, p7, p8)

}
