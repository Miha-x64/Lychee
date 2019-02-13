package net.aquadc.properties.android

import android.app.Activity
import android.app.Application
import android.app.Fragment
import android.app.Service
import android.app.backup.BackupAgent
import android.content.Context
import android.content.ContextWrapper
import android.util.Log


/**
 * [android.view.View] has no adequate ways of observing lifecycle.
 * This shitty workaround is intended to fix it.
 */
internal class `Lifecycle-Watcher` : Fragment() {

    init {
        retainInstance = true
    }

    private val listeners = ArrayList<(Boolean) -> Unit>()
    private var started = false

    override fun onStart() {
        super.onStart()
        notify(true)
    }

    override fun onStop() {
        notify(false)
        super.onStop()
    }

    private fun notify(whether: Boolean) {
        started = whether
        for (i in listeners.indices) {
            listeners[i](whether)
        }
    }

    internal fun observeStartedIf(subscribe: Boolean, onChange: (Boolean) -> Unit) {
        if (subscribe) {
            listeners.add(onChange)
        } else {
            listeners.remove(onChange)
        }

        // greet newcommers, farewell to leavers
        if (started) onChange(subscribe)
    }

}

internal fun Context.observeStartedIf(subscribe: Boolean, onChange: (Boolean) -> Unit) {
    val fm = findActivity()?.fragmentManager
            ?: return onChange(subscribe)
    val tag = `Lifecycle-Watcher`::class.java.name
    var watcher = fm.findFragmentByTag(tag) as `Lifecycle-Watcher`?
    if (subscribe && watcher === null)
        watcher = `Lifecycle-Watcher`().also { fm.beginTransaction().add(it, tag).commit() }
    watcher?.observeStartedIf(subscribe, onChange)
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (true) {
        when (ctx) {
            is Activity -> return ctx
            is Service, is Application, is BackupAgent -> return null
            is ContextWrapper -> ctx = ctx.baseContext
            else -> {
                Log.e("`Lifecycle-Watcher`", "Can't find host component", UnsupportedOperationException())
                return null
            }
        }
    }
}
