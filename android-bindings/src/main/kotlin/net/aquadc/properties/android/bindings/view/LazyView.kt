@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("LazyViews")
package net.aquadc.properties.android.bindings.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo

/**
 * Similar to [android.view.ViewStub].
 * Calls [createView] and replaces self with it
 * when [visibleProperty] becomes `true`.
 */
@SuppressLint("ViewConstructor")
class LazyView(
    context: Context,
    private val visibleProperty: Property<Boolean>,
    private val createView: ViewGroup.() -> View
) : View(context) {

    init {
        visibility = GONE
        setWillNotDraw(true)
        bindViewTo(visibleProperty) { lazy, visible ->
            if (visible) lazy.create()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(0, 0)
    }
    @SuppressLint("MissingSuperCall") override fun draw(canvas: Canvas?) {}
    override fun dispatchDraw(canvas: Canvas?) {}

    @JvmSynthetic internal fun create() {
        val parent = parent as ViewGroup
        val index = parent.indexOfChild(this)
        // this must detach us from window and unsubscribe from the property:
        parent.removeViewsInLayout(index, 1)

        val view = parent.createView()
        view.bindVisibilityHardlyTo(visibleProperty)

        // If a view has its own LP, just use them. Otherwise transfer our, if any.
        val ourLp = layoutParams
        if (view.layoutParams == null && ourLp != null) parent.addView(view, index, ourLp)
        else parent.addView(view, index)
    }

}

/**
 * Create new [LazyView] in this context.
 */
/*
Despite the purpose of this method looks strange, please note that
1. it's conceptually correct to have this overload
2. one may want to setContentView(lazyView { ... }) under certain circumstances
 */
inline fun Context.lazyView(visibleProperty: Property<Boolean>, crossinline createView: Context.() -> View): LazyView =
    LazyView(this, visibleProperty) { context.createView() }

/**
 * Create new [LazyView] for use within this ViewGroup.
 */
@Suppress("UNCHECKED_CAST")
inline fun <VG : ViewGroup> VG.lazyView(visibleProperty: Property<Boolean>, noinline createView: VG.() -> View): LazyView =
    LazyView(context, visibleProperty, createView as ViewGroup.() -> View)
