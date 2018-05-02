package net.aquadc.properties.android.simple.design

import android.support.design.widget.TabLayout

/**
 * Simple adapter class for [TabLayout.OnTabSelectedListener].
 */
open class SimpleOnTabSelectedListener : TabLayout.OnTabSelectedListener {
    override fun onTabReselected(tab: TabLayout.Tab) = Unit
    override fun onTabUnselected(tab: TabLayout.Tab) = Unit
    override fun onTabSelected(tab: TabLayout.Tab) = Unit
}
