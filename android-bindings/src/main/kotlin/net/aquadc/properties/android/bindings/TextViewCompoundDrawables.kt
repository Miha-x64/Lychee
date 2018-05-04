package net.aquadc.properties.android.bindings

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.annotation.IntDef
import android.view.Gravity.*
import android.view.View
import android.widget.TextView
import net.aquadc.properties.Property
import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.*


/**
 * Enumerates possible compound drawable positions.
 */
@SuppressLint("RtlHardcoded")
@Target(VALUE_PARAMETER, FUNCTION, LOCAL_VARIABLE, PROPERTY)
@Retention(SOURCE)
@IntDef(LEFT.toLong(), TOP.toLong(), RIGHT.toLong(), BOTTOM.toLong(), START.toLong(), END.toLong())
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
    inline fun to(property: Property<Drawable?>) = bind(property)

    /**
     * Binds drawable at [position] to the given [property]'s [DrawableRes] value.
     */
    @JvmName("toRes")
    inline fun to(property: Property<Int>) = bind(property)

    @PublishedApi
    internal fun bind(prop: Property<*>) {
        view.bindViewTo(prop, when (position) {
            LEFT -> left ?: SetCompoundDrawable(LEFT).also { left = it }
            TOP -> top ?: SetCompoundDrawable(TOP).also { top = it }
            RIGHT -> right ?: SetCompoundDrawable(RIGHT).also { right = it }
            BOTTOM -> bottom ?: SetCompoundDrawable(BOTTOM).also { bottom = it }
            START -> start ?: SetCompoundDrawable(START).also { start = it }
            END -> end?: SetCompoundDrawable(END).also { end = it }
            else -> throw AssertionError()
        })
    }

    private companion object {
        private var left: SetCompoundDrawable? = null
        private var top: SetCompoundDrawable? = null
        private var right: SetCompoundDrawable? = null
        private var bottom: SetCompoundDrawable? = null
        private var start: SetCompoundDrawable? = null
        private var end: SetCompoundDrawable? = null
    }

}

private class SetCompoundDrawable(
        @CompoundDrawablePosition
        private val position: Int
) : (TextView, Any?) -> Unit {

    @SuppressLint("RtlHardcoded")
    override fun invoke(view: TextView, drawable: Any?) {
        when (position) {
            LEFT -> setAbs(view, 0, drawable)
            TOP -> setAbs(view, 1, drawable)
            RIGHT -> setAbs(view, 2, drawable)
            BOTTOM -> setAbs(view, 3, drawable)
            START -> setRel(view, 0, drawable)
            END -> setRel(view, 2, drawable)
            else -> throw AssertionError()
        }
    }

    private fun setAbs(view: TextView, index: Int, drawable: Any?) {
        val drawables = view.compoundDrawables
        drawables.patch(view, index, drawable)
        view.setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    @TargetApi(17)
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
