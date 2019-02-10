package net.aquadc.properties.android.simple.design

import android.support.design.widget.TabLayout

/**
 * Simple adapter class for [TabLayout.OnTabSelectedListener].
 */
open class SimpleOnTabSelectedListener : TabLayout.OnTabSelectedListener {
    /** No-op. */ override fun onTabReselected(tab: TabLayout.Tab) {}
    /** No-op. */ override fun onTabUnselected(tab: TabLayout.Tab) {}
    /** No-op. */ override fun onTabSelected(tab: TabLayout.Tab) {}
}
