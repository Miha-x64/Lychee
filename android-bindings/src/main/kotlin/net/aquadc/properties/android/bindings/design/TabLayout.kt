package net.aquadc.properties.android.bindings.design

import android.support.design.widget.TabLayout
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.android.bindings.bindViewTo
import net.aquadc.properties.android.simple.design.SimpleOnTabSelectedListener


/**
 * Creates tabs from [values] array with help of [configureTab].
 * Binds tab selection with [prop] value.
 */
inline fun <T> TabLayout.populateAndBindTabsBidirectionally(
        prop: MutableProperty<T>,
        values: Array<T>,
        configureTab: TabLayout.Tab.(T) -> Unit
) {
    val value = prop.value
    values.forEach { t -> addTab(newTab().also { tab ->
        if (t == value) tab.select()
        configureTab(tab, t)
    }) }

    bindTabsBidirectionally(prop, values)
}

fun <T> TabLayout.bindTabsBidirectionally(
        prop: MutableProperty<T>,
        values: Array<T>
) {
    addOnTabSelectedListener(object : SimpleOnTabSelectedListener() {
        override fun onTabSelected(tab: TabLayout.Tab) {
            prop.value = values[tab.position]
        }
    })

    bindViewTo(prop) { getTabAt(values.indexOf(it))!!.select() }
}
