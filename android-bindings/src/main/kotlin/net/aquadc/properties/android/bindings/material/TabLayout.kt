@file:JvmName("TabLayoutBindings")
package net.aquadc.properties.android.bindings.material

import com.google.android.material.tabs.TabLayout
import net.aquadc.properties.MutableProperty
import net.aquadc.properties.android.bindings.Binding
import net.aquadc.properties.android.bindings.bindViewToBinding


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

/**
 * Binds currently selected tab to [prop] value and vice versa,
 * assuming that tabs correspond to [values].
 */
fun <T> TabLayout.bindTabsBidirectionally(
        prop: MutableProperty<T>,
        values: Array<T>
) {
    val listenerAndBinding = TabLayoutBinding(this, prop, values)

    addOnTabSelectedListener(listenerAndBinding)
    bindViewToBinding(prop, listenerAndBinding)
}

private class TabLayoutBinding<T>(
        view: TabLayout,
        prop: MutableProperty<T>,
        private val values: Array<T>
) : Binding<TabLayout, T>(view, prop), TabLayout.OnTabSelectedListener {

    override fun onTabReselected(tab: TabLayout.Tab) {}
    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabSelected(tab: TabLayout.Tab) {
        (property as MutableProperty<T>).value = values[tab.position]
    }

    override fun bind(view: TabLayout, value: T) {
        view.getTabAt(values.indexOf(value))!!.select()
    }
}
