package net.aquadc.properties.android.simple.material

import com.google.android.material.tabs.TabLayout

/**
 * Simple adapter class for [TabLayout.OnTabSelectedListener].
 */
@Deprecated("unused by library")
open class SimpleOnTabSelectedListener : TabLayout.OnTabSelectedListener {
    /** No-op. */ override fun onTabReselected(tab: TabLayout.Tab) {}
    /** No-op. */ override fun onTabUnselected(tab: TabLayout.Tab) {}
    /** No-op. */ override fun onTabSelected(tab: TabLayout.Tab) {}
}
