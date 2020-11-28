@file:JvmName("ViewAnimatorBindings")
package net.aquadc.properties.android.bindings.widget

import android.widget.ViewAnimator
import net.aquadc.properties.Property
import net.aquadc.properties.android.bindings.bindViewTo


fun ViewAnimator.bindDisplayedChildTo(indexProperty: Property<Int>): Unit =
    bindViewTo(indexProperty) { v, i ->
        if (v.displayedChild != i) // check this ourselves, otherwise ViewSwitcher will animate uselessly
            displayedChild = i
    }
