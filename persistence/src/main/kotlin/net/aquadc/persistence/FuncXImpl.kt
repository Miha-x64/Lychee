package net.aquadc.persistence

import androidx.annotation.RestrictTo

// TODO: looks like I should create a separate utility library for such things
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class FuncXImpl<S, T, R> :
    (S) -> R
  , (S, T) -> R
  , (S, T, T) -> R
  , (S, T, T, T) -> R
  , (S, T, T, T, T) -> R
  , (S, T, T, T, T, T) -> R
  , (S, T, T, T, T, T, T) -> R
  , (S, T, T, T, T, T, T, T) -> R
  , (S, T, T, T, T, T, T, T, T) -> R
{

    abstract fun invokeUnchecked(receiver: S, vararg args: T): R

    final override fun invoke(receiver: S, ): R =
        invokeUnchecked(receiver, )

    final override fun invoke(receiver: S, p1: T): R =
        invokeUnchecked(receiver, p1)

    final override fun invoke(receiver: S, p1: T, p2: T): R =
        invokeUnchecked(receiver, p1, p2)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T): R =
        invokeUnchecked(receiver, p1, p2, p3)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T, p4: T): R =
        invokeUnchecked(receiver, p1, p2, p3, p4)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T, p4: T, p5: T): R =
        invokeUnchecked(receiver, p1, p2, p3, p4, p5)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T, p4: T, p5: T, p6: T): R =
        invokeUnchecked(receiver, p1, p2, p3, p4, p5, p6)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T, p4: T, p5: T, p6: T, p7: T): R =
        invokeUnchecked(receiver, p1, p2, p3, p4, p5, p6, p7)

    final override fun invoke(receiver: S, p1: T, p2: T, p3: T, p4: T, p5: T, p6: T, p7: T, p8: T): R =
        invokeUnchecked(receiver, p1, p2, p3, p4, p5, p6, p7, p8)

}
