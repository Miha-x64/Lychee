@file:JvmName("CompoundDrawableBindings")
package net.aquadc.properties.android.bindings.widget

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity.BOTTOM
import android.view.Gravity.END
import android.view.Gravity.LEFT
import android.view.Gravity.RIGHT
import android.view.Gravity.START
import android.view.Gravity.TOP
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.Binding
import net.aquadc.properties.android.bindings.bindViewToBinding
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER


/**
 * Enumerates possible compound drawable positions.
 */
@SuppressLint("RtlHardcoded")
@Target(VALUE_PARAMETER, FUNCTION, LOCAL_VARIABLE, PROPERTY)
@Retention(SOURCE)
@IntDef(LEFT, TOP, RIGHT, BOTTOM, START, END)
annotation class CompoundDrawablePosition


/**
 * Binds drawable at the given [position] to...
 * For [START] and [END] positions API 17+ required.
 * Usage: `tvWithIcon.bindDrawableAt(START).to(iconProp)`.
 */
@SuppressLint("RtlHardcoded")
fun TextView.bindDrawableAt(@CompoundDrawablePosition position: Int): DrawableBindingStub {
    if (Build.VERSION.SDK_INT < 17 && (position == START || position == END))
        throw IllegalArgumentException("START and END compound drawable positions are supported only by SDK 17+")

    return when (position) {
        LEFT, TOP, RIGHT, BOTTOM, START, END ->
            DrawableBindingStub(this, position)
        else ->
            throw IllegalArgumentException(
                    "Wrong position $position, must be one of the values listed by @CompoundDrawablePosition.")
    }
}


/**
 * A 'fluent' bridge for binding compound drawables to properties.
 */
@SuppressLint("RtlHardcoded")
@Suppress("NOTHING_TO_INLINE")
class DrawableBindingStub internal constructor(
        private val view: TextView,
        @CompoundDrawablePosition
        private val position: Int
) {

    /**
     * Binds drawable at [position] to the given [property]'s [Drawable] value.
     */
    inline fun to(property: Property<Drawable?>): Unit = bind(property)

    /**
     * Binds drawable at [position] to the given [property]'s [DrawableRes] value.
     */
    @Deprecated("renamed", ReplaceWith("this.toRes(property)"), DeprecationLevel.ERROR)
    @JvmName("toRes_overloaded")
    inline fun to(property: Property<Int>): Nothing = throw AssertionError()

    /**
     * Binds drawable at [position] to the given [property]'s [DrawableRes] value.
     */
    inline fun toRes(property: Property<Int>): Unit = bind(property)

    @PublishedApi
    internal fun bind(prop: Property<*>) {
        view.bindViewToBinding(prop, CompoundDrawableBinding(view, prop, position))
    }

}

private class CompoundDrawableBinding(
        view: TextView,
        property: Property<Any?>,
        @CompoundDrawablePosition private val position: Int
) : Binding<TextView, Any?>(view, property) {

    @SuppressLint("RtlHardcoded", "NewApi")
    override fun bind(view: TextView, value: Any?) {
        when (position) {
            LEFT -> setAbs(view, 0, value)
            TOP -> setAbs(view, 1, value)
            RIGHT -> setAbs(view, 2, value)
            BOTTOM -> setAbs(view, 3, value)
            START -> setRel(view, 0, value)
            END -> setRel(view, 2, value)
            else -> throw AssertionError()
        }
    }

    private fun setAbs(view: TextView, index: Int, drawable: Any?) {
        val drawables = view.compoundDrawables
        drawables.patch(view, index, drawable)
        view.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    @RequiresApi(17)
    private fun setRel(view: TextView, index: Int, drawable: Any?) {
        val drawables = view.compoundDrawablesRelative
        drawables.patch(view, index, drawable)
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                drawables[0], drawables[1], drawables[2], drawables[3]
        )
    }

    private fun Array<Drawable?>.patch(view: View, index: Int, drawable: Any?) {
        this[index] = when (drawable) {
            is Drawable? -> drawable
            is Int -> view.resources.getDrawableCompat(drawable)
            else -> throw AssertionError()
        }
    }
}
